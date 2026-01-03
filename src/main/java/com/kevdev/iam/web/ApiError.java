package com.kevdev.iam.web;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ApiError {
  private final Instant timestamp;
  private final String requestId;
  private final int status;
  private final String error;
  private final String message;
  private final String path;
  private final List<FieldError> fieldErrors;

  public ApiError(Instant timestamp,
                  String requestId,
                  int status,
                  String error,
                  String message,
                  String path,
                  List<FieldError> fieldErrors) {
    this.timestamp = timestamp == null ? Instant.now() : timestamp;
    this.requestId = requestId == null ? "" : requestId;
    this.status = status;
    this.error = error == null ? "" : error;
    this.message = message == null ? "" : message;
    this.path = path == null ? "" : path;
    this.fieldErrors = fieldErrors == null
        ? List.of()
        : Collections.unmodifiableList(new ArrayList<>(fieldErrors));
  }

  public Instant getTimestamp() { return timestamp; }
  public String getRequestId() { return requestId; }
  public int getStatus() { return status; }
  public String getError() { return error; }
  public String getMessage() { return message; }
  public String getPath() { return path; }
  public List<FieldError> getFieldErrors() { return fieldErrors; }

  public static ApiError of(int status,
                            String error,
                            String message,
                            String path,
                            String requestId,
                            List<?> fieldErrors) {
    @SuppressWarnings("unchecked")
    List<FieldError> fe = fieldErrors == null ? List.of() : (List<FieldError>) fieldErrors;
    return new ApiError(Instant.now(), requestId, status, error, message, path, fe);
  }

  public static final class FieldError {
    private final String field;
    private final String code;
    private final String message;

    public FieldError(String field, String message) {
      this(field, "", message);
    }

    public FieldError(String field, String code, String message) {
      this.field = field == null ? "" : field;
      this.code = code == null ? "" : code;
      this.message = message == null ? "" : message;
    }

    public String getField() { return field; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
  }
}

