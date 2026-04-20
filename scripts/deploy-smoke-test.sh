#!/usr/bin/env sh
set -eu

REPO_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"

POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-smoke-db-password}"
APP_JWT_SECRET="${APP_JWT_SECRET:-$(openssl rand -hex 32)}"
APP_BOOTSTRAP_ADMIN_PASSWORD="${APP_BOOTSTRAP_ADMIN_PASSWORD:-smoke-admin-password}"
export POSTGRES_PASSWORD APP_JWT_SECRET APP_BOOTSTRAP_ADMIN_PASSWORD

cleanup() {
  docker compose -f "${REPO_DIR}/docker-compose.yml" down --volumes --remove-orphans
}
trap cleanup EXIT

"${REPO_DIR}/scripts/generate-tls-certs.sh" localhost

# Ensure a clean DB volume so credentials always match this smoke test run.
docker compose -f "${REPO_DIR}/docker-compose.yml" down --volumes --remove-orphans >/dev/null 2>&1 || true

docker compose -f "${REPO_DIR}/docker-compose.yml" up -d --build

for _ in $(seq 1 30); do
  if curl -kfsS https://127.0.0.1/api/public/home >/dev/null 2>&1; then
    echo "Deploy smoke test passed"
    exit 0
  fi
  sleep 2
done

echo "Deploy smoke test failed: endpoint did not become ready in time" >&2
exit 1
