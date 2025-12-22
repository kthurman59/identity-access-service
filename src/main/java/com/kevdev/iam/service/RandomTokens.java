package com.kevdev.iam.service;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public final class RandomTokens {
  private static final SecureRandom RNG = new SecureRandom();

  private RandomTokens() {}

  /** Returns a URL safe Base64 token without padding from numBytes of entropy */
  public static String base64Url(int numBytes) {
    byte[] bytes = new byte[numBytes];
    RNG.nextBytes(bytes);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    Arrays.fill(bytes, (byte) 0);
    return token;
  }
}

