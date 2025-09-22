package com.carboncredit.exception;

/**
 * Exception thrown when a user attempts to perform an unauthorized operation
 * Can be used for any security/authorization violation in the system
 */
public class UnauthorizedOperationException extends RuntimeException {
    
    public UnauthorizedOperationException(String userId, String entityType, String entityId, String operation) {
        super(String.format("User %s is not authorized to %s %s %s", userId, operation, entityType, entityId));
    }
    
    public UnauthorizedOperationException(String message) {
        super(message);
    }
    
    public UnauthorizedOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
