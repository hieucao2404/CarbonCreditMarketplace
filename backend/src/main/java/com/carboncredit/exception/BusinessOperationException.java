package com.carboncredit.exception;

/**
 * Exception thrown when any operation fails due to business rules
 * Can be used for any failed operations across all services
 */
public class BusinessOperationException extends RuntimeException {
    
    public BusinessOperationException(String entityType, String operation, String reason) {
        super(String.format("%s operation '%s' failed: %s", entityType, operation, reason));
    }
    
    public BusinessOperationException(String message) {
        super(message);
    }
    
    public BusinessOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
