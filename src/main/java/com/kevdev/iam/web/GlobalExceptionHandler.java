package com.kevdev.iam.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> onValidation(MethodArgumentNotValidException ex,
                                               @NonNull HttpServletRequest req) {
    List<ApiError.FieldError> fields = ex.getBindingResult()
        .getFieldErrors()
        .stream()
        .map(fe -> new ApiError.FieldError(
            fe.getField(),
            fe.getCode() == null ? "" : fe.getCode(),
            fe.getDefaultMessage() == null ? "" : fe.getDefaultMessage()))
        .toList();

    ApiError body = ApiError.of(
        HttpStatus.BAD_REQUEST.value(),
        "Bad Request",
        "Validation failed",
        req.getRequestURI(),
        requestId(req),
        fields
    );
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiError> onConstraintViolation(ConstraintViolationException ex,
                                                        @NonNull HttpServletRequest req) {
    List<ApiError.FieldError> fields = ex.getConstraintViolations()
        .stream()
        .map(ConstraintViolation::getPropertyPath)
        .map(path -> new ApiError.FieldError(path.toString(), "invalid"))
        .toList();

    ApiError body = ApiError.of(
        HttpStatus.BAD_REQUEST.value(),
        "Bad Request",
        "Constraint violation",
        req.getRequestURI(),
        requestId(req),
        fields
    );
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> onAny(Exception ex,
                                        @NonNull HttpServletRequest req) {
    ApiError body = ApiError.of(
        HttpStatus.INTERNAL_SERVER_ERROR.value(),
        "Internal Server Error",
        ex.getMessage() == null ? "Unexpected error" : ex.getMessage(),
        req.getRequestURI(),
        requestId(req),
        List.of()
    );
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
  }

  private static String requestId(HttpServletRequest req) {
    Object v = req.getAttribute("requestId");
    if (v != null && !v.toString().isBlank()) return v.toString();
    String h = req.getHeader("X-Request-Id");
    if (h != null && !h.isBlank()) return h;
    h = req.getHeader("X-Correlation-Id");
    return h == null ? "" : h;
  }
}

