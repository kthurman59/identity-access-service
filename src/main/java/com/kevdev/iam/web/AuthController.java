package com.kevdev.iam.web;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class AuthController {

  private final AuthenticationManager authManager;
  private final JwtEncoder jwtEncoder;
  private final com.kevdev.iam.security.RefreshTokenService refreshTokenService;

  @Value("${ias.jwt.issuer:ias-dev}")
  private String issuer;

  public AuthController(AuthenticationManager authManager,
                        JwtEncoder jwtEncoder,
                        com.kevdev.iam.security.RefreshTokenService refreshTokenService) {
    this.authManager = authManager;
    this.jwtEncoder = jwtEncoder;
    this.refreshTokenService = refreshTokenService;
  }

  public record LoginRequest(String username, String password) {}
  public record TokenResponse(String accessToken, String refreshToken) {}

  @PostMapping("/auth/login")
  public ResponseEntity<TokenResponse> login(@RequestHeader("X-Tenant-Key") String tenantKey,
                                             @Valid @RequestBody LoginRequest req) {
    Authentication auth = authManager.authenticate(
        new UsernamePasswordAuthenticationToken(req.username(), req.password()));

    List<String> roles = auth.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
        .toList();

    Instant now = Instant.now();
    Instant exp = now.plusSeconds(60L * 60L);

    JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer(issuer)
        .issuedAt(now)
        .expiresAt(exp)
        .subject(req.username())
        .claim("username", req.username())
        .claim("tenant", tenantKey)
        .claim("roles", roles)
        .build();

    String access = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

    UUID pseudoUserId = UUID.nameUUIDFromBytes((tenantKey + ":" + req.username()).getBytes());
    String refresh = refreshTokenService.mintOnLogin(pseudoUserId, req.username());

    return ResponseEntity.ok(new TokenResponse(access, refresh));
  }

  @PostMapping("/auth/refresh")
  public ResponseEntity<TokenResponse> refresh(@RequestHeader("X-Tenant-Key") String tenantKey,
                                               @RequestBody Map<String, String> body) {
    String presented = body.getOrDefault("refreshToken", "");
    UUID pseudoUserId = UUID.nameUUIDFromBytes(("u:" + presented).getBytes());
    com.kevdev.iam.security.RefreshTokenService.TokenPair pair =
        refreshTokenService.rotate(pseudoUserId, presented);

    Instant now = Instant.now();
    Instant exp = now.plusSeconds(60L * 60L);

    JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer(issuer)
        .issuedAt(now)
        .expiresAt(exp)
        .subject(pair.username())
        .claim("username", pair.username())
        .claim("tenant", tenantKey)
        .claim("roles", List.of("ADMIN"))
        .build();

    String access = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    return ResponseEntity.ok(new TokenResponse(access, pair.refreshToken()));
  }
}

