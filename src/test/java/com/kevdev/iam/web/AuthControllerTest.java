package com.kevdev.iam.web;

import com.kevdev.iam.dto.LoginRequest;
import com.kevdev.iam.service.AuthService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.annotation.Resource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

  @Resource
  MockMvc mvc;

  @MockBean
  AuthService authService;

  @Test
  void loginReturnsTokens() throws Exception {
    Mockito.when(authService.login("user1", "password1"))
        .thenReturn(new AuthService.Result("at", "rt"));

    mvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"user1\",\"password\":\"password1\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").value("at"))
        .andExpect(jsonPath("$.refreshToken").value("rt"));
  }
}

