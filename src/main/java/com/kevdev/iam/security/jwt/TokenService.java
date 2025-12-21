package com.kevdev.iam.security.jwt;

import java.util.List;
import java.util.Map;

public interface TokenService {

    String issueAccessToken(String subject, List<String> roles, Map<String, Object> extraClaims);

    String issueRefreshToken();

    DecodedToken decodeAndValidateAccessToken(String token);

    record DecodedToken(String subject, List<String> roles, Map<String, Object> claims) {
    }
}

