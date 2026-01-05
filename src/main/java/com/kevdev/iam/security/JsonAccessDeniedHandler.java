package com.kevdev.iam.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

  @Override
  public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
      throws IOException {

    write(response, request, 403, "Forbidden", "Forbidden");
  }

  static void write(
      HttpServletResponse response,
      HttpServletRequest request,
      int status,
      String error,
      String message
  ) throws IOException {

    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());

    response.getWriter().write(buildBody(request, status, error, message));
  }

  static String buildBody(HttpServletRequest request, int status, String error, String message) {
    String ts = Instant.now().toString();
    String path = request.getRequestURI();
    String method = request.getMethod();

    return "{"
        + "\"timestamp\":\"" + esc(ts) + "\","
        + "\"status\":" + status + ","
        + "\"error\":\"" + esc(error) + "\","
        + "\"message\":\"" + esc(message) + "\","
        + "\"path\":\"" + esc(path) + "\","
        + "\"method\":\"" + esc(method) + "\","
        + "\"fieldErrors\":[]"
        + "}";
  }

  private static String esc(String s) {
    if (s == null) return "";
    StringBuilder sb = new StringBuilder(s.length() + 16);
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
        case '\\' -> sb.append("\\\\");
        case '"' -> sb.append("\\\"");
        case '\b' -> sb.append("\\b");
        case '\f' -> sb.append("\\f");
        case '\n' -> sb.append("\\n");
        case '\r' -> sb.append("\\r");
        case '\t' -> sb.append("\\t");
        default -> {
          if (c < 0x20) {
            sb.append(String.format("\\u%04x", (int) c));
          } else {
            sb.append(c);
          }
        }
      }
    }
    return sb.toString();
  }
}

