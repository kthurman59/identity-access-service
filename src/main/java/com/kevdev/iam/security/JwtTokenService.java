package com.kevdev.iam.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

  private static final Base64.Encoder B64_URL = Base64.getUrlEncoder().withoutPadding();
  private static final Set<String> RESERVED = Set.of("iss", "sub", "iat", "exp", "roles");

  private final ObjectMapper objectMapper;
  private final byte[] secret;
  private final String issuer;
  private final Duration accessTtl;

  public JwtTokenService(
      ObjectMapper objectMapper,
      @Value("${ias.jwt.secret.b64}") String secretB64,
      @Value("${ias.jwt.issuer:ias}") String issuer,
      @Value("${ias.jwt.access-ttl-minutes:15}") int accessTtlMinutes
  ) {
    this.objectMapper = objectMapper;

    if (secretB64 == null || secretB64.isBlank()) {
      throw new IllegalStateException("ias.jwt.secret.b64 is required");
    }

    byte[] key;
    try {
      key = Base64.getDecoder().decode(secretB64.trim());
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException("ias.jwt.secret.b64 must be valid base64", e);
    }

    if (key.length < 32) {
      throw new IllegalStateException("ias.jwt.secret.b64 must decode to at least 32 bytes");
    }

    this.secret = key;
    this.issuer = (issuer == null || issuer.isBlank()) ? "ias" : issuer.trim();

    if (accessTtlMinutes <= 0) {
      throw new IllegalStateException("ias.jwt.access-ttl-minutes must be positive");
    }
    this.accessTtl = Duration.ofMinutes(accessTtlMinutes);
  }

  public String issueAccessToken(UserDetails user) {
    List<String> roles = user.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
        .map(r -> r.toUpperCase(Locale.ROOT))
        .distinct()
        .toList();

    return issueAccessToken(user.getUsername(), roles, Map.of());
  }

  public String issueAccessToken(String subject, List<String> roles, Map<String, Object> extraClaims) {
    long iat = Instant.now().getEpochSecond();
    long exp = iat + accessTtl.toSeconds();

    List<String> normalizedRoles = (roles == null ? List.<String>of() : roles).stream()
        .map(r -> r == null ? "" : r)
        .map(r -> r.startsWith("ROLE_") ? r.substring(5) : r)
        .map(r -> r.toUpperCase(Locale.ROOT))
        .filter(r -> !r.isBlank())
        .distinct()
        .toList();

    Map<String, Object> header = new LinkedHashMap<>();
    header.put("alg", "HS256");
    header.put("typ", "JWT");

    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("iss", issuer);
    payload.put("sub", subject);
    payload.put("iat", iat);
    payload.put("exp", exp);
    payload.put("roles", normalizedRoles);

    if (extraClaims != null) {
      for (Map.Entry<String, Object> e : extraClaims.entrySet()) {
        String k = e.getKey();
        if (k == null || k.isBlank()) continue;
        if (RESERVED.contains(k)) continue;
        payload.put(k, e.getValue());
      }
    }

    String headerB64 = b64Url(jsonBytes(header));
    String payloadB64 = b64Url(jsonBytes(payload));
    String signingInput = headerB64 + "." + payloadB64;
    String sigB64 = b64Url(hmacSha256(signingInput));

    return signingInput + "." + sigB64;
  }

  private byte[] jsonBytes(Object obj) {
    try {
      return objectMapper.writeValueAsBytes(obj);
    } catch (Exception e) {
      throw new IllegalStateException("JWT JSON serialization failed", e);
    }
  }

  private String b64Url(byte[] bytes) {
    return B64_URL.encodeToString(bytes);
  }

  private byte[] hmacSha256(String signingInput) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret, "HmacSHA256"));
      return mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
    } catch (Exception e) {
      throw new IllegalStateException("JWT HMAC signing failed", e);
    }
  }
}

