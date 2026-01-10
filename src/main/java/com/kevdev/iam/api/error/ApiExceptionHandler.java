package com.kevdev.iam.api.error;

import com.kevdev.iam.web.web.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class ApiExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

  private final Clock clock;

  public ApiExceptionHandler(Clock clock) {
    this.clock = clock;
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ApiErrorResponse> handleResponseStatus(ResponseStatusException ex, HttpServletRequest req) {
    int status = ex.getStatusCode().value();
    String error = reasonPhrase(status);
    String message = (ex.getReason() == null || ex.getReason().isBlank()) ? error : ex.getReason();

    return ResponseEntity.status(status).body(
        new ApiErrorResponse(
            Instant.now(clock),
            requestId(req),
            status,
            error,
            message,
            req.getRequestURI(),
            List.of()
        )
    );
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
    int status = HttpStatus.BAD_REQUEST.value();
    String error = reasonPhrase(status);

    List<ApiErrorResponse.FieldError> fields = new ArrayList<>();
    for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
      fields.add(new ApiErrorResponse.FieldError(
          fe.getField(),
          safeString(fe.getCode()),
          safeString(fe.getDefaultMessage())
      ));
    }
    for (ObjectError oe : ex.getBindingResult().getGlobalErrors()) {
      fields.add(new ApiErrorResponse.FieldError(
          safeString(oe.getObjectName()),
          safeString(oe.getCode()),
          safeString(oe.getDefaultMessage())
      ));
    }

    return ResponseEntity.status(status).body(
        new ApiErrorResponse(
            Instant.now(clock),
            requestId(req),
            status,
            error,
            "Validation failed",
            req.getRequestURI(),
            fields
        )
    );
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
    int status = HttpStatus.BAD_REQUEST.value();
    String error = reasonPhrase(status);

    List<ApiErrorResponse.FieldError> fields = new ArrayList<>();
    ex.getConstraintViolations().forEach(v -> fields.add(
        new ApiErrorResponse.FieldError(
            v.getPropertyPath() == null ? "" : v.getPropertyPath().toString(),
            safeString(v.getConstraintDescriptor() == null ? null : v.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName()),
            safeString(v.getMessage())
        )
    ));

    return ResponseEntity.status(status).body(
        new ApiErrorResponse(
            Instant.now(clock),
            requestId(req),
            status,
            error,
            "Validation failed",
            req.getRequestURI(),
            fields
        )
    );
  }

  @ExceptionHandler(ErrorResponseException.class)
  public ResponseEntity<ApiErrorResponse> handleErrorResponse(ErrorResponseException ex, HttpServletRequest req) {
    int status = ex.getStatusCode().value();
    String error = reasonPhrase(status);
    String message = safeString(ex.getBody() == null ? null : ex.getBody().getDetail());
    if (message.isBlank()) message = error;

    return ResponseEntity.status(status).body(
        new ApiErrorResponse(
            Instant.now(clock),
            requestId(req),
            status,
            error,
            message,
            req.getRequestURI(),
            List.of()
        )
    );
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) {
    String rid = requestId(req);

    log.error(
        "Unhandled exception method={} path={} XRequestId={}",
        req.getMethod(),
        req.getRequestURI(),
        rid,
        ex
    );

    int status = HttpStatus.INTERNAL_SERVER_ERROR.value();
    String error = reasonPhrase(status);

    return ResponseEntity.status(status).body(
        new ApiErrorResponse(
            Instant.now(clock),
            rid,
            status,
            error,
            "Unexpected error",
            req.getRequestURI(),
            List.of()
        )
    );
  }

  private String requestId(HttpServletRequest req) {
    Object attr = req.getAttribute(RequestIdFilter.REQUEST_ID_KEY);
    if (attr != null) return attr.toString();

    String header = req.getHeader(RequestIdFilter.REQUEST_ID_HEADER);
    return header == null ? "" : header;
  }

  private static String reasonPhrase(int status) {
    try {
      return HttpStatus.valueOf(status).getReasonPhrase();
    } catch (Exception ignored) {
      return "HTTP " + status;
    }
  }

  private static String safeString(String s) {
    return s == null ? "" : s;
  }
}

