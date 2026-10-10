package com.example.documenttracker.api;

public class ApiValidationException extends RuntimeException {

    private final String field;

    public ApiValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
