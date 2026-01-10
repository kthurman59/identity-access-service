package com.kevdev.iam.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class JwtTokenService implements TokenService {

  private static final Base64.Encoder B64_URL = Base64.getUrlEncoder().withoutPadding();
  private static final Base64.Decoder B64_URL_DEC = Base64.getUrlDecoder();

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
        .toList();

    return issueAccessToken(user.getUsername(), roles, Map.of());
  }

  @Override
  public String issueAccessToken(String subject, List<String> roles, Map<String, Object> extraClaims) {
    if (subject == null || subject.isBlank()) {
      throw new IllegalStateException("subject is required");
    }

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
        if (e.getKey() == null || e.getKey().isBlank()) continue;
        if (payload.containsKey(e.getKey())) continue;
        payload.put(e.getKey(), e.getValue());
      }
    }

    String headerB64 = b64Url(jsonBytes(header));
    String payloadB64 = b64Url(jsonBytes(payload));
    String signingInput = headerB64 + "." + payloadB64;
    String sigB64 = b64Url(hmacSha256(signingInput));

    return signingInput + "." + sigB64;
  }

  @Override
  public DecodedAccessToken decodeAndValidateAccessToken(String token) {
    if (token == null || token.isBlank()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "missing token");
    }

    String[] parts = token.split("\\.");
    if (parts.length != 3) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid token");
    }

    String signingInput = parts[0] + "." + parts[1];
    byte[] expectedSig = hmacSha256(signingInput);
    byte[] providedSig = b64UrlDecode(parts[2]);

    if (!MessageDigest.isEqual(expectedSig, providedSig)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid token signature");
    }

    Map<String, Object> header = readJsonMap(b64UrlDecode(parts[0]));
    Object alg = header.get("alg");
    if (alg == null || !"HS256".equals(String.valueOf(alg))) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid token alg");
    }

    Map<String, Object> claims = readJsonMap(b64UrlDecode(parts[1]));

    String iss = asString(claims.get("iss"));
    if (iss == null || !issuer.equals(iss)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid token issuer");
    }

    String sub = asString(claims.get("sub"));
    if (sub == null || sub.isBlank()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid token subject");
    }

    long now = Instant.now().getEpochSecond();
    Long exp = asLong(claims.get("exp"));
    if (exp == null || now >= exp) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "token expired");
    }

    List<String> roles = readRoles(claims.get("roles"));

    return new DecodedAccessToken(sub, roles, claims);
  }

  private List<String> readRoles(Object rolesObj) {
    if (rolesObj instanceof List<?> list) {
      return list.stream()
          .map(v -> v == null ? "" : String.valueOf(v))
          .filter(s -> !s.isBlank())
          .map(s -> s.startsWith("ROLE_") ? s.substring(5) : s)
          .map(s -> s.toUpperCase(Locale.ROOT))
          .distinct()
          .toList();
    }
    return List.of();
  }

  private String asString(Object v) {
    return v == null ? null : String.valueOf(v);
  }

  private Long asLong(Object v) {
    if (v == null) return null;
    if (v instanceof Number n) return n.longValue();
    try {
      return Long.parseLong(String.valueOf(v));
    } catch (Exception e) {
      return null;
    }
  }

  private byte[] jsonBytes(Object obj) {
    try {
      return objectMapper.writeValueAsBytes(obj);
    } catch (Exception e) {
      throw new IllegalStateException("JWT JSON serialization failed", e);
    }
  }

  private Map<String, Object> readJsonMap(byte[] bytes) {
    try {
      return objectMapper.readValue(bytes, new TypeReference<Map<String, Object>>() {});
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid token json");
    }
  }

  private String b64Url(byte[] bytes) {
    return B64_URL.encodeToString(bytes);
  }

  private byte[] b64UrlDecode(String s) {
    try {
      return B64_URL_DEC.decode(s);
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid token encoding");
    }
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

