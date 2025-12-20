# IAM Auth service architecture

## Purpose
Authenticate users, issue tokens, and enforce authorization decisions for downstream services.

## Non goals
1. No OMS or Inventory domain logic.
2. No UI.
3. No shared database access by other services.
4. No custom cryptography.

## Layer boundaries
1. api layer
   1. Controllers only call application services.
   2. Controllers use request and response models only.
   3. Controllers never return persistence entities.
2. application layer
   1. Orchestrates use cases and transactions.
   2. Defines ports for persistence and integrations.
   3. Contains mapping between api models and domain models.
3. domain layer
   1. Holds business rules and decisions.
   2. No framework annotations.
4. infrastructure layer
   1. Implements ports.
   2. Contains Spring configuration, security wiring, persistence adapters.

## Package layout
Base package: com.kevdev.iamauth

1. com.kevdev.iamauth.api
2. com.kevdev.iamauth.application
3. com.kevdev.iamauth.domain
4. com.kevdev.iamauth.infrastructure

## API rules
1. Every endpoint has explicit request and response types.
2. Validation happens at the boundary using bean validation.
3. Errors use a single standard error response.
4. Status codes are defined and tested.

## Transaction rules
1. Transactions live in application services, not controllers.
2. One use case equals one transaction unless there is a hard reason.

## Security rules
1. Never log passwords or full tokens.
2. Refresh tokens are rotated on use and stored hashed.
3. Logout revokes refresh tokens.

## Definition of done for each PR
1. Layer boundaries respected.
2. Tests cover success and failure paths.
3. Migrations included for schema changes.
4. Audit events added for security relevant behavior.
5. No TODO left behind.

