package com.kevdev.iam.security;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

import org.springframework.context.annotation.Primary;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Primary
@Service("securityUserDetailsServiceJdbc")
public class SecurityUserDetailsServiceJdbc implements UserDetailsService {

  private final JdbcTemplate jdbc;
  private final String pwdColumn;

  public SecurityUserDetailsServiceJdbc(JdbcTemplate jdbc) {
    this.jdbc = jdbc;

    String sql =
        """
        select column_name
        from information_schema.columns
        where table_schema = 'public'
          and table_name = 'iam_user'
          and column_name in
              ('password','password_hash','encoded_password','passwd','pwd_hash','password_encoded')
        order by case column_name
                   when 'password' then 1
                   when 'password_hash' then 2
                   when 'encoded_password' then 3
                   when 'passwd' then 4
                   when 'pwd_hash' then 5
                   when 'password_encoded' then 6
                   else 99
                 end
        limit 1
        """;
    String col = jdbc.query(sql, rs -> rs.next() ? rs.getString(1) : null);
    if (col == null) {
      throw new IllegalStateException("No password column on iam_user");
    }
    this.pwdColumn = col;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    try {
      var u = jdbc.queryForObject(
          "select id, username, enabled, " + pwdColumn + " as pwd from iam_user where username = ?",
          new Object[] { username },
          new RowMapper<UserRow>() {
            @Override
            public UserRow mapRow(ResultSet rs, int rowNum) throws SQLException {
              return new UserRow(rs.getObject("id"),
                                 rs.getString("username"),
                                 rs.getBoolean("enabled"),
                                 rs.getString("pwd"));
            }
          });

      if (u == null || u.password == null) {
        throw new UsernameNotFoundException("User not found or missing password");
      }

      List<GrantedAuthority> auths = jdbc.query(
          """
          select r.name
          from iam_role r
          join iam_user_role ur on ur.role_id = r.id
          where ur.user_id = ?
          """,
          ps -> ps.setObject(1, u.id),
          (rs, n) -> new SimpleGrantedAuthority("ROLE_" + rs.getString(1).toUpperCase(Locale.ROOT)));

      return User.withUsername(u.username)
                 .password(u.password)
                 .authorities(auths)
                 .disabled(!u.enabled)
                 .build();

    } catch (EmptyResultDataAccessException ex) {
      throw new UsernameNotFoundException("User not found");
    }
  }

  private record UserRow(Object id, String username, boolean enabled, String password) {}
}

