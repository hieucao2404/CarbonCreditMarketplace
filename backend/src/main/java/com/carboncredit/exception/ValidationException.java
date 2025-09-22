package com.carboncredit.exception;

/**
 * Exception thrown when entity validation fails (e.g., invalid data, business rule violations)
 * Can be used for any validation errors across all entities
 */
public class ValidationException extends RuntimeException {
    
    public ValidationException(String entityType, String field, String reason) {
        super(String.format("Validation failed for %s.%s: %s", entityType, field, reason));
    }
    
    public ValidationException(String message) {
        super(message);
    }
    
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
