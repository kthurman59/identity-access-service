package com.kevdev.iam.config;

import com.kevdev.iam.security.SecurityErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  private final SecurityErrorHandler errorHandler;

  public SecurityConfig(SecurityErrorHandler errorHandler) {
    this.errorHandler = errorHandler;
  }

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http,
                                          AuthenticationManager authenticationManager) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/**", "/auth/login", "/auth/refresh", "/error").permitAll()
            .requestMatchers("/admin/**").hasRole("ADMIN")
            .requestMatchers("/secure/**").authenticated()
            .anyRequest().permitAll()
        )
        .exceptionHandling(h -> h
            .authenticationEntryPoint(errorHandler)
            .accessDeniedHandler(errorHandler))
        .httpBasic(Customizer.withDefaults());

    return http.build();
  }
}

