package com.kevdev.iam.security;

import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

  private static final Base64.Encoder B64_URL = Base64.getUrlEncoder().withoutPadding();

  private final JdbcTemplate jdbc;
  private final Clock clock;
  private final SecureRandom rng = new SecureRandom();
  private final Duration refreshTtl;

  public RefreshTokenService(
      JdbcTemplate jdbc,
      Clock clock,
      @Value("${ias.refresh.ttl:P14D}") String refreshTtl
  ) {
    this.jdbc = jdbc;
    this.clock = clock;
    this.refreshTtl = Duration.parse(refreshTtl);
  }

  public record TokenPair(String username, List<String> roles, String refreshToken) {}

  private record RefreshRow(UUID id, String subject, Instant expiresAt, Instant revokedAt, UUID replacedBy) {}

  private record ParsedSubject(String tenantKey, String username) {}

  @Transactional
  public TokenPair mintOnLogin(String tenantKey, String username) {
    if (tenantKey == null || tenantKey.isBlank()) throw new IllegalArgumentException("Missing tenantKey");
    if (username == null || username.isBlank()) throw new IllegalArgumentException("Missing username");

    UUID tenantId = requireTenantId(tenantKey);
    String u = requireUserInTenant(tenantId, username);
    List<String> roles = loadRolesBestEffort(tenantId, u);

    UUID id = UUID.randomUUID();
    UUID familyId = UUID.randomUUID();

    String token = newToken();
    String tokenHash = sha256B64Url(token);

    insertRefreshToken(
        id,
        buildSubject(tenantKey, u),
        tokenHash,
        Instant.now(clock).plus(refreshTtl),
        tenantId,
        u,
        familyId
    );

    return new TokenPair(u, roles, token);
  }

  @Transactional
  public TokenPair rotate(String tenantKey, String presentedRefreshToken) {
    if (tenantKey == null || tenantKey.isBlank()) throw new IllegalArgumentException("Missing tenantKey");
    if (presentedRefreshToken == null || presentedRefreshToken.isBlank()) throw new IllegalArgumentException("Missing refreshToken");

    String presentedHash = sha256B64Url(presentedRefreshToken);
    RefreshRow current = findLatestByHashForUpdate(presentedHash);
    if (current == null) throw new IllegalArgumentException("Invalid refresh token");

    ParsedSubject sub = parseSubject(current.subject);
    if (!tenantKey.equals(sub.tenantKey)) throw new IllegalArgumentException("Tenant mismatch");

    Instant now = Instant.now(clock);
    if (current.expiresAt != null && current.expiresAt.isBefore(now)) throw new IllegalArgumentException("Refresh token expired");
    if (current.revokedAt != null) throw new IllegalArgumentException("Refresh token revoked");
    if (current.replacedBy != null) throw new IllegalArgumentException("Refresh token already rotated");

    UUID tenantId = requireTenantId(tenantKey);
    String u = requireUserInTenant(tenantId, sub.username);
    List<String> roles = loadRolesBestEffort(tenantId, u);

    UUID familyId = jdbc.queryForObject(
        "select family_id from refresh_token where id = ?",
        (rs, rowNum) -> (UUID) rs.getObject(1),
        current.id
    );

    UUID newId = UUID.randomUUID();
    Instant newExpiresAt = now.plus(refreshTtl);

    String newToken = newToken();
    String newTokenHash = sha256B64Url(newToken);

    revoke(current.id, now, newId);

    insertRefreshToken(
        newId,
        buildSubject(tenantKey, u),
        newTokenHash,
        newExpiresAt,
        tenantId,
        u,
        familyId
    );

    return new TokenPair(u, roles, newToken);
  }

  private UUID requireTenantId(String tenantKey) {
    try {
      return jdbc.queryForObject(
          "select id from iam_tenant where tenant_key = ?",
          (rs, rowNum) -> (UUID) rs.getObject(1),
          tenantKey
      );
    } catch (EmptyResultDataAccessException e) {
      throw new IllegalArgumentException("Unknown tenant: " + tenantKey);
    }
  }

  private String requireUserInTenant(UUID tenantId, String username) {
    try {
      return jdbc.queryForObject(
          "select username from iam_user where tenant_id = ? and username = ?",
          (rs, rowNum) -> rs.getString(1),
          tenantId, username
      );
    } catch (EmptyResultDataAccessException e) {
      throw new IllegalArgumentException("Unknown user in tenant");
    }
  }

  private List<String> loadRolesBestEffort(UUID tenantId, String username) {
    try {
      return jdbc.queryForList(
          """
          select r.name
            from iam_role r
            join iam_user_role ur
              on ur.role_id = r.id
             and ur.tenant_id = r.tenant_id
           where ur.tenant_id = ?
             and ur.username = ?
           order by r.name
          """,
          String.class,
          tenantId, username
      );
    } catch (Exception e) {
      return List.of();
    }
  }

  private RefreshRow findLatestByHashForUpdate(String tokenHash) {
    List<RefreshRow> rows = jdbc.query(
        """
        select id, subject, expires_at, revoked_at, replaced_by
          from refresh_token
         where token_hash = ?
         for update
        """,
        (rs, rowNum) -> new RefreshRow(
            (UUID) rs.getObject("id"),
            rs.getString("subject"),
            rs.getTimestamp("expires_at") == null ? null : rs.getTimestamp("expires_at").toInstant(),
            rs.getTimestamp("revoked_at") == null ? null : rs.getTimestamp("revoked_at").toInstant(),
            (UUID) rs.getObject("replaced_by")
        ),
        tokenHash
    );
    return rows.isEmpty() ? null : rows.get(0);
  }

  private void revoke(UUID id, Instant now, UUID replacedBy) {
    jdbc.update(
        """
        update refresh_token
           set revoked_at = ?,
               replaced_by = ?,
               revoke_reason = 'ROTATED'
         where id = ?
        """,
        Timestamp.from(now),
        replacedBy,
        id
    );
  }

  private void insertRefreshToken(
      UUID id,
      String subject,
      String tokenHash,
      Instant expiresAt,
      UUID tenantId,
      String username,
      UUID familyId
  ) {
    jdbc.update(con -> {
      PreparedStatement ps = con.prepareStatement(
          """
          insert into refresh_token (id, subject, token_hash, expires_at, tenant_id, username, family_id)
          values (?, ?, ?, ?, ?, ?, ?)
          """
      );
      ps.setObject(1, id);
      ps.setString(2, subject);
      ps.setString(3, tokenHash);
      ps.setTimestamp(4, Timestamp.from(expiresAt));
      ps.setObject(5, tenantId);
      ps.setString(6, username);
      ps.setObject(7, familyId);
      return ps;
    });
  }

  private String buildSubject(String tenantKey, String username) {
    return tenantKey + ":" + username;
  }

  private ParsedSubject parseSubject(String subject) {
    if (subject == null) throw new IllegalArgumentException("Invalid subject");
    int idx = subject.indexOf(':');
    if (idx <= 0 || idx == subject.length() - 1) throw new IllegalArgumentException("Invalid subject");
    return new ParsedSubject(subject.substring(0, idx), subject.substring(idx + 1));
  }

  private String newToken() {
    byte[] b = new byte[32];
    rng.nextBytes(b);
    return B64_URL.encodeToString(b);
  }

  private String sha256B64Url(String token) {
    try {
      var md = java.security.MessageDigest.getInstance("SHA-256");
      byte[] digest = md.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      return B64_URL.encodeToString(digest);
    } catch (Exception e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }
}

