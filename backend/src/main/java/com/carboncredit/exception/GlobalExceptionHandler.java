package com.carboncredit.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for all application exceptions
 * Handles exceptions from any entity or service in the system
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEntityNotFound(
            EntityNotFoundException ex, WebRequest request) {
        log.warn("Entity not found: {}", ex.getMessage());
        return createErrorResponse(HttpStatus.NOT_FOUND, "ENTITY_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(InvalidStatusException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidStatus(
            InvalidStatusException ex, WebRequest request) {
        log.warn("Invalid status operation: {}", ex.getMessage());
        return createErrorResponse(HttpStatus.CONFLICT, "INVALID_STATUS", ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedOperationException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorizedOperation(
            UnauthorizedOperationException ex, WebRequest request) {
        log.warn("Unauthorized operation: {}", ex.getMessage());
        return createErrorResponse(HttpStatus.FORBIDDEN, "ACCESS_DENIED", ex.getMessage());
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            ValidationException ex, WebRequest request) {
        log.warn("Validation failed: {}", ex.getMessage());
        return createErrorResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.getMessage());
    }

    @ExceptionHandler(BusinessOperationException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessOperation(
            BusinessOperationException ex, WebRequest request) {
        log.error("Business operation failed: {}", ex.getMessage());
        return createErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY, "OPERATION_FAILED", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex, WebRequest request) {
        log.warn("Invalid argument: {}", ex.getMessage());
        return createErrorResponse(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(
            IllegalStateException ex, WebRequest request) {
        log.warn("Illegal state: {}", ex.getMessage());
        return createErrorResponse(HttpStatus.CONFLICT, "ILLEGAL_STATE", ex.getMessage());
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, Object>> handleSecurity(
            SecurityException ex, WebRequest request) {
        log.warn("Security violation: {}", ex.getMessage());
        return createErrorResponse(HttpStatus.FORBIDDEN, "SECURITY_VIOLATION", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex, WebRequest request) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", 
            "An unexpected error occurred. Please try again later.");
    }

    private ResponseEntity<Map<String, Object>> createErrorResponse(
            HttpStatus status, String errorCode, String message) {
        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("timestamp", LocalDateTime.now());
        errorBody.put("status", status.value());
        errorBody.put("error", status.getReasonPhrase());
        errorBody.put("errorCode", errorCode);
        errorBody.put("message", message);
        
        return new ResponseEntity<>(errorBody, status);
    }
}
