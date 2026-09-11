#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

if [[ ! -f .env.local ]]; then
  echo "missing server/.env.local (see README)" >&2
  exit 1
fi

set -a
source .env.local
set +a

exec mvn spring-boot:run -Dspring-boot.run.profiles=dev
