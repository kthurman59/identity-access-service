package com.kevdev.iam.web;

import com.kevdev.iam.security.RefreshTokenService;
import com.kevdev.iam.security.TokenService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@Validated
public class AuthController {

  private final AuthenticationManager authManager;
  private final TokenService tokenService;
  private final RefreshTokenService refreshTokenService;

  public AuthController(AuthenticationManager authManager,
                        TokenService tokenService,
                        RefreshTokenService refreshTokenService) {
    this.authManager = authManager;
    this.tokenService = tokenService;
    this.refreshTokenService = refreshTokenService;
  }

  public record LoginRequest(String username, String password) {}
  public record RefreshRequest(String refreshToken) {}
  public record TokenResponse(String accessToken, String refreshToken) {}

  @PostMapping("/login")
  public ResponseEntity<TokenResponse> login(
      @RequestHeader("X-Tenant-Key") String tenantKey,
      @Valid @RequestBody LoginRequest req
  ) {
    Authentication auth = authManager.authenticate(
        new UsernamePasswordAuthenticationToken(req.username(), req.password())
    );

    String username = req.username();
    List<String> roles = auth.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .toList();

    var pair = refreshTokenService.mintOnLogin(tenantKey, username);
    String refresh = pair.refreshToken();

    String subject = tenantKey + ":" + username;
    String access = tokenService.issueAccessToken(subject, roles, Map.of());

    return ResponseEntity.ok(new TokenResponse(access, refresh));
  }

  @PostMapping("/refresh")
  public ResponseEntity<TokenResponse> refresh(
      @RequestHeader("X-Tenant-Key") String tenantKey,
      @Valid @RequestBody RefreshRequest req
  ) {
    var pair = refreshTokenService.rotate(tenantKey, req.refreshToken());
    String subject = tenantKey + ":" + pair.username();
    String access = tokenService.issueAccessToken(subject, pair.roles(), Map.of());
    return ResponseEntity.ok(new TokenResponse(access, pair.refreshToken()));
  }
}

