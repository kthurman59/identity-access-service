package com.kevdev.iam.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.annotation.Resource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthRefreshFlowIT {

  @Resource MockMvc mvc;
  @Resource ObjectMapper om;

  @Test
  void loginThenRefreshReturnsNewPair() throws Exception {
    var login = mvc.perform(post("/auth/login")
            .contentType("application/json")
            .content("{\"username\":\"user1\",\"password\":\"password1\"}"))
        .andExpect(status().isOk())
        .andReturn();

    String refreshToken = om.readTree(login.getResponse().getContentAsString()).get("refreshToken").asText();

    mvc.perform(post("/auth/refresh")
            .contentType("application/json")
            .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").exists())
        .andExpect(jsonPath("$.refreshToken").exists());
  }
}

