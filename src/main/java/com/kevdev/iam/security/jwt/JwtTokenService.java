package com.kevdev.iam.security.jwt;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JwtTokenService implements TokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final SecurityProperties props;
    private final SecureRandom secureRandom = new SecureRandom();

    public JwtTokenService(JwtEncoder jwtEncoder, JwtDecoder jwtDecoder, SecurityProperties props) {
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.props = props;
    }

    @Override
    public String issueAccessToken(String subject, List<String> roles, Map<String, Object> extraClaims) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(props.accessTokenTtlSeconds());

        Map<String, Object> claims = new HashMap<>();
        if (extraClaims != null) {
            claims.putAll(extraClaims);
        }
        claims.put("roles", roles == null ? List.of() : roles);

        JwtClaimsSet claimSet = JwtClaimsSet.builder()
                .issuer(props.issuer())
                .issuedAt(now)
                .expiresAt(exp)
                .subject(subject)
                .claims(c -> c.putAll(claims))
                .build();

        JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();

        return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claimSet)).getTokenValue();
    }

    @Override
    public String issueRefreshToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public DecodedToken decodeAndValidateAccessToken(String token) {
        Jwt jwt = jwtDecoder.decode(token);

        Object rolesObj = jwt.getClaims().get("roles");
        List<String> roles = (rolesObj instanceof List<?> list)
                ? list.stream().map(String::valueOf).toList()
                : List.of();

        return new DecodedToken(jwt.getSubject(), roles, Map.copyOf(jwt.getClaims()));
    }
}

