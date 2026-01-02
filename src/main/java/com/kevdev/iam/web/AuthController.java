package com.kevdev.iam.web;

import com.kevdev.iam.domain.Tenant;
import com.kevdev.iam.repo.TenantRepository;
import com.kevdev.iam.security.JwtTokenService;
import com.kevdev.iam.security.RefreshTokenService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/auth", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthController {

    private static final String TENANT_HEADER = "X-Tenant-Key";

    private final AuthenticationManager authenticationManager;
    private final TenantRepository tenantRepository;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(
        AuthenticationManager authenticationManager,
        TenantRepository tenantRepository,
        JwtTokenService jwtTokenService,
        RefreshTokenService refreshTokenService
    ) {
        this.authenticationManager = authenticationManager;
        this.tenantRepository = tenantRepository;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AuthResult login(
        @RequestHeader(TENANT_HEADER) String tenantKey,
        @Valid @RequestBody LoginRequest req
    ) {
        Tenant tenant = tenantRepository.findByKey(tenantKey)
            .orElseThrow(() -> new IllegalArgumentException("unknown_tenant"));

        Authentication auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(req.username(), req.password())
        );

        UserDetails principal = (UserDetails) auth.getPrincipal();
        String accessToken = jwtTokenService.issueAccessToken(principal);
        String refreshToken = refreshTokenService.mintOnLogin(tenant.getId(), req.username());

        return new AuthResult(accessToken, refreshToken);
    }

    @PostMapping(path = "/refresh", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AuthResult refresh(
        @RequestHeader(TENANT_HEADER) String tenantKey,
        @Valid @RequestBody RefreshRequest req
    ) {
        Tenant tenant = tenantRepository.findByKey(tenantKey)
            .orElseThrow(() -> new IllegalArgumentException("unknown_tenant"));

        RefreshTokenService.TokenPair pair = refreshTokenService.rotate(tenant.getId(), req.refreshToken());
        return new AuthResult(pair.accessToken(), pair.refreshToken());
    }

    public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password
    ) {}

    public record RefreshRequest(
        @NotBlank String refreshToken
    ) {}

    public record AuthResult(
        String accessToken,
        String refreshToken
    ) {}
}

