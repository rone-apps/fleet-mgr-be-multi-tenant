package com.taxi.infrastructure.plugin.mapper;

/**
 * Exception thrown when data mapping fails.
 */
public class MappingException extends RuntimeException {

    public MappingException(String message) {
        super(message);
    }

    public MappingException(String message, Throwable cause) {
        super(message, cause);
    }
}
