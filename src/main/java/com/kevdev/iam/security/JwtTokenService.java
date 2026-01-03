package com.kevdev.iam.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class JwtTokenService {

    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final String issuer;
    private final Duration accessTtl;

    public JwtTokenService(
            ObjectMapper objectMapper,
            @Value("${ias.jwt.secret.b64}") String secretB64,
            @Value("${ias.jwt.issuer:ias}") String issuer,
            @Value("${ias.jwt.access-ttl-minutes:15}") long accessTtlMinutes
    ) {
        this.objectMapper = objectMapper;
        if (secretB64 == null || secretB64.isBlank()) {
            throw new IllegalStateException("ias.jwt.secret.b64 is required");
        }
        byte[] key = Base64.getDecoder().decode(secretB64.trim());
        if (key.length < 32) {
            throw new IllegalStateException("ias.jwt.secret.b64 must decode to at least 32 bytes");
        }
        this.secret = key;
        this.issuer = issuer;
        this.accessTtl = Duration.ofMinutes(accessTtlMinutes);
    }

    public String issueAccessToken(UserDetails user) {
        long iat = Instant.now().getEpochSecond();
        long exp = iat + accessTtl.toSeconds();

        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                .toList();

        Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("iss", issuer);
        payload.put("sub", user.getUsername());
        payload.put("iat", iat);
        payload.put("exp", exp);
        payload.put("roles", roles);

        String headerB64 = b64Url(jsonBytes(header));
        String payloadB64 = b64Url(jsonBytes(payload));
        String signingInput = headerB64 + "." + payloadB64;
        String sigB64 = b64Url(hmacSha256(signingInput));

        return signingInput + "." + sigB64;
    }

    public Map<String, Object> parseAndVerify(String jwt) {
        String[] parts = jwt.split("\\.");
        if (parts.length != 3) throw new IllegalArgumentException("invalid_jwt");

        String signingInput = parts[0] + "." + parts[1];
        byte[] expected = hmacSha256(signingInput);
        byte[] actual = Base64.getUrlDecoder().decode(parts[2]);
        if (!constantTimeEquals(expected, actual)) throw new IllegalArgumentException("invalid_signature");

        byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
        Map<String, Object> payload = readMap(payloadBytes);

        Object iss = payload.get("iss");
        if (iss == null || !issuer.equals(iss.toString())) throw new IllegalArgumentException("invalid_issuer");

        Object expObj = payload.get("exp");
        long exp = expObj instanceof Number ? ((Number) expObj).longValue() : Long.parseLong(expObj.toString());
        if (Instant.now().getEpochSecond() >= exp) throw new IllegalArgumentException("token_expired");

        return payload;
    }

    private byte[] jsonBytes(Object value) {
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (Exception e) {
            throw new IllegalStateException("json_failed", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readMap(byte[] json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            throw new IllegalStateException("json_failed", e);
        }
    }

    private String b64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private byte[] hmacSha256(String input) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("hmac_failed", e);
        }
    }

    private boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        int r = 0;
        for (int i = 0; i < a.length; i++) r |= a[i] ^ b[i];
        return r == 0;
    }
}

