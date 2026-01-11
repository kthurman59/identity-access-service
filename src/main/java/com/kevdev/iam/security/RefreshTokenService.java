// src/main/java/com/kevdev/iam/security/RefreshTokenService.java
package com.kevdev.iam.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RefreshTokenService {

    public record TokenPair(String accessToken, String refreshToken) {}

    private record TokenRow(UUID id, String subject, String tokenHash, Instant expiresAt, Instant revokedAt) {}

    private final TokenService tokenService;
    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;
    private final PasswordEncoder passwordEncoder;
    private final UserDetailsService userDetailsService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Duration refreshTtl;

    public RefreshTokenService(
            TokenService tokenService,
            JdbcTemplate jdbcTemplate,
            Clock clock,
            PasswordEncoder passwordEncoder,
            UserDetailsService userDetailsService,
            @Value("${security.refresh-token.ttl:P30D}") String refreshTtlProp
    ) {
        this.tokenService = tokenService;
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
        this.passwordEncoder = passwordEncoder;
        this.userDetailsService = userDetailsService;
        String raw = (refreshTtlProp == null || refreshTtlProp.isBlank()) ? "P30D" : refreshTtlProp.trim();
        this.refreshTtl = DurationStyle.detectAndParse(raw);
    }

    @Transactional
    public TokenPair mintOnLogin(UserDetails user, String subject) {
        Instant now = Instant.now(clock);
        Instant exp = now.plus(refreshTtl);

        String refreshRaw = generateToken();
        insertRefreshToken(subject, refreshRaw, now, exp);

        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        String access = tokenService.issueAccessToken(subject, roles, Map.of());
        return new TokenPair(access, refreshRaw);
    }

    @Transactional
    public TokenPair rotate(String tenantKey, String refreshRaw) {
        TokenRow match = findMatchingRowForTenant(tenantKey, refreshRaw);
        if (match == null) {
            throw new IllegalArgumentException("invalid refresh token");
        }
        if (match.expiresAt().isBefore(Instant.now(clock))) {
            throw new IllegalStateException("expired refresh token");
        }
        if (match.revokedAt() != null) {
            throw new IllegalStateException("revoked refresh token");
        }

        jdbcTemplate.update(
                "update refresh_token set revoked_at = ? where id = ?",
                Timestamp.from(Instant.now(clock)), match.id()
        );

        String subject = match.subject();
        String username = subject.substring(subject.indexOf(':') + 1);
        UserDetails user = userDetailsService.loadUserByUsername(username);

        Instant now = Instant.now(clock);
        Instant exp = now.plus(refreshTtl);
        String newRaw = generateToken();
        insertRefreshToken(subject, newRaw, now, exp);

        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        String access = tokenService.issueAccessToken(subject, roles, Map.of());
        return new TokenPair(access, newRaw);
    }

    private TokenRow findMatchingRowForTenant(String tenantKey, String refreshRaw) {
        String like = tenantKey + ":%";
        RowMapper<TokenRow> mapper = (ResultSet rs, int i) -> new TokenRow(
                (UUID) rs.getObject("id"),
                rs.getString("subject"),
                rs.getString("token_hash"),
                rs.getTimestamp("expires_at").toInstant(),
                rs.getTimestamp("revoked_at") == null ? null : rs.getTimestamp("revoked_at").toInstant()
        );

        List<TokenRow> rows = jdbcTemplate.query(
                "select id, subject, token_hash, expires_at, revoked_at " +
                        "from refresh_token where subject like ? order by issued_at desc limit 200",
                mapper, like
        );

        for (TokenRow row : rows) {
            if (passwordEncoder.matches(refreshRaw, row.tokenHash())) {
                return row;
            }
        }
        return null;
    }

    private void insertRefreshToken(String subject, String refreshRaw, Instant issuedAt, Instant expiresAt) {
        jdbcTemplate.update(con -> {
            var ps = con.prepareStatement(
                    "insert into refresh_token (id, subject, token_hash, issued_at, expires_at) values (?, ?, ?, ?, ?)"
            );
            ps.setObject(1, UUID.randomUUID());
            ps.setString(2, subject);
            ps.setString(3, passwordEncoder.encode(refreshRaw));
            ps.setTimestamp(4, Timestamp.from(issuedAt));
            ps.setTimestamp(5, Timestamp.from(expiresAt));
            return ps;
        });
    }

    private String generateToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

