// src/main/java/com/kevdev/iam/web/dto/AuthResponse.java
package com.kevdev.iam.web.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken
) {}

