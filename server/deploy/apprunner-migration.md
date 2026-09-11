# Moving the RemindMe API from EC2 + ALB to AWS App Runner

Almost all of this was run directly against the account (IAM, DynamoDB, ECR, SSM, App Runner) -
this doc is a record of what exists and the couple of things that need a human:

- a repo-side secret paste for CI, and
- the DNS cutover + AWS teardown, both deliberately left for explicit sign-off.

## What's been created

| Resource | Region | Name / ARN |
|---|---|---|
| DynamoDB table | us-west-1 | `FeedCache` (on-demand, PK `Name`) - holds the persisted digest/watchlist/local-events caches |
| ECR repository | us-west-2 | `891377299679.dkr.ecr.us-west-2.amazonaws.com/remindme-api` |
| SSM parameters (SecureString) | us-west-2 | `/remindme/oauth-client-secret`, `/remindme/anthropic-api-key`, `/remindme/tavily-api-key`, `/remindme/ticketmaster-api-key` |
| IAM role (instance) | global | `RemindMe-AppRunner-InstanceRole` - what the running container assumes; see `apprunner-instance-policy.json` |
| IAM role (ECR access) | global | `RemindMe-AppRunner-ECRAccessRole` - lets App Runner pull from ECR |
| IAM user (CI) | global | `remindme-ci-ecr` - GitHub Actions pushes images with this; see `ci-ecr-push-policy.json` |
| App Runner service | us-west-2 | `remindme-api`, 0.5 vCPU / 1 GB, auto-deploy on ECR `:latest` push, health check `GET /actuator/health` |

The app authenticates to DynamoDB / EventBridge Scheduler / Pinpoint (all still in `us-west-1`)
via the **instance role** - no access keys anywhere in the running service.

## What's left

### 1. GitHub: add the CI secrets

Repo → Settings → Secrets and variables → Actions → New repository secret:

- `AWS_ECR_ACCESS_KEY_ID`
- `AWS_ECR_SECRET_ACCESS_KEY`

(Values were generated for the `remindme-ci-ecr` IAM user and given to you separately - they're
not written down here.) Once set, push to `master` (or run the `Spring CI/CD` workflow manually)
and confirm the `deploy` job pushes an image and App Runner picks it up.

Delete the now-unused secrets once the cutover below is done: `AWS_INSTANCE_SG_ID`,
`AWS_EC2_ACCESS_KEY_ID`, `AWS_EC2_SECRET_ACCESS_KEY`, `AWS_EC2_SSH_PRIVATE_KEY`.

### 2. Verify on the App Runner default domain

```bash
curl -s https://<service-url>.us-west-2.awsapprunner.com/actuator/health
curl -s https://<service-url>.us-west-2.awsapprunner.com/digest | head -c 300
```

### 3. Custom domain + DNS cutover (needs your go-ahead - production-facing)

```bash
aws apprunner associate-custom-domain --region us-west-2 \
  --service-arn <service-arn> \
  --domain-name api.remindme.amsksolutions.com
```

This returns certificate-validation CNAME records and the target to point the domain at. In
Route53 (zone `amsksolutions.com`, `Z090961133I6RP4C2ZEEJ`):

- remove the existing `api.remindme.amsksolutions.com` A-ALIAS record (currently pointed at the ALB)
- add the CNAME(s) `associate-custom-domain` printed (validation + the service's default domain)

Wait for `aws apprunner describe-custom-domains` to report the certificate as `ACTIVE`, then
confirm `https://api.remindme.amsksolutions.com/actuator/health` and load the actual site.

### 4. Decommission EC2 (after a clean day or two - also needs your go-ahead)

- EC2 → stop, then terminate the instance
- EC2 → Load Balancers → delete the ALB; delete its target group and listener
- EC2 → Elastic IPs → release the one that was on the instance
- Certificate Manager → delete the ALB's cert if nothing else uses it
- EC2 → Security Groups → delete the API instance's security group
- Route53 → remove the ACM validation CNAME left over from the old cert

Leave alone: DynamoDB tables, EventBridge Scheduler + schedule groups,
`RemindMe-EventBridgeExecutionRole`, the SendText / SendEmail Lambdas, Pinpoint, the S3 client
bucket and its pipeline.

## Rollback

Route53 TTLs are short during cutover - if App Runner misbehaves, point `api` back at the ALB;
the EC2 box (left running until step 4) serves it again.

## Redeploying by hand

```bash
cd server
aws ecr get-login-password --region us-west-2 | docker login --username AWS --password-stdin 891377299679.dkr.ecr.us-west-2.amazonaws.com
docker build --platform linux/amd64 -t 891377299679.dkr.ecr.us-west-2.amazonaws.com/remindme-api:latest .
docker push 891377299679.dkr.ecr.us-west-2.amazonaws.com/remindme-api:latest
```

Auto-deploy is on, so the push alone triggers a rollout - no separate `update-service` needed.
