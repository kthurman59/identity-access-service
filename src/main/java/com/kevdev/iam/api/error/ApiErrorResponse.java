package com.kevdev.iam.api.error;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
    Instant timestamp,
    String requestId,
    int status,
    String error,
    String message,
    String path,
    List<FieldError> fieldErrors
) {

  public record FieldError(
      String field,
      String code,
      String message
  ) {}
}

