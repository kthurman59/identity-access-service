# IAS runbook

## Build and run
./mvnw -q -DskipTests package
export COMPOSE_PROJECT_NAME=ias
docker compose --project-name ias -f docker/compose.yml --env-file docker/.env down -v
docker compose --project-name ias -f docker/compose.yml --env-file docker/.env up -d --force-recreate

## Health
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8082/actuator/health

## Login and refresh
TENANT=demo
curl -sS -H "Content-Type: application/json" -H "X-Tenant-Key: $TENANT" -d '{"username":"admin","password":"Admin123!"}' http://localhost:8082/auth/login | jq .

