package com.kevdev.iam.security;

import java.util.List;
import java.util.stream.Collectors;
import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class SecurityUserDetailsService implements UserDetailsService {
  private final JdbcTemplate jdbc;

  public SecurityUserDetailsService(DataSource ds) {
    this.jdbc = new JdbcTemplate(ds);
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    var users = jdbc.query("""
        select u.username, u.password_hash, u.enabled, u.locked
        from iam_user u
        where u.username = ?
        """,
        (rs, n) -> new DbRow(
            rs.getString("username"),
            rs.getString("password_hash"),
            rs.getBoolean("enabled"),
            rs.getBoolean("locked")),
        username);

    if (users.isEmpty()) {
      throw new UsernameNotFoundException("user not found");
    }
    var u = users.get(0);

    List<String> roles = jdbc.query("""
        select r.name
        from iam_role r
        join iam_user_role ur on ur.role_id = r.id
        join iam_user u on u.id = ur.user_id
        where u.username = ?
        """,
        (rs, n) -> rs.getString("name"),
        username);

    var authorities = roles.stream()
        .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toList());

    return User.builder()
        .username(u.username())
        .password(u.passwordHash())          // includes "{bcrypt}" prefix from your seeds
        .accountLocked(u.locked())
        .disabled(!u.enabled())
        .authorities(authorities)
        .build();
  }

  private record DbRow(String username, String passwordHash, boolean enabled, boolean locked) {}
}

