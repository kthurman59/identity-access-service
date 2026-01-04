package com.kevdev.iam.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  JwtAuthenticationConverter jwtAuthConverter() {
    JwtGrantedAuthoritiesConverter c = new JwtGrantedAuthoritiesConverter();
    c.setAuthoritiesClaimName("roles");
    c.setAuthorityPrefix("ROLE_");
    JwtAuthenticationConverter conv = new JwtAuthenticationConverter();
    conv.setJwtGrantedAuthoritiesConverter(c);
    return conv;
  }

  @Bean
  SecurityFilterChain security(HttpSecurity http, JwtAuthenticationConverter jwtConv) throws Exception {
    http.csrf(csrf -> csrf.disable());
    http.authorizeHttpRequests(auth -> auth
        .requestMatchers(HttpMethod.POST, "/auth/login", "/auth/refresh").permitAll()
        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
        .anyRequest().authenticated()
    );
    http.oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(jwtConv)));
    return http.build();
  }
}

