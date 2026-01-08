package com.kevdev.iam.security;

import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
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
public class JwtCryptoConfig {

  public static final String SIGNING_KID = "ias-hs256";

  @Bean
  public JwtEncoder jwtEncoder(@Value("${ias.jwt.secret.b64}") String secretB64) {
    byte[] keyBytes = Base64.getDecoder().decode(secretB64);

    OctetSequenceKey jwk =
        new OctetSequenceKey.Builder(keyBytes)
            .keyUse(KeyUse.SIGNATURE)
            .algorithm(JWSAlgorithm.HS256)
            .keyID(SIGNING_KID)
            .build();

    var jwkSource = new ImmutableJWKSet<SecurityContext>(new JWKSet(jwk));
    return new NimbusJwtEncoder(jwkSource);
  }

  @Bean
  public JwtDecoder jwtDecoder(@Value("${ias.jwt.secret.b64}") String secretB64) {
    byte[] keyBytes = Base64.getDecoder().decode(secretB64);
    SecretKey key = new SecretKeySpec(keyBytes, "HmacSHA256");

    return NimbusJwtDecoder
        .withSecretKey(key)
        .macAlgorithm(MacAlgorithm.HS256)
        .build();
  }
}

