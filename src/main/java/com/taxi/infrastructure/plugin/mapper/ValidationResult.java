package com.taxi.infrastructure.plugin.mapper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of a validation operation.
 * Contains validation status, errors, and warnings.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {

    /**
     * Whether validation passed
     */
    private boolean valid;

    /**
     * List of validation errors
     */
    private List<ValidationError> errors;

    /**
     * List of warnings (non-fatal issues)
     */
    private List<String> warnings;

    /**
     * Create a successful validation result.
     *
     * @return Validation result with valid=true
     */
    public static ValidationResult success() {
        return new ValidationResult(true, Collections.emptyList(), Collections.emptyList());
    }

    /**
     * Create a failed validation result with a single error.
     *
     * @param fieldName Field that failed validation
     * @param message Error message
     * @return Validation result with valid=false
     */
    public static ValidationResult failure(String fieldName, String message) {
        List<ValidationError> errors = new ArrayList<>();
        errors.add(new ValidationError(fieldName, message));
        return new ValidationResult(false, errors, Collections.emptyList());
    }

    /**
     * Create a failed validation result with multiple errors.
     *
     * @param errors List of validation errors
     * @return Validation result with valid=false
     */
    public static ValidationResult failure(List<ValidationError> errors) {
        return new ValidationResult(false, errors, Collections.emptyList());
    }

    /**
     * Add an error to this validation result and mark as invalid.
     *
     * @param fieldName Field name
     * @param message Error message
     */
    public void addError(String fieldName, String message) {
        if (errors == null) {
            errors = new ArrayList<>();
        }
        errors.add(new ValidationError(fieldName, message));
        this.valid = false;
    }

    /**
     * Add a warning to this validation result.
     *
     * @param warning Warning message
     */
    public void addWarning(String warning) {
        if (warnings == null) {
            warnings = new ArrayList<>();
        }
        warnings.add(warning);
    }

    /**
     * Check if there are any errors.
     *
     * @return true if errors list is not empty
     */
    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    /**
     * Check if there are any warnings.
     *
     * @return true if warnings list is not empty
     */
    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }
}
