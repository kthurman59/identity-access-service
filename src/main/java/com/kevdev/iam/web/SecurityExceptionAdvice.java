package com.kevdev.iam.web;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class SecurityExceptionAdvice {

    @ResponseBody
    @ExceptionHandler({ AuthorizationDeniedException.class, AccessDeniedException.class })
    public ResponseEntity<String> handleForbidden(HttpServletRequest request, Exception ex) {
        return buildJsonResponse(request, HttpStatus.FORBIDDEN, "Forbidden", "Forbidden");
    }

    @ResponseBody
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<String> handleUnauthorized(HttpServletRequest request, AuthenticationException ex) {
        return buildJsonResponse(request, HttpStatus.UNAUTHORIZED, "Unauthorized", "Unauthorized");
    }

    private ResponseEntity<String> buildJsonResponse(HttpServletRequest request,
                                                     HttpStatus status,
                                                     String error,
                                                     String message) {
        String requestId = firstNonBlank(
            getHeaderSafe(request, "XRequestId"),
            asString(request.getAttribute("requestId")),
            asString(request.getAttribute("XRequestId"))
        );
        if (requestId == null) requestId = "";

        String body = buildJson(
            Instant.now().toString(),
            requestId,
            status.value(),
            error,
            message,
            request.getRequestURI()
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));
        return new ResponseEntity<>(body, headers, status);
    }

    private static String getHeaderSafe(HttpServletRequest req, String name) {
        try {
            return req.getHeader(name);
        } catch (Exception e) {
            return null;
        }
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
                case '\"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
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

