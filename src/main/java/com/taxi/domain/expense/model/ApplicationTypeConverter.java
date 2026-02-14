package com.taxi.domain.expense.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * ApplicationTypeConverter - Converts ApplicationType enum to/from VARCHAR
 *
 * This converter allows Hibernate to store the enum as a string without
 * generating automatic CHECK constraints that limit the allowed values.
 * Application-level validation in @PrePersist/@PreUpdate enforces correctness.
 */
@Converter(autoApply = true)
public class ApplicationTypeConverter implements AttributeConverter<ApplicationType, String> {

    @Override
    public String convertToDatabaseColumn(ApplicationType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name();
    }

    @Override
    public ApplicationType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return ApplicationType.valueOf(dbData);
        } catch (IllegalArgumentException e) {
            // If an unknown value is in the database, log it and return null
            // Application validation will enforce valid types
            return null;
        }
    }
}
