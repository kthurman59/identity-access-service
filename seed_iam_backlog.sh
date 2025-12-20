#!/usr/bin/env bash
set -euo pipefail

REPO="${1:-}"
if [[ -z "${REPO}" ]]; then
  REPO="$(gh repo view --json nameWithOwner -q .nameWithOwner)"
fi

echo "Using repo: ${REPO}"

need() { command -v "$1" >/dev/null 2>&1 || {
  echo "Missing $1"
  exit 1
}; }
need gh
gh auth status >/dev/null

create_label() {
  local name="$1" color="$2" desc="$3"
  if gh label list -R "$REPO" --limit 500 | awk '{print $1}' | grep -qx "$name"; then
    return 0
  fi
  gh label create "$name" -R "$REPO" --color "$color" --description "$desc" >/dev/null
}

create_milestone() {
  local title="$1" desc="$2"
  if gh api -H "Accept: application/vnd.github+json" "repos/${REPO}/milestones?state=all&per_page=100" |
    grep -q "\"title\":\"${title//\"/\\\"}\""; then
    return 0
  fi
  gh api -X POST -H "Accept: application/vnd.github+json" "repos/${REPO}/milestones" \
    -f title="$title" -f description="$desc" >/dev/null
}

issue_exists() {
  local title="$1"
  gh issue list -R "$REPO" --limit 500 --search "\"$title\" in:title" | grep -q "$title"
}

create_issue() {
  local title="$1" milestone="$2" labels="$3" body="$4"
  issue_exists "$title" && return 0
  gh issue create -R "$REPO" --title "$title" --body-file "$body" --milestone "$milestone" --label "$labels" >/dev/null
}

tmpdir="$(mktemp -d)"
trap 'rm -rf "$tmpdir"' EXIT

echo "Creating labels"
create_label "type:feature" "1D76DB" "New functionality"
create_label "type:bug" "D73A4A" "Defect"
create_label "type:chore" "6F42C1" "Maintenance"
create_label "type:docs" "0E8A16" "Documentation"
create_label "type:security" "B60205" "Security work"
create_label "type:techdebt" "FBCA04" "Refactors and debt"

create_label "prio:p0" "B60205" "Must do now"
create_label "prio:p1" "D93F0B" "Should do next"
create_label "prio:p2" "FBCA04" "Nice to have"

create_label "area:build" "5319E7" "Build and CI"
create_label "area:auth" "0366D6" "Authentication"
create_label "area:rbac" "0052CC" "Roles and permissions"
create_label "area:security" "B60205" "Security"
create_label "area:ops" "0E8A16" "Operations"
create_label "area:docs" "0E8A16" "Docs"
create_label "area:api" "1D76DB" "API"

echo "Creating milestones"
create_milestone "v1 Foundation" "Project scaffolding build CI local dev"
create_milestone "v1 Auth Core" "Registration login tokens sessions"
create_milestone "v1 RBAC" "Roles permissions enforcement admin endpoints"
create_milestone "v1 Security Hardening" "Key rotation rate limits audit logging"
create_milestone "v1 Release" "Docs OpenAPI Docker release checklist"

echo "Creating issues"

body="$tmpdir/issue.md"

cat >"$body" <<'MD'
Goal
Set baseline quality for the repo so every later change is consistent.

Acceptance criteria
Format enforced in CI
Static analysis runs in CI
Coverage gate configured
Dependency and plugin versions pinned
Local developer workflow documented

Tasks
Add Spotless
Add Maven Enforcer for Java version and dependency convergence
Add Checkstyle or Error Prone
Add Jacoco thresholds and fail build on regression
Add Makefile or documented Maven commands
MD
create_issue "Build quality gates and formatting baseline" "v1 Foundation" "type:chore,prio:p0,area:build" "$body"

cat >"$body" <<'MD'
Goal
Make local development predictable and fast.

Acceptance criteria
One command starts required services
App runs against local Postgres
Flyway migrations run on startup

Tasks
Add docker compose with Postgres
Add application local profile
Document bootstrap in README
MD
create_issue "Local dev stack with Postgres and Flyway" "v1 Foundation" "type:chore,prio:p0,area:ops" "$body"

cat >"$body" <<'MD'
Goal
Establish integration tests that match production behavior.

Acceptance criteria
Integration tests use Testcontainers
Tests cover migrations and repository layer
CI runs tests with containers

Tasks
Add Testcontainers Postgres
Add base integration test config
Add a migration smoke test
MD
create_issue "Testcontainers integration test foundation" "v1 Foundation" "type:feature,prio:p0,area:build" "$body"

cat >"$body" <<'MD'
Goal
Remove the Mockito self attach warning and future JDK breakage.

Acceptance criteria
Mockito inline uses javaagent in test JVM args
No runtime warning about dynamic attach
Document the change in testing section

Tasks
Set surefire argLine with mockito javaagent
Verify with mvn clean test
MD
create_issue "Mockito javaagent config for inline mock maker" "v1 Foundation" "type:chore,prio:p1,area:build" "$body"

cat >"$body" <<'MD'
Goal
Ship a reliable CI pipeline.

Acceptance criteria
CI caches Maven
Runs unit and integration tests
Enforces Jacoco gate
Builds container image on main and PR

Tasks
Add GitHub Actions workflow
Publish build artifacts
Fail fast on test or lint errors
MD
create_issue "CI pipeline for build test and image" "v1 Foundation" "type:chore,prio:p0,area:build" "$body"

cat >"$body" <<'MD'
Goal
Implement user registration with secure password storage.

Acceptance criteria
POST register endpoint
Email and password validated
Password stored with strong hash
Duplicate email returns proper error
Integration tests cover success and failure

Notes
Use bcrypt or Argon2
MD
create_issue "User registration endpoint with secure password hashing" "v1 Auth Core" "type:feature,prio:p0,area:auth,area:security,area:api" "$body"

cat >"$body" <<'MD'
Goal
Implement login and token issuing.

Acceptance criteria
POST login endpoint
Returns access token and refresh token
Access token has iss sub iat exp jti aud
Refresh token stored server side tied to user and device
Tests cover invalid credentials and disabled account

Security
Constant time compare
No user enumeration in errors
MD
create_issue "Login endpoint with access and refresh tokens" "v1 Auth Core" "type:feature,prio:p0,area:auth,area:security,area:api" "$body"

cat >"$body" <<'MD'
Goal
Support refresh token rotation and revoke on reuse.

Acceptance criteria
POST refresh endpoint
Rotation on every use
Reuse of an old refresh revokes the session family
Logout revokes refresh token
Tests cover rotation and reuse detection
MD
create_issue "Refresh token rotation with reuse detection" "v1 Auth Core" "type:feature,prio:p0,area:auth,area:security,area:api" "$body"

cat >"$body" <<'MD'
Goal
Verify user email before full access.

Acceptance criteria
Issue verification token and email event
Verify endpoint marks user as verified
Unverified users have limited access
Tests cover token expiry and reuse
MD
create_issue "Email verification flow" "v1 Auth Core" "type:feature,prio:p1,area:auth,area:security,area:api" "$body"

cat >"$body" <<'MD'
Goal
Allow secure password reset.

Acceptance criteria
Start reset endpoint issues time bound token
Complete reset endpoint sets new password and invalidates tokens
Rate limited to prevent abuse
Tests cover token expiry and invalid token
MD
create_issue "Password reset flow" "v1 Auth Core" "type:feature,prio:p1,area:auth,area:security,area:api" "$body"

cat >"$body" <<'MD'
Goal
Prevent brute force login.

Acceptance criteria
Track failed attempts per account and optionally per IP
Temporary lock after threshold
Admin unlock path or automatic unlock after duration
Tests cover lock behavior
MD
create_issue "Account lockout and failed attempt tracking" "v1 Auth Core" "type:feature,prio:p1,area:auth,area:security" "$body"

cat >"$body" <<'MD'
Goal
Let users view and revoke sessions.

Acceptance criteria
List current sessions with device and last used
Revoke specific session
Revocation kills refresh and rejects further use
Tests cover revoke behavior
MD
create_issue "Device and session management endpoints" "v1 Auth Core" "type:feature,prio:p2,area:auth,area:security,area:api" "$body"

cat >"$body" <<'MD'
Goal
Let OMS and Inventory validate tokens easily.

Acceptance criteria
Expose JWK set endpoint when using asymmetric signing
Document verification approach for other services
Provide example Spring Security Resource Server config
Contract tests for token claims shape
MD
create_issue "Token verification contract for downstream services" "v1 Auth Core" "type:feature,prio:p1,area:security,area:docs,area:api" "$body"

cat >"$body" <<'MD'
Goal
Define token claims and versioning.

Acceptance criteria
Claim set documented with a version field
aud maps to allowed clients
Custom claims documented for roles and permissions
Tests enforce claim presence and shape
MD
create_issue "Token claim schema and versioning" "v1 Auth Core" "type:docs,prio:p2,area:api,area:security" "$body"

cat >"$body" <<'MD'
Goal
Define RBAC data model and persistence.

Acceptance criteria
Entities and migrations for role permission user role role permission
Seed initial roles and permissions
Repository tests cover joins and constraints

Tasks
Define permission naming
Define default system roles
MD
create_issue "RBAC schema entities and migrations" "v1 RBAC" "type:feature,prio:p0,area:rbac,area:security" "$body"

cat >"$body" <<'MD'
Goal
Manage roles and assignments.

Acceptance criteria
Admin endpoints for role create read update delete
Endpoints to assign roles to users
Changes audited
Tests cover authorization and validation

Security
Admin endpoints require explicit permission
MD
create_issue "Admin API for roles and user assignments" "v1 RBAC" "type:feature,prio:p0,area:rbac,area:api,area:security" "$body"

cat >"$body" <<'MD'
Goal
Enforce permissions in code.

Acceptance criteria
Spring method security enabled
At least two endpoints protected by permissions
Integration tests show forbidden and allowed based on role
MD
create_issue "Permission enforcement via method security" "v1 RBAC" "type:feature,prio:p0,area:rbac,area:security" "$body"

cat >"$body" <<'MD'
Goal
Rotate signing keys safely.

Acceptance criteria
Keys have key id
New tokens use active key id
Old keys kept for verification until window passes
Document rotation procedure
Tests cover verification with old and new keys
MD
create_issue "JWT signing key management with rotation" "v1 Security Hardening" "type:security,prio:p0,area:security" "$body"

cat >"$body" <<'MD'
Goal
Rate limit sensitive endpoints.

Acceptance criteria
Limits on login and refresh
Return 429 with retry info
Configurable per environment
Tests cover basic behavior
MD
create_issue "Rate limiting for login and refresh" "v1 Security Hardening" "type:security,prio:p1,area:security,area:auth" "$body"

cat >"$body" <<'MD'
Goal
Audit security events.

Events
Login success and failure
Token refresh
Logout
Role changes
User disable and enable

Acceptance criteria
Audit table or log sink exists
Correlation id included
No secrets logged
Tests cover at least one audit write path
MD
create_issue "Audit logging for auth and RBAC events" "v1 Security Hardening" "type:security,prio:p1,area:security,area:ops" "$body"

cat >"$body" <<'MD'
Goal
Expose health metrics and structured logs.

Acceptance criteria
Spring Actuator enabled with sane exposure
Prometheus metrics endpoint
Logs include correlation id
Readme lists endpoints and local testing
MD
create_issue "Observability baseline with Actuator metrics and logs" "v1 Foundation" "type:feature,prio:p1,area:ops" "$body"

cat >"$body" <<'MD'
Goal
Publish OpenAPI and keep it in sync.

Acceptance criteria
OpenAPI available at runtime
OpenAPI checked in or generated in CI
Docs show example requests and responses
MD
create_issue "OpenAPI documentation for IAM API" "v1 Release" "type:docs,prio:p1,area:api,area:docs" "$body"

cat >"$body" <<'MD'
Goal
Containerize the service with sane defaults.

Acceptance criteria
Dockerfile or Jib builds the image
Image runs as non root user
Config via env vars
CI builds image on PR and main
MD
create_issue "Docker image build and CI integration" "v1 Release" "type:feature,prio:p0,area:ops" "$body"

cat >"$body" <<'MD'
Goal
Provide deploy examples for cluster use.

Acceptance criteria
Kustomize or Helm manifests with config examples
Secrets and config documented
Readiness and liveness probes present
Sample Ingress for internal use

Notes
Downstream services example config included
MD
create_issue "Deployment manifests and samples" "v1 Release" "type:docs,prio:p1,area:ops,area:docs" "$body"

cat >"$body" <<'MD'
Goal
Publish a release checklist and runbook.

Acceptance criteria
Checklist for version bump build tag image push docs
Runbook for key rotation token verification outages and rollback
MD
create_issue "Release checklist and operations runbook" "v1 Release" "type:docs,prio:p2,area:docs,area:ops" "$body"

echo "Done. Created labels milestones and issues in ${REPO}"
