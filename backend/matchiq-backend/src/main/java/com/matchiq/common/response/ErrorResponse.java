package com.matchiq.common.response;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ErrorResponse {
    private boolean success;
    private String message;
    private List<Object> errors;
    private LocalDateTime timestamp;

    public ErrorResponse() {
        this.success = false;
        this.errors = new ArrayList<>();
        this.timestamp = LocalDateTime.now();
    }

    public ErrorResponse(String message) {
        this();
        this.message = message;
    }

    public ErrorResponse(String message, List<Object> errors) {
        this();
        this.message = message;
        this.errors = errors;
    }

    public void addError(Object error) { this.errors.add(error); }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<Object> getErrors() { return errors; }
    public void setErrors(List<Object> errors) { this.errors = errors; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

}
