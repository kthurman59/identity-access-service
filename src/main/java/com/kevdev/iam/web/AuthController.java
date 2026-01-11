package com.kevdev.iam.web;

import com.kevdev.iam.security.RefreshTokenService;
import com.kevdev.iam.security.TokenService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class AuthController {

  private final AuthenticationManager authManager;
  private final TokenService tokenService;
  private final RefreshTokenService refreshTokenService;

  public AuthController(
      AuthenticationManager authManager,
      TokenService tokenService,
      RefreshTokenService refreshTokenService
  ) {
    this.authManager = authManager;
    this.tokenService = tokenService;
    this.refreshTokenService = refreshTokenService;
  }

  public record LoginRequest(
      @NotBlank String username,
      @NotBlank String password
  ) {}

  public record RefreshRequest(
      @NotBlank String refreshToken
  ) {}

  public record TokenResponse(
      String accessToken,
      String refreshToken
  ) {}

  @PostMapping("/auth/login")
  public ResponseEntity<TokenResponse> login(
      @RequestHeader("X-Tenant-Key") String tenantKey,
      @Valid @RequestBody LoginRequest req
  ) {
    var auth = authManager.authenticate(
        new UsernamePasswordAuthenticationToken(req.username(), req.password())
    );
    String username = auth.getName();

    // mint refresh and load roles
    RefreshTokenService.TokenPair pair = refreshTokenService.mintOnLogin(tenantKey, username);

    // subject format must match RefreshTokenService buildSubject
    String subject = tenantKey + ":" + pair.username();

    String access = tokenService.issueAccessToken(
        subject,
        pair.roles(),
        Map.of("tenant", tenantKey)
    );

    return ResponseEntity.ok(new TokenResponse(access, pair.refreshToken()));
  }

  @PostMapping("/auth/refresh")
  public ResponseEntity<TokenResponse> refresh(
      @RequestHeader("X-Tenant-Key") String tenantKey,
      @Valid @RequestBody RefreshRequest req
  ) {
    RefreshTokenService.TokenPair pair = refreshTokenService.rotate(tenantKey, req.refreshToken());
    String subject = tenantKey + ":" + pair.username();

    String access = tokenService.issueAccessToken(
        subject,
        pair.roles(),
        Map.of("tenant", tenantKey)
    );

    return ResponseEntity.ok(new TokenResponse(access, pair.refreshToken()));
  }
}

