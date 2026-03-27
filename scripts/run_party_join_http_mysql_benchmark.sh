#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MYSQL_PORT="${MYSQL_PORT:-3307}"
DB_URL="jdbc:mysql://localhost:${MYSQL_PORT}/partition_mate_http_benchmark?createDatabaseIfNotExist=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8"

cd "$ROOT_DIR"

docker compose rm -sf db >/dev/null 2>&1 || true
MYSQL_PORT="$MYSQL_PORT" docker compose up -d db

echo "Waiting for MySQL on port ${MYSQL_PORT}..."
for _ in {1..30}; do
  HEALTH_STATUS="$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}starting{{end}}' partition-mate-db 2>/dev/null || echo starting)"
  if [[ "$HEALTH_STATUS" == "healthy" ]]; then
    break
  fi
  sleep 2
done

if [[ "$(docker inspect -f '{{.State.Health.Status}}' partition-mate-db 2>/dev/null || echo unhealthy)" != "healthy" ]]; then
  echo "MySQL container did not become healthy in time." >&2
  exit 1
fi

RUN_MYSQL_HTTP_BENCHMARK=true \
DB_URL="$DB_URL" \
DB_USERNAME="${DB_USERNAME:-root}" \
DB_PASSWORD="${DB_PASSWORD:-qwe123}" \
JPA_DDL_AUTO=create-drop \
JPA_SHOW_SQL=false \
JPA_FORMAT_SQL=false \
HIBERNATE_DIALECT=org.hibernate.dialect.MySQLDialect \
STORE_SEED_ENABLED=false \
./mvnw -q -Dtest=PartyJoinHttpMysqlBenchmarkTest test
