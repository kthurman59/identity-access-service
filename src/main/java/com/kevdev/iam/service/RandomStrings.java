package com.kevdev.iam.service;

import java.security.SecureRandom;

public final class RandomStrings {
  private static final SecureRandom RNG = new SecureRandom();
  private static final char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();

  private RandomStrings() {}

  public static String secure(int len) {
    char[] out = new char[len];
    for (int i = 0; i < len; i++) {
      out[i] = ALPHABET[RNG.nextInt(ALPHABET.length)];
    }
    return new String(out);
  }
}

