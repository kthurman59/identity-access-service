package com.kevdev.iam.web;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

record LoginRequest(String username, String password) {}
record LoginResponse(String accessToken) {}

@RestController
@RequestMapping("/auth")
public class AuthController {
  private final AuthenticationManager authManager;
  public AuthController(AuthenticationManager authManager) {
    this.authManager = authManager;
  }
  @PostMapping("/login")
  public LoginResponse login(@RequestBody LoginRequest req) {
    Authentication auth = authManager.authenticate(
        new UsernamePasswordAuthenticationToken(req.username(), req.password()));
    return new LoginResponse("dummy-token");
  }
}

