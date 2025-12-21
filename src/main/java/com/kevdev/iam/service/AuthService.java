package com.kevdev.iam.service;

import com.kevdev.iam.security.jwt.TokenService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AuthService {

  private final TokenService tokenService;

  public AuthService(TokenService tokenService) {
    this.tokenService = tokenService;
  }

  public Result login(String username, String password) {
    if (!isValid(username, password)) {
      throw new BadCredentialsException("Invalid credentials");
    }

    List<String> roles = List.of("ROLE_USER");
    List<String> permissions = List.of("iam.user.read");

    String accessToken = tokenService.issueAccessToken(
        username,
        roles,
        Map.of("permissions", permissions)
    );

    String refreshToken = tokenService.issueRefreshToken();

    return new Result(accessToken, refreshToken);
  }

  private boolean isValid(String username, String password) {
    return "user1".equals(username) && "password1".equals(password);
  }

  public record Result(String accessToken, String refreshToken) {}
}

