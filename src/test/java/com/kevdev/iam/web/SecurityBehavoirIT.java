package com.kevdev.iam.web;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.annotation.Resource;

import java.time.Instant;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityBehaviorIT {

  @Resource
  MockMvc mvc;

  @TestConfiguration
  static class JwtDecoderStub {
    @Bean
    JwtDecoder jwtDecoder() {
      return token -> new Jwt(
          token, Instant.now(), Instant.now().plusSeconds(600),
          Map.of("alg","HS256"), Map.of("sub","stub")
      );
    }
  }

  @Test
  void loginIsPublic() throws Exception {
    mvc.perform(post("/auth/login")
            .contentType("application/json")
            .content("{\"username\":\"user1\",\"password\":\"password1\"}"))
        .andExpect(status().isOk());
  }

  @Test
  void securePingIsProtected() throws Exception {
    mvc.perform(get("/secure/ping")).andExpect(status().isUnauthorized());
  }
}

