package com.kevdev.iam.web;

import com.kevdev.iam.security.RefreshTokenService;
import com.kevdev.iam.security.TokenService;
import com.kevdev.iam.web.dto.AuthResponse;
import com.kevdev.iam.web.dto.LoginRequest;
import com.kevdev.iam.web.dto.RefreshRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
public class AuthController {

  private final UserDetailsService userDetailsService;
  private final PasswordEncoder passwordEncoder;
  private final RefreshTokenService refreshTokenService;
  @SuppressWarnings("unused")
  private final TokenService tokenService;

  public AuthController(
      UserDetailsService userDetailsService,
      PasswordEncoder passwordEncoder,
      RefreshTokenService refreshTokenService,
      TokenService tokenService
  ) {
    this.userDetailsService = userDetailsService;
    this.passwordEncoder = passwordEncoder;
    this.refreshTokenService = refreshTokenService;
    this.tokenService = tokenService;
  }

  @PostMapping("/login")
  public AuthResponse login(
      @RequestHeader("X-Tenant-Key") String tenantKey,
      @Valid @RequestBody LoginRequest req
  ) {
    UserDetails user = userDetailsService.loadUserByUsername(req.username());
    if (!passwordEncoder.matches(req.password(), user.getPassword())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "bad credentials");
    }
    var pair = refreshTokenService.mintOnLogin(user, tenantKey);
    return new AuthResponse(pair.accessToken(), pair.refreshToken());
  }

  @PostMapping("/refresh")
  public AuthResponse refresh(
      @RequestHeader("X-Tenant-Key") String tenantKey,
      @Valid @RequestBody RefreshRequest req
  ) {
    var pair = refreshTokenService.rotate(tenantKey, req.refreshToken());
    return new AuthResponse(pair.accessToken(), pair.refreshToken());
  }
}

