package com.kevdev.iam.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      TenantKeyFilter tenantKeyFilter,
      RestAuthenticationEntryPoint authenticationEntryPoint,
      RestAccessDeniedHandler accessDeniedHandler
  ) throws Exception {

    http.csrf(csrf -> csrf.disable());

    http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

    http.exceptionHandling(ex -> ex
        .authenticationEntryPoint(authenticationEntryPoint)
        .accessDeniedHandler(accessDeniedHandler)
    );

    http.authorizeHttpRequests(auth -> auth
        .requestMatchers("/auth/login", "/auth/refresh").permitAll()
        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
        .requestMatchers("/ping").permitAll()
        .requestMatchers("/admin/ping").hasAuthority("ADMIN")
        .anyRequest().authenticated()
    );

    http.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

    http.addFilterBefore(tenantKeyFilter, BearerTokenAuthenticationFilter.class);
    http.addFilterBefore(tenantKeyFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}

