// src/main/java/com/kevdev/iam/web/AuthController.java
package com.kevdev.iam.web;

import com.kevdev.iam.security.RefreshTokenService;
import com.kevdev.iam.security.RefreshTokenService.TokenPair;
import com.kevdev.iam.web.dto.AuthResponse;
import com.kevdev.iam.web.dto.LoginRequest;
import com.kevdev.iam.web.dto.RefreshRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;

    public AuthController(AuthenticationManager authenticationManager,
                          RefreshTokenService refreshTokenService) {
        this.authenticationManager = authenticationManager;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/auth/login")
    public AuthResponse login(
            @RequestHeader("X-Tenant-Key") @NotBlank String tenantKey,
            @RequestBody @Valid LoginRequest request
    ) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        UserDetails user = (UserDetails) auth.getPrincipal();
        String subject = tenantKey + ":" + user.getUsername();

        TokenPair pair = refreshTokenService.mintOnLogin(user, subject);
        return new AuthResponse(pair.accessToken(), pair.refreshToken());
    }

    @PostMapping("/auth/refresh")
    public AuthResponse refresh(
            @RequestHeader("X-Tenant-Key") @NotBlank String tenantKey,
            @RequestBody @Valid RefreshRequest request
    ) {
        TokenPair pair = refreshTokenService.rotate(tenantKey, request.refreshToken());
        return new AuthResponse(pair.accessToken(), pair.refreshToken());
    }
}

