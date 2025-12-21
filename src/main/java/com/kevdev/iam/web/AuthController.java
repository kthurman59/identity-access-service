package com.kevdev.iam.web;

import com.kevdev.iam.dto.LoginRequest;
import com.kevdev.iam.dto.LoginResponse;
import com.kevdev.iam.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
    var result = authService.login(req.username(), req.password());
    return ResponseEntity.ok(new LoginResponse(result.accessToken(), result.refreshToken()));
  }
}

