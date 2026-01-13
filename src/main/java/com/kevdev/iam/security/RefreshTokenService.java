package com.kevdev.iam.security;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class RefreshTokenService {

  public record TokenPair(
      String username,
      List<String> roles,
      String accessToken,
      String refreshToken
  ) {}

  private static final String INSERT_SQL = """
      INSERT INTO refresh_token
        (tenant_key, username, token_hash, created_at, updated_at, expires_at)
      VALUES (?, ?, ?, ?, ?, ?)
      """;

  private static final String SELECT_ACTIVE_SQL = """
      SELECT tenant_key, username, token_hash
      FROM refresh_token
      WHERE tenant_key = ?
        AND revoked_at IS NULL
        AND (expires_at IS NULL OR expires_at > ?)
      """;

  private static final String REVOKE_SQL = """
      UPDATE refresh_token
      SET revoked_at = ?, updated_at = ?
      WHERE tenant_key = ? AND username = ? AND token_hash = ? AND revoked_at IS NULL
      """;

  private final JdbcTemplate jdbc;
  private final PasswordEncoder passwordEncoder;
  private final TokenService tokenService;
  private final Duration refreshTtl;
  private final Clock clock;
  private final SecureRandom random = new SecureRandom();

  private static final RowMapper<RtRow> RT_MAPPER = (rs, i) ->
      new RtRow(rs.getString("tenant_key"), rs.getString("username"), rs.getString("token_hash"));

  public RefreshTokenService(
      JdbcTemplate jdbc,
      PasswordEncoder passwordEncoder,
      TokenService tokenService,
      Duration refreshTokenTtl,
      Clock clock
  ) {
    this.jdbc = Objects.requireNonNull(jdbc);
    this.passwordEncoder = Objects.requireNonNull(passwordEncoder);
    this.tokenService = Objects.requireNonNull(tokenService);
    this.refreshTtl = Objects.requireNonNull(refreshTokenTtl);
    this.clock = Objects.requireNonNull(clock);
  }

  @Transactional
  public TokenPair mintOnLogin(UserDetails user, String tenantKey) {
    String username = user.getUsername();
    List<String> roles = user.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .toList();

    String rawRefresh = generateRefreshToken();
    String hash = passwordEncoder.encode(rawRefresh);
    Instant now = Instant.now(clock);
    Instant exp = now.plus(refreshTtl);

    jdbc.update(INSERT_SQL, tenantKey, username, hash, now, now, exp);

    String subject = tenantKey + ":" + username;
    String access = tokenService.issueAccessToken(subject, roles, Map.of("tenant", tenantKey));

    return new TokenPair(username, roles, access, rawRefresh);
  }

  @Transactional
  public TokenPair rotate(String tenantKey, String refreshTokenPlaintext) {
    Instant now = Instant.now(clock);
    var candidates = jdbc.query(SELECT_ACTIVE_SQL, RT_MAPPER, tenantKey, now);

    String username = null;
    String matchedHash = null;

    for (var row : candidates) {
      String hash = row.tokenHash();
      if (hash != null && passwordEncoder.matches(refreshTokenPlaintext, hash)) {
        username = row.username();
        matchedHash = hash;
        break;
      }
    }

    if (username == null) {
      throw new IllegalArgumentException("refresh token not found or inactive");
    }

    jdbc.update(REVOKE_SQL, now, now, tenantKey, username, matchedHash);

    String newRaw = generateRefreshToken();
    String newHash = passwordEncoder.encode(newRaw);
    jdbc.update(INSERT_SQL, tenantKey, username, newHash, now, now, now.plus(refreshTtl));

    List<String> roles = List.of("ADMIN");
    String subject = tenantKey + ":" + username;
    String access = tokenService.issueAccessToken(subject, roles, Map.of("tenant", tenantKey));

    return new TokenPair(username, roles, access, newRaw);
  }

  @Transactional
  public void revoke(String tenantKey, String refreshTokenPlaintext) {
    Instant now = Instant.now(clock);
    var candidates = jdbc.query(SELECT_ACTIVE_SQL, RT_MAPPER, tenantKey, now);

    for (var row : candidates) {
      String hash = row.tokenHash();
      if (hash != null && passwordEncoder.matches(refreshTokenPlaintext, hash)) {
        jdbc.update(REVOKE_SQL, now, now, tenantKey, row.username(), hash);
        return;
      }
    }
  }

  private String generateRefreshToken() {
    byte[] buf = new byte[32];
    random.nextBytes(buf);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
  }

  private record RtRow(String tenantKey, String username, String tokenHash) { }
}

