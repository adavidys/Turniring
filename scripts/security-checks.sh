#!/usr/bin/env sh
set -eu

REPO_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"

if ! command -v docker >/dev/null 2>&1; then
  echo "docker is required for security checks" >&2
  exit 1
fi

POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-check-only-db-password}"
APP_JWT_SECRET="${APP_JWT_SECRET:-$(openssl rand -hex 32)}"
APP_BOOTSTRAP_ADMIN_PASSWORD="${APP_BOOTSTRAP_ADMIN_PASSWORD:-check-only-admin-password}"
export POSTGRES_PASSWORD APP_JWT_SECRET APP_BOOTSTRAP_ADMIN_PASSWORD

echo "==> Running gitleaks on repository files"
docker run --rm -v "${REPO_DIR}:/repo" zricethezav/gitleaks:v8.24.3 \
  detect --source /repo --redact --verbose

echo "==> Running trivy filesystem scan (vuln/misconfig/secret)"
docker run --rm -v "${REPO_DIR}:/repo" aquasec/trivy:0.56.2 \
  fs --scanners vuln,misconfig,secret --severity HIGH,CRITICAL --exit-code 1 --no-progress --skip-check-update \
  --skip-dirs /repo/deploy/certs --skip-dirs /repo/frontend/.npm-cache --skip-dirs /repo/frontend/node_modules \
  --skip-dirs /repo/build --skip-dirs /repo/frontend/dist /repo

echo "==> Validating compose syntax"
docker compose -f "${REPO_DIR}/docker-compose.yml" config -q

echo "Security checks passed"
