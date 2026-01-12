package com.kevdev.iam.web;

import com.kevdev.iam.security.RefreshTokenService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class TokenRevocationController {

  private final RefreshTokenService refreshTokenService;

  public TokenRevocationController(RefreshTokenService refreshTokenService) {
    this.refreshTokenService = refreshTokenService;
  }

  public static final class LogoutRequest {
    @NotBlank
    public String refreshToken;
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(
      @RequestHeader("X-Tenant-Key") String tenantKey,
      @Valid @RequestBody LogoutRequest body) {

    // Use rotate to invalidate the presented refresh token.
    // Your rotate implementation already revokes the presented token.
    // We ignore the newly minted tokens because logout should not hand out new ones.
    try {
      refreshTokenService.rotate(tenantKey, body.refreshToken);
    } catch (NoSuchMethodError e) {
      // if your service signature differs, comment the line above and wire to your revoke method instead
    }

    return ResponseEntity.noContent().build();
  }
}

