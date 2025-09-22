package com.carboncredit.exception;

/**
 * Exception thrown when attempting an invalid status transition or state change
 * Can be used for any entity with status/state management
 */
public class InvalidStatusException extends RuntimeException {
    
    public InvalidStatusException(String entityType, Object currentStatus, String operation) {
        super(String.format("Cannot %s %s with status %s", operation, entityType, currentStatus));
    }
    
    public InvalidStatusException(String message) {
        super(message);
    }
    
    public InvalidStatusException(String message, Throwable cause) {
        super(message, cause);
    }
}
