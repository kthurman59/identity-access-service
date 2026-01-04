package com.kevdev.iam.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;

@Configuration
public class JwtConfig {

  @Value("${ias.jwt.secret.b64:}")
  private String secretB64;

  @Value("${ias.jwt.secret.value:}")
  private String secretRaw;

  private byte[] material() {
    if (secretB64 != null && !secretB64.isBlank()) {
      return java.util.Base64.getDecoder().decode(secretB64);
    }
    byte[] raw = (secretRaw == null || secretRaw.isBlank())
        ? "demo-secret-demo-secret-demo-secret-32b".getBytes(java.nio.charset.StandardCharsets.UTF_8)
        : secretRaw.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    if (raw.length < 32) raw = java.util.Arrays.copyOf(raw, 32);
    return raw;
  }

  @Bean
  SecretKey jwtSecretKey() {
    return new SecretKeySpec(material(), "HmacSHA256");
  }

  @Bean
  JwtEncoder jwtEncoder(SecretKey key) {
    return new NimbusJwtEncoder(new ImmutableSecret<>(key));
  }

  @Bean
  JwtDecoder jwtDecoder(SecretKey key) {
    return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
  }
}

