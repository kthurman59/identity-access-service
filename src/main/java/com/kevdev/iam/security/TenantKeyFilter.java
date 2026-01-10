package com.kevdev.iam.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TenantKeyFilter extends OncePerRequestFilter {

  private final TenantContext tenantContext;

  public TenantKeyFilter(TenantContext tenantContext) {
    this.tenantContext = tenantContext;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    try {
      String tenantKey = request.getHeader("X-Tenant-Key");
      if (tenantKey != null && !tenantKey.isBlank()) tenantContext.setTenantKey(tenantKey);
      filterChain.doFilter(request, response);
    } finally {
      tenantContext.clear();
    }
  }
}

