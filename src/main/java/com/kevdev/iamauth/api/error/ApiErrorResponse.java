package com.kevdev.iamauth.api.error;

import java.time.Instant;

public record ApiErrorResponse(
        Instant timestamp,
        String requestId,
        int status,
        String error,
        String message,
        String path
) {
}

