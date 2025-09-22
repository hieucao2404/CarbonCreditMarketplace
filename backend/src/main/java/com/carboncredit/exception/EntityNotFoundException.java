package com.carboncredit.exception;

/**
 * Exception thrown when an entity is not found by its ID
 * Can be used for any entity in the system (CarbonCredit, User, Journey, etc.)
 */
public class EntityNotFoundException extends RuntimeException {
    
    public EntityNotFoundException(String entityType, String id) {
        super(String.format("%s not found with ID: %s", entityType, id));
    }
    
    public EntityNotFoundException(String message) {
        super(message);
    }
    
    public EntityNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
