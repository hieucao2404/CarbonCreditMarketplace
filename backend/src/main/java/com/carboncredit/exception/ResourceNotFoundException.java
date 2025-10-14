package com.carboncredit.exception;

public class ResourceNotFoundException extends RuntimeException{
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String rsourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s", rsourceName, fieldName, fieldValue));
    }
}
