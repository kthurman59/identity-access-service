package com.kevdev.iam.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  public TenantKeyFilter tenantKeyFilter(TenantContext tenantContext) {
    return new TenantKeyFilter(tenantContext);
  }

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      TenantKeyFilter tenantKeyFilter,
      AuthenticationManager authenticationManager
  ) throws Exception {

    http
        .csrf(csrf -> csrf.disable())
        .cors(Customizer.withDefaults())
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(reg -> reg
            .requestMatchers("/actuator/health", "/actuator/info").permitAll()
            .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
            .requestMatchers(HttpMethod.POST, "/auth/refresh").permitAll()
            .requestMatchers(HttpMethod.POST, "/auth/logout").permitAll()
            .anyRequest().authenticated()
        );

    // Add once before UsernamePasswordAuthenticationFilter
    http.addFilterBefore(tenantKeyFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}

