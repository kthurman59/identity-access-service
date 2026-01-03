// src/main/java/com/kevdev/iam/security/SecurityUserDetailsService.java
package com.kevdev.iam.security;

import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Primary
@Service("securityUserDetailsService")
@Transactional(readOnly = true)
public class SecurityUserDetailsService implements UserDetailsService {

  private final JdbcTemplate jdbc;

  public SecurityUserDetailsService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  private static final RowMapper<UserRow> USER_MAPPER = (rs, n) -> new UserRow(
      rs.getLong("id"),
      rs.getString("username"),
      rs.getString("password"),
      rs.getBoolean("enabled")
  );

  private record UserRow(long id, String username, String password, boolean enabled) {}

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    final String sqlUser = """
        select id, username, password,
               coalesce(enabled, true) as enabled
        from app_user
        where lower(username) = lower(?)
        """;

    final UserRow u = jdbc.query(sqlUser, USER_MAPPER, username)
        .stream()
        .findFirst()
        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

    List<GrantedAuthority> authorities;
    try {
      // Adjust table names if your schema differs
      final String sqlRoles = """
          select r.name
          from role r
          join user_role ur on ur.role_id = r.id
          where ur.user_id = ?
          """;
      authorities = jdbc.query(sqlRoles, (rs, n) -> new SimpleGrantedAuthority(
          rs.getString("name").startsWith("ROLE_") ? rs.getString("name") : "ROLE_" + rs.getString("name")
      ), u.id());
    } catch (DataAccessException e) {
      authorities = List.of(); // proceed without roles if schema names differ, fix sqlRoles later
    }

    return User.withUsername(u.username())
        .password(u.password())
        .authorities(authorities)
        .accountExpired(false)
        .accountLocked(false)
        .credentialsExpired(false)
        .disabled(!u.enabled())
        .build();
  }
}

