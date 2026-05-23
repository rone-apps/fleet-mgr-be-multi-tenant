package com.taxi.infrastructure.plugin.mapper;

/**
 * Generic interface for mapping data from external sources to internal FareFlow entities.
 * Plugins implement this interface to transform their specific data formats.
 *
 * @param <S> Source type (external data format)
 * @param <T> Target type (internal FareFlow entity)
 */
public interface DataMapper<S, T> {

    /**
     * Map source data to target entity.
     *
     * @param source Source data
     * @param context Mapping context with metadata and configuration
     * @return Mapped target entity
     * @throws MappingException if mapping fails
     */
    T map(S source, MappingContext context) throws MappingException;

    /**
     * Validate source data before mapping.
     *
     * @param source Source data to validate
     * @param context Mapping context
     * @return Validation result with errors/warnings
     */
    ValidationResult validate(S source, MappingContext context);

    /**
     * Check if this mapper supports the given source and target types.
     *
     * @param sourceType Source class
     * @param targetType Target class
     * @return true if this mapper can handle the conversion
     */
    boolean supports(Class<?> sourceType, Class<?> targetType);
}
