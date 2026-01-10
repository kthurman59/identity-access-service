package com.kevdev.iam.security;

import org.springframework.stereotype.Component;

@Component
public class TenantContext {

  private static final ThreadLocal<String> TENANT_KEY = new ThreadLocal<>();

  public void setTenantKey(String tenantKey) {
    TENANT_KEY.set(tenantKey);
  }

  public void clear() {
    TENANT_KEY.remove();
  }

  public String requiredTenantKey() {
    String v = TENANT_KEY.get();
    if (v == null || v.isBlank()) throw new IllegalStateException("Missing X-Tenant-Key");
    return v;
  }
}

