package com.kevdev.iam.security;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

public class RestAccessDeniedHandler implements AccessDeniedHandler {

    public RestAccessDeniedHandler() {}

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        writeApiError(response, request, 403, "Forbidden", "Forbidden");
    }

    static void writeApiError(HttpServletResponse response,
                              HttpServletRequest request,
                              int status,
                              String error,
                              String message) throws IOException {

        String requestId = firstNonBlank(
            response.getHeader("XRequestId"),
            asString(request.getAttribute("requestId")),
            asString(request.getAttribute("XRequestId"))
        );

        if (requestId == null) requestId = "";

        String json = buildJson(
            Instant.now().toString(),
            requestId,
            status,
            error,
            message,
            request.getRequestURI()
        );

        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");

        ServletOutputStream out = response.getOutputStream();
        out.write(json.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    private static String buildJson(String timestamp,
                                    String requestId,
                                    int status,
                                    String error,
                                    String message,
                                    String path) {

        StringBuilder sb = new StringBuilder(256);
        sb.append('{');
        sb.append("\"timestamp\":\"").append(escapeJson(timestamp)).append("\",");
        sb.append("\"requestId\":\"").append(escapeJson(requestId)).append("\",");
        sb.append("\"status\":").append(status).append(',');
        sb.append("\"error\":\"").append(escapeJson(error)).append("\",");
        sb.append("\"message\":\"").append(escapeJson(message)).append("\",");
        sb.append("\"path\":\"").append(escapeJson(path)).append("\",");
        sb.append("\"fieldErrors\":[]");
        sb.append('}');
        return sb.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
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

    private static String firstNonBlank(String a, String b, String c) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        if (c != null && !c.isBlank()) return c;
        return null;
    }

    private static String asString(Object o) {
        return (o == null) ? null : String.valueOf(o);
    }
}

