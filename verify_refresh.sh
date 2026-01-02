#!/usr/bin/env bash
set -euo pipefail

BASE="${BASE:-http://localhost:8082}"
TENANT="${TENANT:-demo}"

login() {
  local user="$1"
  local pass="$2"
  curl -sS \
    -H "Content-Type: application/json" \
    -H "X-Tenant-Key: ${TENANT}" \
    -d "{\"username\":\"${user}\",\"password\":\"${pass}\"}" \
    "${BASE}/auth/login"
}

refresh() {
  local rt="$1"
  curl -sS \
    -H "Content-Type: application/json" \
    -H "X-Tenant-Key: ${TENANT}" \
    -d "{\"refreshToken\":\"${rt}\"}" \
    "${BASE}/auth/refresh"
}

http_code() {
  curl -sS -o /dev/null -w "%{http_code}" "$@"
}

ADMIN_JSON="$(login admin Admin123!)"
ADMIN_AT="$(jq -r .accessToken <<<"$ADMIN_JSON")"
ADMIN_RT="$(jq -r .refreshToken <<<"$ADMIN_JSON")"

test "$(awk -F. '{print NF}' <<<"$ADMIN_AT")" = "3"
test "$(awk -F. '{print NF}' <<<"$ADMIN_RT")" != "3"

ADMIN_REFRESHED_JSON="$(refresh "$ADMIN_RT")"
ADMIN_AT2="$(jq -r .accessToken <<<"$ADMIN_REFRESHED_JSON")"
ADMIN_RT2="$(jq -r .refreshToken <<<"$ADMIN_REFRESHED_JSON")"

REUSE_STATUS="$(http_code \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Key: ${TENANT}" \
  -d "{\"refreshToken\":\"${ADMIN_RT}\"}" \
  "${BASE}/auth/refresh")"
test "$REUSE_STATUS" = "401"

test "$(http_code -H "X-Tenant-Key: ${TENANT}" -H "Authorization: Bearer ${ADMIN_AT2}" "${BASE}/admin/ping")" = "200"
test "$(http_code -H "X-Tenant-Key: ${TENANT}" -H "Authorization: Bearer ${ADMIN_AT2}" "${BASE}/secure/ping")" = "200"

USER_JSON="$(login user User123!)"
USER_AT="$(jq -r .accessToken <<<"$USER_JSON")"

test "$(http_code -H "X-Tenant-Key: ${TENANT}" -H "Authorization: Bearer ${USER_AT}" "${BASE}/secure/ping")" = "200"
test "$(http_code -H "X-Tenant-Key: ${TENANT}" -H "Authorization: Bearer ${USER_AT}" "${BASE}/admin/ping")" = "403"

echo ok
