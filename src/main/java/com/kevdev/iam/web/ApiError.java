package com.kevdev.iam.web;

import java.time.Instant;
import java.util.List;

public record ApiError(
    Instant timestamp,
    int status,
    String error,
    String message,
    String path,
    String method,
    List<FieldError> fieldErrors
) {

  public record FieldError(String field, String code, String message) { }

  public static ApiError of(
      Instant timestamp,
      int status,
      String error,
      String message,
      String path,
      String method,
      List<FieldError> fieldErrors
  ) {
    List<FieldError> safe = (fieldErrors == null) ? List.of() : List.copyOf(fieldErrors);
    return new ApiError(timestamp, status, error, message, path, method, safe);
  }

  public static ApiError of(
      int status,
      String error,
      String message,
      String path,
      String method,
      List<FieldError> fieldErrors
  ) {
    return of(Instant.now(), status, error, message, path, method, fieldErrors);
  }
}

