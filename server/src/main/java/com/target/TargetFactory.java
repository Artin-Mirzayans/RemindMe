package com.target;

import software.amazon.awssdk.services.scheduler.model.Target;
import org.springframework.stereotype.Component;

@Component
public class TargetFactory {

    private final String emailLambdaFunctionArn = "arn:aws:lambda:us-west-1:891377299679:function:SendEmail";
    private final String textLambdaFunctionArn = "arn:aws:lambda:us-west-1:891377299679:function:SendText";

    private final String eventBridgeRoleArn = "arn:aws:iam::891377299679:role/RemindMe-EventBridgeExecutionRole";

    public Target createLambdaTarget(String contactMethod) {
        String lambdaFunctionArn;

        switch (contactMethod) {
            case "Email":
                lambdaFunctionArn = emailLambdaFunctionArn;
                break;
            case "Text":
                lambdaFunctionArn = textLambdaFunctionArn;
                break;
            default:
                throw new IllegalArgumentException("Unknown event type: " + contactMethod);
        }

        return Target.builder()
                .arn(lambdaFunctionArn)
                .roleArn(eventBridgeRoleArn)
                .build();
    }
}
