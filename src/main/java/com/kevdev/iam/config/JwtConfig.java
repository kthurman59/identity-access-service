package com.kevdev.iam.config;

import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
public class JwtConfig {

  @Bean
  SecretKey jwtSecretKey(@Value("${ias.jwt.secret.b64}") String b64) {
    if (b64 == null || b64.isBlank()) {
      throw new IllegalStateException("Missing ias.jwt.secret.b64");
    }

    byte[] keyBytes = Base64.getDecoder().decode(b64);

    if (keyBytes.length < 32) {
      throw new IllegalStateException("ias.jwt.secret.b64 must decode to at least 32 bytes for HS256");
    }

    return new SecretKeySpec(keyBytes, "HmacSHA256");
  }

  @Bean
  JwtEncoder jwtEncoder(SecretKey key) {
    ImmutableSecret<SecurityContext> secret = new ImmutableSecret<>(key);
    return new NimbusJwtEncoder(secret);
  }

  @Bean
  JwtDecoder jwtDecoder(SecretKey key) {
    return NimbusJwtDecoder.withSecretKey(key)
        .macAlgorithm(MacAlgorithm.HS256)
        .build();
  }
}

