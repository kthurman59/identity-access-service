package com.kevdev.iam.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public org.springframework.http.ResponseEntity<ApiError> handleNoResourceFound(
      org.springframework.web.servlet.resource.NoResourceFoundException ex,
      jakarta.servlet.http.HttpServletRequest request
) {
      ApiError body = new ApiError(
          org.springframework.http.HttpStatus.NOT_FOUND.value(),
          org.springframework.http.HttpStatus.NOT_FOUND.getReasonPhrase(),
          ex.getMessage(),
          request.getRequestURI()
      );
      return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).body(body);
}

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ApiError apiError = baseError(status, "Validation failed", request);

        ex.getConstraintViolations().forEach(cv ->
                apiError.addFieldError(
                        cv.getPropertyPath().toString(),
                        cv.getMessage()
                )
        );

        return new ResponseEntity<>(apiError, status);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        String rootMessage = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();

        ApiError apiError = baseError(status, rootMessage, request);
        return new ResponseEntity<>(apiError, status);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBadRequest(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ApiError apiError = baseError(status, ex.getMessage(), request);
        return new ResponseEntity<>(apiError, status);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(
            Exception ex,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        String message = ex.getMessage() != null ? ex.getMessage() : "Unexpected error";
        ApiError apiError = baseError(status, message, request);

        return new ResponseEntity<>(apiError, status);
    }

    private ApiError baseError(HttpStatus status, String message, HttpServletRequest request) {
        String path = request.getRequestURI();
        return new ApiError(
                status.value(),
                status.getReasonPhrase(),
                message,
                path
        );
    }
}
