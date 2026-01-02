package com.kevdev.iam.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex,
                                               HttpServletRequest req,
                                               HttpServletResponse res) {

        List<ApiError.FieldError> fieldErrors = new ArrayList<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.add(new ApiError.FieldError(fe.getField(), fe.getDefaultMessage()));
        }

        ApiError body = ApiError.of(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            "Validation failed",
            req.getRequestURI(),
            resolveRequestId(req, res),
            fieldErrors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler({AuthorizationDeniedException.class, AccessDeniedException.class})
    public ResponseEntity<ApiError> forbidden(Exception ex,
                                              HttpServletRequest req,
                                              HttpServletResponse res) {

        ApiError body = ApiError.of(
            HttpStatus.FORBIDDEN.value(),
            "Forbidden",
            "Forbidden",
            req.getRequestURI(),
            resolveRequestId(req, res),
            List.of()
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> generic(Exception ex,
                                            HttpServletRequest req,
                                            HttpServletResponse res) {

        ApiError body = ApiError.of(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            "Internal Server Error",
            req.getRequestURI(),
            resolveRequestId(req, res),
            List.of()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private String resolveRequestId(HttpServletRequest request, HttpServletResponse response) {
        String v = response.getHeader("XRequestId");
        if (v != null && !v.isBlank()) return v;

        v = request.getHeader("XRequestId");
        if (v != null && !v.isBlank()) return v;

        Object attr = request.getAttribute("requestId");
        if (attr != null && !attr.toString().isBlank()) return attr.toString();

        attr = request.getAttribute("XRequestId");
        if (attr != null && !attr.toString().isBlank()) return attr.toString();

        return null;
    }
}

