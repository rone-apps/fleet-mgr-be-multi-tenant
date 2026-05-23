package com.taxi.infrastructure.plugin.core;

import com.taxi.infrastructure.plugin.config.PluginConfig;

/**
 * Base interface for all FareFlow plugins.
 * Plugins extend this interface to integrate with third-party systems (dispatch, payment, data import, etc.).
 */
public interface Plugin {

    /**
     * Get plugin metadata including identity, capabilities, and configuration fields.
     *
     * @return Plugin metadata
     */
    PluginMetadata getMetadata();

    /**
     * Check if this plugin is enabled for a specific tenant.
     *
     * @param tenantId Tenant identifier
     * @return true if plugin is enabled for this tenant
     */
    boolean isEnabled(String tenantId);

    /**
     * Execute the plugin's main functionality.
     *
     * @param context Execution context containing parameters and metadata
     * @return Execution result with status, counts, and errors
     * @throws PluginException if execution fails
     */
    PluginExecutionResult execute(PluginContext context) throws PluginException;

    /**
     * Validate plugin configuration before saving.
     *
     * @param config Plugin configuration to validate
     * @throws PluginException if configuration is invalid
     */
    void validateConfiguration(PluginConfig config) throws PluginException;
}
