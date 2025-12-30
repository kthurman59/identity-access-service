package com.kevdev.iam.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kevdev.iam.security.RestAccessDeniedHandler;
import com.kevdev.iam.security.RestAuthenticationEntryPoint;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper om,
      Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthConverter) throws Exception {

    http.csrf(csrf -> csrf.disable());
    http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    http.formLogin(fl -> fl.disable());
    http.httpBasic(hb -> hb.disable());

    http.exceptionHandling(eh -> eh
        .authenticationEntryPoint(new RestAuthenticationEntryPoint(om))
        .accessDeniedHandler(new RestAccessDeniedHandler(om))
    );

    http.authorizeHttpRequests(auth -> auth
        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
        .requestMatchers("/auth/**").permitAll()
        .requestMatchers(HttpMethod.GET, "/swagger-ui/**", "/v3/api-docs/**").permitAll()
        .requestMatchers("/admin/**").hasRole("ADMIN")
        .requestMatchers("/secure/**").hasAnyRole("USER", "ADMIN")
        .anyRequest().authenticated()
    );

    http.oauth2ResourceServer(oauth2 -> oauth2
        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter))
    );

    return http.build();
  }

  @Bean
  JwtDecoder jwtDecoder(@Value("${security.jwt.secret}") String secret) {
    SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
  }

  @Bean
  Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthConverter() {
    JwtGrantedAuthoritiesConverter perms = new JwtGrantedAuthoritiesConverter();
    perms.setAuthoritiesClaimName("permissions");
    perms.setAuthorityPrefix("");

    return jwt -> {
      Collection<GrantedAuthority> out = new HashSet<>(perms.convert(jwt));
      Object rolesClaim = jwt.getClaims().get("roles");
      if (rolesClaim instanceof Collection<?> roles) {
        for (Object r : roles) {
          String role = String.valueOf(r);
          if (!role.startsWith("ROLE_")) role = "ROLE_" + role;
          out.add(new SimpleGrantedAuthority(role));
        }
      }
      return new JwtAuthenticationToken(jwt, out, jwt.getSubject());
    };
  }
}

