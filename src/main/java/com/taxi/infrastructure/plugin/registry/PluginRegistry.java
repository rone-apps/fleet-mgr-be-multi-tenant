package com.taxi.infrastructure.plugin.registry;

import com.taxi.infrastructure.plugin.core.Plugin;
import com.taxi.infrastructure.plugin.core.PluginMetadata;
import com.taxi.infrastructure.plugin.core.PluginType;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Registry for discovering and accessing plugins.
 * Plugins are auto-registered at Spring application startup.
 */
public interface PluginRegistry {

    /**
     * Register a plugin manually.
     *
     * @param plugin Plugin to register
     */
    void register(Plugin plugin);

    /**
     * Get a plugin by ID.
     *
     * @param pluginId Plugin identifier
     * @return Plugin instance, or empty if not found
     */
    Optional<Plugin> getPlugin(String pluginId);

    /**
     * Get all plugins of a specific type.
     *
     * @param type Plugin type
     * @return List of plugins of the specified type
     */
    List<Plugin> getPluginsByType(PluginType type);

    /**
     * Get all plugins enabled for a specific tenant.
     *
     * @param tenantId Tenant identifier
     * @return List of enabled plugins
     */
    List<Plugin> getEnabledPlugins(String tenantId);

    /**
     * Get metadata for all registered plugins.
     *
     * @return Map of plugin ID to metadata
     */
    Map<String, PluginMetadata> getAllPluginMetadata();

    /**
     * Get all registered plugins.
     *
     * @return List of all plugins
     */
    List<Plugin> getAllPlugins();
}
