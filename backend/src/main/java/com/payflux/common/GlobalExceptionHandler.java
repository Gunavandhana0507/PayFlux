package com.payflux.common;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ApiError("VALIDATION_ERROR", "Please correct the highlighted fields", errors));
    }
    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiError> constraint(ConstraintViolationException ex) {
        return ResponseEntity.badRequest().body(new ApiError("VALIDATION_ERROR", ex.getMessage(), Map.of()));
    }
    @ExceptionHandler(ApiExceptions.NotFoundException.class)
    ResponseEntity<ApiError> notFound(ApiExceptions.NotFoundException ex) { return ResponseEntity.status(404).body(error("NOT_FOUND", ex.getMessage())); }
    @ExceptionHandler(ApiExceptions.ConflictException.class)
    ResponseEntity<ApiError> conflict(ApiExceptions.ConflictException ex) { return ResponseEntity.status(409).body(error(ex.code, ex.getMessage())); }
    @ExceptionHandler(ApiExceptions.BusinessRuleException.class)
    ResponseEntity<ApiError> business(ApiExceptions.BusinessRuleException ex) { return ResponseEntity.status(422).body(error(ex.code, ex.getMessage())); }
    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<ApiError> credentials() { return ResponseEntity.status(401).body(error("INVALID_CREDENTIALS", "Email or password is incorrect")); }
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> denied() { return ResponseEntity.status(403).body(error("FORBIDDEN", "You do not have permission to perform this action")); }
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> fallback(Exception ex) { log.error("Unhandled API error", ex); return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error("INTERNAL_ERROR", "Something went wrong on our side")); }
    private ApiError error(String code, String message) { return new ApiError(code, message, Map.of()); }
}
