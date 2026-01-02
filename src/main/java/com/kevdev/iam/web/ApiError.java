package com.kevdev.iam.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ApiError {

    private Instant timestamp;
    private String requestId;
    private int status;
    private String error;
    private String message;
    private String path;

    @JsonProperty("fieldErrors")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private List<FieldError> fieldErrors = new ArrayList<>();

    public ApiError() {
        this.fieldErrors = new ArrayList<>();
    }

    public ApiError(
            Instant timestamp,
            String requestId,
            int status,
            String error,
            String message,
            String path,
            List<FieldError> fieldErrors
    ) {
        this.timestamp = timestamp;
        this.requestId = requestId;
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.fieldErrors = (fieldErrors == null) ? new ArrayList<>() : new ArrayList<>(fieldErrors);
    }

    public static ApiError of(
            int status,
            String error,
            String message,
            String path,
            String requestId,
            List<FieldError> fieldErrors
    ) {
        return new ApiError(
                Instant.now(),
                requestId,
                status,
                error,
                message,
                path,
                fieldErrors
        );
    }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    @JsonProperty("fieldErrors")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public List<FieldError> getFieldErrors() {
        return fieldErrors;
    }

    @JsonProperty("fieldErrors")
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    public void setFieldErrors(List<FieldError> fieldErrors) {
        this.fieldErrors = (fieldErrors == null) ? new ArrayList<>() : new ArrayList<>(fieldErrors);
    }

    public static class FieldError {
        private String field;
        private String message;

        public FieldError() {}

        public FieldError(String field, String message) {
            this.field = field;
            this.message = message;
        }

        public String getField() { return field; }
        public void setField(String field) { this.field = field; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}

