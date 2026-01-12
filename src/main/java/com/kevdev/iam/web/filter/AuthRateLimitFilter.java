package com.kevdev.iam.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in memory rate limit for /auth/login and /auth/refresh
 * No external deps and good enough for dev and demo
 */
@Component
@Order(10)
public class AuthRateLimitFilter extends OncePerRequestFilter {

  private static final String LOGIN = "/auth/login";
  private static final String REFRESH = "/auth/refresh";

  private static final int LOGIN_CAPACITY = 5;
  private static final int REFRESH_CAPACITY = 30;
  private static final Duration WINDOW = Duration.ofMinutes(1);

  private final Map<String, Deque<Long>> buckets = new ConcurrentHashMap<>();

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String p = request.getRequestURI();
    return !(p.equals(LOGIN) || p.equals(REFRESH));
  }

  @Override
  protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    String path = req.getRequestURI();
    int capacity = path.equals(LOGIN) ? LOGIN_CAPACITY : REFRESH_CAPACITY;
    long now = System.currentTimeMillis();
    long cutoff = now - WINDOW.toMillis();

    String tenant = req.getHeader("X-Tenant-Key");
    String ip = clientIp(req);
    String key = path + "|" + Objects.toString(tenant, "no-tenant") + "|" + ip;

    Deque<Long> q = buckets.computeIfAbsent(key, k -> new ArrayDeque<>());
    synchronized (q) {
      while (!q.isEmpty() && q.peekFirst() < cutoff) {
        q.pollFirst();
      }
      if (q.size() >= capacity) {
        res.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        res.setHeader("Retry-After", String.valueOf(WINDOW.getSeconds()));
        res.setContentType("application/json");
        res.getWriter().write("{\"error\":\"rate_limit\",\"message\":\"Too many requests\"}");
        return;
      }
      q.addLast(now);
    }
    chain.doFilter(req, res);
  }

  private static String clientIp(HttpServletRequest req) {
    String xff = req.getHeader("X-Forwarded-For");
    if (xff != null && !xff.isBlank()) {
      int comma = xff.indexOf(',');
      return comma > 0 ? xff.substring(0, comma).trim() : xff.trim();
    }
    return req.getRemoteAddr();
  }
}

