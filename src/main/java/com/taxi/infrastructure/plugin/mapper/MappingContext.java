package com.taxi.infrastructure.plugin.mapper;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Context for data mapping operations.
 * Contains tenant information, caches, field mappings, and execution metadata.
 */
@Value
@Builder
public class MappingContext {

    /**
     * Tenant ID for this mapping operation
     */
    String tenantId;

    /**
     * Field name mappings (source field → target field)
     * Used for CSV/Excel imports where headers may vary
     */
    @Singular("fieldMapping")
    Map<String, String> fieldMappings;

    /**
     * Metadata cache (repositories, lookups, etc.)
     * Prevents repeated database queries during batch mapping
     */
    @Singular("metadataEntry")
    Map<String, Object> metadata;

    /**
     * Execution timestamp
     */
    LocalDateTime executionTime;

    /**
     * Get a cached metadata value.
     *
     * @param key Metadata key
     * @param type Expected type
     * @param <T> Type parameter
     * @return Cached value, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T getMetadata(String key, Class<T> type) {
        Object value = metadata.get(key);
        if (value == null) {
            return null;
        }
        return (T) value;
    }

    /**
     * Get a field mapping.
     *
     * @param sourceField Source field name
     * @return Target field name, or null if no mapping exists
     */
    public String getMappedField(String sourceField) {
        return fieldMappings.get(sourceField);
    }

    /**
     * Check if a field mapping exists.
     *
     * @param sourceField Source field name
     * @return true if mapping exists
     */
    public boolean hasMapping(String sourceField) {
        return fieldMappings.containsKey(sourceField);
    }
}
