package com.kevdev.iam.security;

import java.util.List;
import java.util.Map;

public interface TokenService {

  String issueAccessToken(String subject, List<String> roles, Map<String, Object> extraClaims);

  DecodedAccessToken decodeAndValidateAccessToken(String token);

  record DecodedAccessToken(
      String subject,
      List<String> roles,
      Map<String, Object> claims
  ) {}
}

