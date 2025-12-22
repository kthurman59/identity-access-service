package com.kevdev.iam.service;

import com.kevdev.iam.security.jwt.SecurityProperties;
import com.kevdev.iam.security.jwt.TokenService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AuthService {

  private final TokenService tokenService;
  private final RefreshTokenService refreshTokenService;
  private final SecurityProperties securityProperties;

  public AuthService(TokenService tokenService,
                     RefreshTokenService refreshTokenService,
                     SecurityProperties securityProperties) {
    this.tokenService = tokenService;
    this.refreshTokenService = refreshTokenService;
    this.securityProperties = securityProperties;
  }

  public Result login(String username, String password) {
    if (!isValid(username, password)) {
      throw new BadCredentialsException("Invalid credentials");
    }

    List<String> roles = List.of("ROLE_USER");
    Map<String, Object> claims = Map.of("permissions", List.of("iam.user.read"));

    String access = tokenService.issueAccessToken(username, roles, claims);
    String refresh = refreshTokenService.issue(username, securityProperties.refreshTokenTtlSeconds());

    return new Result(access, refresh);
  }

  public RefreshResult refresh(String rawRefreshToken) {
    var rotated = refreshTokenService.rotate(rawRefreshToken, securityProperties.refreshTokenTtlSeconds());
    String subject = rotated.subject();

    List<String> roles = List.of("ROLE_USER");
    Map<String, Object> claims = Map.of("permissions", List.of("iam.user.read"));

    String access = tokenService.issueAccessToken(subject, roles, claims);
    return new RefreshResult(access, rotated.newRefreshToken());
  }

  private boolean isValid(String username, String password) {
    return "user1".equals(username) && "password1".equals(password);
  }

  public record Result(String accessToken, String refreshToken) {}
  public record RefreshResult(String accessToken, String refreshToken) {}
}

