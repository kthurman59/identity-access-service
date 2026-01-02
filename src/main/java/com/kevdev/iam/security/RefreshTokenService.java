package com.kevdev.iam.security;

import com.kevdev.iam.domain.RefreshToken;
import com.kevdev.iam.repo.RefreshTokenRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final Duration REFRESH_TTL = Duration.ofDays(30);

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecurityUserDetailsService userDetailsService;
    private final JwtTokenService jwtTokenService;

    private final SecureRandom secureRandom = new SecureRandom();
    private final Clock clock = Clock.systemUTC();

    public record TokenPair(String accessToken, String refreshToken) {}

    public RefreshTokenService(
        RefreshTokenRepository refreshTokenRepository,
        SecurityUserDetailsService userDetailsService,
        JwtTokenService jwtTokenService
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userDetailsService = userDetailsService;
        this.jwtTokenService = jwtTokenService;
    }

    @Transactional
    public String mintOnLogin(UUID tenantId, String username) {
        Instant now = Instant.now(clock);

        String raw = generateOpaque();
        String hash = sha256Hex(raw);

        RefreshToken rt = new RefreshToken();
        rt.setId(UUID.randomUUID());
        rt.setTenantId(tenantId);
        rt.setUsername(username);
        rt.setTokenHash(hash);
        rt.setFamilyId(UUID.randomUUID());
        rt.setCreatedAt(now);
        rt.setExpiresAt(now.plus(REFRESH_TTL));

        refreshTokenRepository.save(rt);
        return raw;
    }

    @Transactional
    public TokenPair rotate(UUID tenantId, String presentedRefreshToken) {
        Instant now = Instant.now(clock);
        String presentedHash = sha256Hex(presentedRefreshToken);

        RefreshToken current = refreshTokenRepository.findForUpdateByTokenHash(presentedHash)
            .orElseThrow(() -> unauthorized("invalid_refresh"));

        if (current.getTenantId() == null || !tenantId.equals(current.getTenantId())) {
            throw unauthorized("invalid_refresh");
        }

        if (current.isExpired(now)) {
            throw unauthorized("expired_refresh");
        }

        if (current.isRevoked()) {
            throw unauthorized("revoked_refresh");
        }

        String newRaw = generateOpaque();
        String newHash = sha256Hex(newRaw);

        current.setRevokedAt(now);
        current.setReplacedByHash(newHash);
        current.setRevokeReason("ROTATED");

        RefreshToken next = new RefreshToken();
        next.setId(UUID.randomUUID());
        next.setTenantId(current.getTenantId());
        next.setUsername(current.getUsername());
        next.setTokenHash(newHash);
        next.setFamilyId(current.getFamilyId());
        next.setCreatedAt(now);
        next.setExpiresAt(now.plus(REFRESH_TTL));

        refreshTokenRepository.save(current);
        refreshTokenRepository.save(next);

        UserDetails userDetails = userDetailsService.loadUserByUsername(current.getUsername());
        String access = jwtTokenService.issueAccessToken(userDetails);

        return new TokenPair(access, newRaw);
    }

    private String generateOpaque() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256Hex(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("sha256_failed", e);
        }
    }

    private ResponseStatusException unauthorized(String code) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, code);
    }
}

