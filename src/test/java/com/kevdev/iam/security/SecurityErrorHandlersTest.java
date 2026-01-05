package com.kevdev.iam.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.*;

class SecurityErrorHandlersTest {

  @Test
  void writes401JsonBodyWithoutJackson() throws Exception {
    JsonAuthenticationEntryPoint ep = new JsonAuthenticationEntryPoint();

    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/secure/ping");
    MockHttpServletResponse res = new MockHttpServletResponse();

    ep.commence(req, res, new BadCredentialsException("no"));

    assertEquals(401, res.getStatus());
    assertNotNull(res.getContentType());
    assertTrue(res.getContentType().startsWith("application/json"));

    String body = res.getContentAsString();
    assertTrue(body.contains("\"timestamp\":\""));
    assertTrue(body.contains("\"status\":401"));
    assertTrue(body.contains("\"error\":\"Unauthorized\""));
    assertTrue(body.contains("\"path\":\"/secure/ping\""));
    assertTrue(body.contains("\"fieldErrors\":[]"));
  }

  @Test
  void writes403JsonBodyWithoutJackson() throws Exception {
    JsonAccessDeniedHandler h = new JsonAccessDeniedHandler();

    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/admin/ping");
    MockHttpServletResponse res = new MockHttpServletResponse();

    h.handle(req, res, new AccessDeniedException("nope"));

    assertEquals(403, res.getStatus());
    assertNotNull(res.getContentType());
    assertTrue(res.getContentType().startsWith("application/json"));

    String body = res.getContentAsString();
    assertTrue(body.contains("\"timestamp\":\""));
    assertTrue(body.contains("\"status\":403"));
    assertTrue(body.contains("\"error\":\"Forbidden\""));
    assertTrue(body.contains("\"path\":\"/admin/ping\""));
    assertTrue(body.contains("\"fieldErrors\":[]"));
  }
}

