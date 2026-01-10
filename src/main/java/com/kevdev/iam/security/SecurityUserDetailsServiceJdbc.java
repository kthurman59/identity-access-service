package com.kevdev.iam.security;

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service("securityUserDetailsServiceJdbc")
public class SecurityUserDetailsServiceJdbc implements UserDetailsService {

  private final JdbcTemplate jdbc;
  private final TenantContext tenantContext;

  public SecurityUserDetailsServiceJdbc(JdbcTemplate jdbc, TenantContext tenantContext) {
    this.jdbc = jdbc;
    this.tenantContext = tenantContext;
  }

  private record UserRow(UUID tenantId, String username, String passwordHash, boolean enabled) {}

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    String tenantKey = tenantContext.requiredTenantKey();

    String sqlUser = """
        select u.tenant_id,
               u.username,
               u.password_hash,
               u.enabled
          from iam_user u
          join iam_tenant t on t.id = u.tenant_id
         where t.tenant_key = ?
           and u.username = ?
        """;

    UserRow u = jdbc.query(sqlUser, rs -> {
      if (!rs.next()) return null;
      return new UserRow(
          (UUID) rs.getObject("tenant_id"),
          rs.getString("username"),
          rs.getString("password_hash"),
          rs.getBoolean("enabled")
      );
    }, tenantKey, username);

    if (u == null) throw new UsernameNotFoundException("User not found");

    List<GrantedAuthority> auths;
    try {
      String sqlRoles = """
          select r.name
            from iam_user_role ur
            join iam_role r on r.id = ur.role_id
           where ur.tenant_id = ?
             and ur.username = ?
          """;

      auths = jdbc.query(
          sqlRoles,
          (rs, i) -> (GrantedAuthority) new SimpleGrantedAuthority(rs.getString("name")),
          u.tenantId(),
          u.username()
      );
    } catch (BadSqlGrammarException e) {
      auths = List.of();
    }

    return User.withUsername(u.username())
        .password(u.passwordHash())
        .disabled(!u.enabled())
        .authorities(auths)
        .build();
  }
}

