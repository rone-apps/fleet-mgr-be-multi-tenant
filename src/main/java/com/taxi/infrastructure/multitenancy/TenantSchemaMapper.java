package com.taxi.infrastructure.multitenancy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps tenant identifiers to their corresponding database schema names.
 *
 * This allows for flexible naming conventions where tenant IDs don't have to match
 * schema names exactly (e.g., "maclures" → "fareflow_maclures").
 */
@Component
@Slf4j
public class TenantSchemaMapper {

    private final Map<String, String> tenantToSchemaMap = new HashMap<>();

    public TenantSchemaMapper() {
        // Initialize tenant-to-schema mappings
        // Format: tenantId -> schemaName

        // Maclure's Cabs - maps to fareflow_maclures
        tenantToSchemaMap.put("mac-cabs", "fareflow_maclures");     // Primary login ID
        tenantToSchemaMap.put("maclures", "fareflow_maclures");     // Alias
        tenantToSchemaMap.put("maclures_cabs", "fareflow_maclures");// Alias

        // Bonny's Taxi - maps to fareflow_bonny
        tenantToSchemaMap.put("bonny-taxi", "fareflow_bonny");      // Primary login ID
        tenantToSchemaMap.put("bonny", "fareflow_bonny");           // Alias
        tenantToSchemaMap.put("bonnys", "fareflow_bonny");          // Alias
        tenantToSchemaMap.put("bonnys_taxi", "fareflow_bonny");     // Alias

        // Yellow Cabs - maps to fareflow_yellow
        tenantToSchemaMap.put("yellow-cabs", "fareflow_yellow");    // Primary login ID
        tenantToSchemaMap.put("yellow", "fareflow_yellow");         // Alias
        tenantToSchemaMap.put("yellowcabs", "fareflow_yellow");     // Alias
        tenantToSchemaMap.put("yellow_cabs", "fareflow_yellow");    // Alias

        // Demo Tenant - maps to fareflow_demo
        tenantToSchemaMap.put("demo", "fareflow_demo");

        log.info("TenantSchemaMapper initialized with {} mappings", tenantToSchemaMap.size());
        tenantToSchemaMap.forEach((tenant, schema) ->
            log.debug("  {} -> {}", tenant, schema)
        );
    }

    /**
     * Get the database schema name for a given tenant identifier.
     *
     * @param tenantId The tenant identifier (e.g., "maclures", "bonnys")
     * @return The database schema name (e.g., "fareflow_maclures", "fareflow_bonny")
     */
    public String getSchemaName(String tenantId) {
        if (tenantId == null || tenantId.isEmpty()) {
            log.warn("Null or empty tenant ID provided, returning system tenant");
            return TenantContext.SYSTEM_TENANT;
        }

        // Normalize tenant ID (lowercase, trim)
        String normalizedTenantId = tenantId.toLowerCase().trim();

        // Check if we have a mapping
        String schemaName = tenantToSchemaMap.get(normalizedTenantId);

        if (schemaName != null) {
            log.debug("Mapped tenant '{}' to schema '{}'", tenantId, schemaName);
            return schemaName;
        }

        // Reject unknown tenant IDs instead of using them as schema names
        log.error("Unknown tenant ID '{}' - tenant not registered in system", tenantId);
        throw new IllegalArgumentException(
            "Unknown tenant: '" + tenantId + "'. Valid tenants: " +
            String.join(", ", tenantToSchemaMap.keySet())
        );
    }

    /**
     * Add a new tenant-to-schema mapping dynamically.
     *
     * @param tenantId The tenant identifier
     * @param schemaName The database schema name
     */
    public void addMapping(String tenantId, String schemaName) {
        if (tenantId != null && !tenantId.isEmpty() && schemaName != null && !schemaName.isEmpty()) {
            tenantToSchemaMap.put(tenantId.toLowerCase().trim(), schemaName);
            log.info("Added tenant mapping: {} -> {}", tenantId, schemaName);
        } else {
            log.warn("Attempted to add invalid mapping: {} -> {}", tenantId, schemaName);
        }
    }

    /**
     * Remove a tenant-to-schema mapping.
     *
     * @param tenantId The tenant identifier to remove
     */
    public void removeMapping(String tenantId) {
        if (tenantId != null && !tenantId.isEmpty()) {
            String removed = tenantToSchemaMap.remove(tenantId.toLowerCase().trim());
            if (removed != null) {
                log.info("Removed tenant mapping: {} -> {}", tenantId, removed);
            }
        }
    }

    /**
     * Get all current tenant-to-schema mappings.
     *
     * @return Unmodifiable map of tenant IDs to schema names
     */
    public Map<String, String> getAllMappings() {
        return Map.copyOf(tenantToSchemaMap);
    }

    /**
     * Check if a tenant has an explicit mapping.
     *
     * @param tenantId The tenant identifier
     * @return true if an explicit mapping exists
     */
    public boolean hasMapping(String tenantId) {
        return tenantId != null && tenantToSchemaMap.containsKey(tenantId.toLowerCase().trim());
    }
}
