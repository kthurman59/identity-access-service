package com.kevdev.iam.web;

import com.kevdev.iam.security.JwtTokenService;
import com.kevdev.iam.security.RefreshTokenService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class AuthController {

  private final AuthenticationManager authManager;
  private final JwtTokenService jwtTokenService;
  private final RefreshTokenService refreshTokenService;
  private final JdbcTemplate jdbc;

  public AuthController(
      AuthenticationManager authManager,
      JwtTokenService jwtTokenService,
      RefreshTokenService refreshTokenService,
      JdbcTemplate jdbc
  ) {
    this.authManager = authManager;
    this.jwtTokenService = jwtTokenService;
    this.refreshTokenService = refreshTokenService;
    this.jdbc = jdbc;
  }

  public record LoginRequest(String username, String password) {}
  public record TokenResponse(String accessToken, String refreshToken) {}

  @PostMapping("/auth/login")
  public ResponseEntity<TokenResponse> login(
      @RequestHeader("X-Tenant-Key") String tenantKey,
      @Valid @RequestBody LoginRequest req
  ) {
    Authentication auth = authManager.authenticate(
        new UsernamePasswordAuthenticationToken(req.username(), req.password()));

    List<String> roles = auth.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
        .map(r -> r.toUpperCase(Locale.ROOT))
        .distinct()
        .toList();

    String access = jwtTokenService.issueAccessToken(
        req.username(),
        roles,
        Map.of(
            "username", req.username(),
            "tenant", tenantKey
        )
    );

    UUID userId = lookupUserId(tenantKey, req.username());
    String refresh = refreshTokenService.mintOnLogin(userId, req.username());

    return ResponseEntity.ok(new TokenResponse(access, refresh));
  }

  @PostMapping("/auth/refresh")
  public ResponseEntity<TokenResponse> refresh(
      @RequestHeader("X-Tenant-Key") String tenantKey,
      @RequestBody Map<String, String> body
  ) {
    String presented = body.getOrDefault("refreshToken", "").trim();
    String username = body.getOrDefault("username", "").trim();

    if (presented.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "refreshToken is required");
    }
    if (username.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username is required");
    }

    UUID userId = lookupUserId(tenantKey, username);

    RefreshTokenService.TokenPair pair = refreshTokenService.rotate(userId, presented);

    List<String> roles = lookupRoles(tenantKey, pair.username());

    String access = jwtTokenService.issueAccessToken(
        pair.username(),
        roles,
        Map.of(
            "username", pair.username(),
            "tenant", tenantKey
        )
    );

    return ResponseEntity.ok(new TokenResponse(access, pair.refreshToken()));
  }

  private UUID lookupUserId(String tenantKey, String username) {
    try {
      return jdbc.queryForObject(
          """
          select u.id
          from iam_user u
          join iam_tenant t on t.id = u.tenant_id
          where t.tenant_key = ?
            and u.username = ?
          """,
          (rs, n) -> rs.getObject(1, UUID.class),
          tenantKey,
          username
      );
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user or tenant");
    }
  }

  private List<String> lookupRoles(String tenantKey, String username) {
    return jdbc.query(
        """
        select r.name
        from iam_role r
        join iam_user_role ur on ur.role_id = r.id
        join iam_tenant t on t.id = ur.tenant_id
        where t.tenant_key = ?
          and ur.username = ?
        order by r.name
        """,
        (rs, n) -> rs.getString(1).toUpperCase(Locale.ROOT),
        tenantKey,
        username
    );
  }
}

