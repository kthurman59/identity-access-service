#!/usr/bin/env bash
set -euo pipefail

BASE="http://localhost:8082"
TENANT="demo"

echo "health"
code=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/actuator/health")
echo "health http $code"
test "$code" = "200"

echo "login"
LOGIN_JSON=$(curl -sS -H "Content-Type: application/json" -H "X-Tenant-Key: $TENANT" \
  -d '{"username":"admin","password":"Admin123!"}' "$BASE/auth/login")
echo "$LOGIN_JSON" | jq .

ACCESS=$(jq -r '.accessToken // empty' <<<"$LOGIN_JSON")
REFRESH=$(jq -r '.refreshToken // empty' <<<"$LOGIN_JSON")
test -n "$ACCESS" && test -n "$REFRESH"

echo "decode access"
echo "$ACCESS" | cut -d. -f2 | base64 -d 2>/dev/null | jq '{iss,sub,aud,jti,roles,tenant,exp}'

echo "refresh"
REFRESH_JSON=$(curl -sS -H "Content-Type: application/json" -H "X-Tenant-Key: $TENANT" \
  -d "{\"refreshToken\":\"$REFRESH\"}" "$BASE/auth/refresh")
echo "$REFRESH_JSON" | jq .

NEW_REFRESH=$(jq -r '.refreshToken // empty' <<<"$REFRESH_JSON")
test -n "$NEW_REFRESH"

echo "reuse old refresh should fail"
curl -s -o /dev/null -w 'old reuse http %{http_code}\n' -H "Content-Type: application/json" -H "X-Tenant-Key: $TENANT" \
  -d "{\"refreshToken\":\"$REFRESH\"}" "$BASE/auth/refresh"

echo "revoke current refresh then confirm failure"
curl -s -o /dev/null -w 'revoke http %{http_code}\n' -H "Content-Type: application/json" -H "X-Tenant-Key: $TENANT" \
  -d "{\"refreshToken\":\"$NEW_REFRESH\"}" "$BASE/auth/revoke"
curl -s -o /dev/null -w 'post-revoke refresh http %{http_code}\n' -H "Content-Type: application/json" -H "X-Tenant-Key: $TENANT" \
  -d "{\"refreshToken\":\"$NEW_REFRESH\"}" "$BASE/auth/refresh"
