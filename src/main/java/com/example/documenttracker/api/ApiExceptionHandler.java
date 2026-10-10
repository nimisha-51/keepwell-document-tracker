package com.example.documenttracker.api;

import com.example.documenttracker.service.DocumentService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.example.documenttracker.api.ApiDtos.ApiError;

@RestControllerAdvice(basePackages = "com.example.documenttracker.api")
public class ApiExceptionHandler {

    @ExceptionHandler(DocumentService.ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError notFound(DocumentService.ResourceNotFoundException exception) {
        return new ApiError(exception.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError validation(MethodArgumentNotValidException exception) {
        Map<String, String> fields = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return new ApiError("Validation failed.", fields);
    }

    @ExceptionHandler(ApiValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError validation(ApiValidationException exception) {
        return new ApiError("Validation failed.", Map.of(exception.getField(), exception.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError malformedRequest() {
        return new ApiError("Request body is malformed or contains an invalid value.", null);
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiError unauthenticated(AuthenticationException exception) {
        String message = exception instanceof BadCredentialsException
                ? "Email or password wasn't recognized." : "Authentication failed.";
        return new ApiError(message, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError conflict() {
        return new ApiError("The request conflicts with an existing record.", null);
    }
}
