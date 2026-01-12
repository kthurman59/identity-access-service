#!/usr/bin/env bash
set -euo pipefail

BASE="${BASE:-http://localhost:8082}"
TENANT="${TENANT:-demo}"

echo "== health =="
curl -s -o /dev/null -w '%{http_code}\n' "$BASE/actuator/health"

echo "== login =="
LOGIN_JSON=$(curl -sS -H "Content-Type: application/json" -H "X-Tenant-Key: $TENANT" \
  -d '{"username":"admin","password":"Admin123!"}' "$BASE/auth/login")
echo "$LOGIN_JSON" | jq .

ACCESS=$(jq -r '.accessToken // empty' <<<"$LOGIN_JSON")
REFRESH=$(jq -r '.refreshToken // empty' <<<"$LOGIN_JSON")

echo "== refresh =="
curl -sS -H "Content-Type: application/json" -H "X-Tenant-Key: $TENANT" \
  -d "{\"refreshToken\":\"$REFRESH\"}" "$BASE/auth/refresh" | jq .

echo "== logout =="
curl -i -s -H "Content-Type: application/json" -H "X-Tenant-Key: $TENANT" \
  -d "{\"refreshToken\":\"$REFRESH\"}" "$BASE/auth/logout"
echo
