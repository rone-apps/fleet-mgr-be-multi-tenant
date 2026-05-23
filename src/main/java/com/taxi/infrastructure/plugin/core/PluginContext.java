package com.taxi.infrastructure.plugin.core;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.Map;

/**
 * Execution context for a plugin run.
 * Contains parameters, metadata, and configuration for a single execution.
 */
@Value
@Builder
public class PluginContext {

    /**
     * Tenant ID for this execution
     */
    String tenantId;

    /**
     * Execution type (SCHEDULED, MANUAL, WEBHOOK)
     */
    String executionType;

    /**
     * Execution parameters (e.g., startDate, endDate, filters)
     * Use @Singular to allow individual parameter additions
     */
    @Singular
    Map<String, Object> parameters;

    /**
     * Additional metadata (caches, repositories, etc.)
     */
    @Singular("metadataEntry")
    Map<String, Object> metadata;

    /**
     * Get a parameter value with type casting.
     *
     * @param key Parameter name
     * @param type Expected type
     * @param <T> Type parameter
     * @return Parameter value cast to type, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T getParameter(String key, Class<T> type) {
        Object value = parameters.get(key);
        if (value == null) {
            return null;
        }
        return (T) value;
    }

    /**
     * Get a metadata value with type casting.
     *
     * @param key Metadata key
     * @param type Expected type
     * @param <T> Type parameter
     * @return Metadata value cast to type, or null if not found
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
     * Create a plugin context from a parameter map.
     *
     * @param params Parameter map
     * @return Plugin context
     */
    public static PluginContext fromMap(Map<String, Object> params) {
        return PluginContext.builder()
                .parameters(params)
                .build();
    }
}
