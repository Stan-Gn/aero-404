package com.aero.exception;

public class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(String message) {
        super(message);
    }

    public EntityNotFoundException(String entityName, Long id) {
        super(entityName + " with id " + id + " not found");
    }
}
