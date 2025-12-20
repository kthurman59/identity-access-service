package com.kevdev.iamauth.infrastructure.security;

import com.kevdev.iamauth.application.ports.security.TokenService;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenServiceTest {

    @Test
    void issuesAndDecodesAccessToken() {
        String secretBase64 = Base64.getEncoder().encodeToString(
                "devsecretdevsecretdevsecretdevsecret".getBytes(StandardCharsets.UTF_8)
        );
        SecurityProperties props = new SecurityProperties("iamauth", secretBase64, 900);

        byte[] keyBytes = Base64.getDecoder().decode(props.secret());
        SecretKey key = new SecretKeySpec(keyBytes, "HmacSHA256");

        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(key));
        JwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();

        TokenService service = new JwtTokenService(encoder, decoder, props);

        String token = service.issueAccessToken("user1", List.of("ROLE_USER"), Map.of("tenant", "t1"));
        TokenService.DecodedToken decoded = service.decodeAndValidateAccessToken(token);

        assertThat(decoded.subject()).isEqualTo("user1");
        assertThat(decoded.roles()).contains("ROLE_USER");
        assertThat(decoded.claims().get("tenant")).isEqualTo("t1");
    }

    @Test
    void issuesRefreshTokenAsOpaqueRandomString() {
        String secretBase64 = Base64.getEncoder().encodeToString(
                "devsecretdevsecretdevsecretdevsecret".getBytes(StandardCharsets.UTF_8)
        );
        SecurityProperties props = new SecurityProperties("iamauth", secretBase64, 900);

        byte[] keyBytes = Base64.getDecoder().decode(props.secret());
        SecretKey key = new SecretKeySpec(keyBytes, "HmacSHA256");

        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(key));
        JwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();

        TokenService service = new JwtTokenService(encoder, decoder, props);

        String rt1 = service.issueRefreshToken();
        String rt2 = service.issueRefreshToken();

        assertThat(rt1).isNotBlank();
        assertThat(rt2).isNotBlank();
        assertThat(rt1).isNotEqualTo(rt2);
    }
}

