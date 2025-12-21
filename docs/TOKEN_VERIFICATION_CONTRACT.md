# Token verification contract for downstream services

## Scope
This document defines how OMS and Inventory verify access tokens issued by Identity Access Service.

## Token format and headers
1. JSON Web Token
2. Header alg must equal HS256
3. Header typ must equal JWT

## Claims
1. iss issuer string required
2. sub subject string required user id or username
3. exp expiration required
4. nbf not before optional
5. roles array of strings required when an endpoint enforces authorization
6. aud audience optional in version one see Audience section

## Required validations in resource servers
1. Verify signature using the shared secret
2. Verify header alg equals HS256
3. Verify header typ equals JWT
4. Verify iss equals the configured issuer
5. Verify exp is in the future allow up to sixty seconds of clock skew
6. If aud is present verify it matches the configured audience value for that service
7. If the endpoint requires authorization verify that roles contains the required role name

## Error mapping for resource servers
1. Invalid signature return 401
2. Expired token return 401
3. Wrong issuer return 401
4. Missing role where required return 403
5. Wrong audience return 401

## Secret management
1. The shared secret length must be at least thirty two bytes
2. Provide the secret only through environment variables never through committed files
3. All environments must use distinct secrets
4. Services read the same secret value as the issuer so they can verify tokens

### Environment variable names
1. Issuer service reads SECURITY_JWT_SECRET
2. Resource servers read SECURITY_JWT_SECRET or a service specific variable if you prefer to scope per service

## Rotation plan with kid
1. Introduce a kid header to identify the active key id
2. Maintain a set of current and previous secrets during a rotation window
3. Issuer signs with the new secret and kid new
4. Resource servers accept both secrets during the grace period then drop the previous secret
5. Document the calendar and steps for rotation in a private runbook not in this public contract

## Audience
1. If you have one consumer per environment set aud to that service name and require a match
2. If you cannot wire audience now leave aud out of tokens in version one
3. When you later introduce audience update this contract and add tests in each resource server

## Token issuer rule
1. Only Identity Access Service signs tokens
2. Resource servers verify tokens and never mint or sign tokens

## Spring Boot reference verification
Minimal configuration for a resource server that verifies HS256 with a secret loaded from environment. No example secret value is shown here.

```java
@Configuration
public class JwtVerificationConfig {

  @Bean
  JwtDecoder jwtDecoder(@Value("${SECURITY_JWT_SECRET}") String secret) {
    if (secret == null || secret.isBlank()) {
      throw new IllegalStateException("SECURITY_JWT_SECRET is required");
    }
    byte[] keyBytes;
    try {
      keyBytes = java.util.Base64.getDecoder().decode(secret);
    } catch (IllegalArgumentException ignore) {
      keyBytes = secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
    javax.crypto.SecretKey key = new javax.crypto.spec.SecretKeySpec(keyBytes, "HmacSHA256");
    return org.springframework.security.oauth2.jwt.NimbusJwtDecoder
        .withSecretKey(key)
        .macAlgorithm(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256)
        .build();
  }
}

