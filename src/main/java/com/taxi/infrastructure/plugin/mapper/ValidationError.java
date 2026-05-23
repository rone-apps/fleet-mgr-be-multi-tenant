package com.taxi.infrastructure.plugin.mapper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a validation error during data mapping.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationError {

    /**
     * Field name that failed validation
     */
    private String fieldName;

    /**
     * Error message
     */
    private String message;

    /**
     * Error code (optional, for programmatic handling)
     */
    private String errorCode;

    /**
     * The invalid value (optional)
     */
    private Object invalidValue;

    /**
     * Create a validation error with field name and message.
     *
     * @param fieldName Field name
     * @param message Error message
     */
    public ValidationError(String fieldName, String message) {
        this.fieldName = fieldName;
        this.message = message;
    }

    /**
     * Create a validation error with field, message, and invalid value.
     *
     * @param fieldName Field name
     * @param message Error message
     * @param invalidValue The invalid value
     */
    public ValidationError(String fieldName, String message, Object invalidValue) {
        this.fieldName = fieldName;
        this.message = message;
        this.invalidValue = invalidValue;
    }
}
