package com.taxi.infrastructure.plugin.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Describes a configuration field required by a plugin.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfigField {

    /**
     * Field name (used as key in config JSON)
     */
    private String fieldName;

    /**
     * Human-readable label for display
     */
    private String displayLabel;

    /**
     * Field data type
     */
    private FieldType type;

    /**
     * Whether this field is required
     */
    private boolean required;

    /**
     * Default value (null if none)
     */
    private String defaultValue;

    /**
     * Validation pattern (regex for STRING fields)
     */
    private String validationPattern;
}
