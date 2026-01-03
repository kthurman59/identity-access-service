package com.kevdev.iam.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Component
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

  @Override
  public void commence(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull AuthenticationException authException) throws IOException {

    writeJsonError(response, request, HttpStatus.UNAUTHORIZED, "Unauthorized",
        authException.getMessage());
  }

  @Override
  public void handle(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull AccessDeniedException accessDeniedException) throws IOException {

    writeJsonError(response, request, HttpStatus.FORBIDDEN, "Forbidden",
        accessDeniedException.getMessage());
  }

  private void writeJsonError(HttpServletResponse res, HttpServletRequest req,
                              HttpStatus status, String error, String message) throws IOException {
    res.setStatus(status.value());
    res.setContentType("application/json;charset=UTF-8");

    String path = req.getRequestURI();
    String requestId = firstNonEmpty(
        req.getHeader("X-Request-Id"),
        req.getHeader("X-Request-ID"),
        req.getHeader("X-Correlation-Id"),
        stringAttr(req, "requestId"),
        stringAttr(req, "correlationId"),
        ""
    );

    String body = toJson(
        Instant.now().toString(),
        requestId,
        status.value(),
        error,
        message != null ? message : error,
        path
    );
    res.getOutputStream().write(body.getBytes());
  }

  private static String stringAttr(HttpServletRequest req, String name) {
    Object v = req.getAttribute(name);
    return v == null ? null : v.toString();
  }

  private static String firstNonEmpty(String... vals) {
    for (String v : vals) if (v != null && !v.isBlank()) return v;
    return "";
  }

  private static String toJson(String timestamp, String requestId, int status,
                               String error, String message, String path) {
    return "{"
        + "\"timestamp\":\"" + esc(timestamp) + "\","
        + "\"requestId\":\"" + esc(requestId) + "\","
        + "\"status\":" + status + ","
        + "\"error\":\"" + esc(error) + "\","
        + "\"message\":\"" + esc(message) + "\","
        + "\"path\":\"" + esc(path) + "\","
        + "\"fieldErrors\":[]"
        + "}";
  }

  private static String esc(String s) {
    if (s == null) return "";
    StringBuilder b = new StringBuilder(s.length() + 16);
    for (char c : s.toCharArray()) {
      switch (c) {
        case '"' -> b.append("\\\"");
        case '\\' -> b.append("\\\\");
        case '\b' -> b.append("\\b");
        case '\f' -> b.append("\\f");
        case '\n' -> b.append("\\n");
        case '\r' -> b.append("\\r");
        case '\t' -> b.append("\\t");
        default -> {
          if (c < 0x20) {
            String hex = Integer.toHexString(c);
            b.append("\\u");
            for (int i = hex.length(); i < 4; i++) b.append('0');
            b.append(hex);
          } else {
            b.append(c);
          }
        }
      }
    }
    return b.toString();
  }
}

