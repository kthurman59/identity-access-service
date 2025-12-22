package com.kevdev.iam.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kevdev.iam.security.RestAccessDeniedHandler;
import com.kevdev.iam.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        http.csrf(csrf -> csrf.disable());

        http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.formLogin(fl -> fl.disable());
        http.httpBasic(hb -> hb.disable());

        http.exceptionHandling(eh -> eh
                .authenticationEntryPoint(new RestAuthenticationEntryPoint(objectMapper))
                .accessDeniedHandler(new RestAccessDeniedHandler(objectMapper))
        );

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/auth/refresh").permitAll()

                .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()

                .requestMatchers(HttpMethod.GET, "/secure/ping").authenticated()
                .requestMatchers(HttpMethod.GET, "/admin/ping").hasAuthority("iam.user.read")
                .anyRequest().authenticated()
        );

        http.oauth2ResourceServer(oauth2 -> oauth2.jwt());

        return http.build();
    }
}

