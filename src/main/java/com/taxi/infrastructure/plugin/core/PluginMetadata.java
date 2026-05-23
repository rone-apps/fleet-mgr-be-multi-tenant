package com.taxi.infrastructure.plugin.core;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Set;

/**
 * Metadata describing a plugin's identity, capabilities, and configuration requirements.
 */
@Value
@Builder
public class PluginMetadata {

    /**
     * Unique plugin identifier (e.g., "taxicaller", "icabbi", "moneris")
     */
    String pluginId;

    /**
     * Human-readable display name (e.g., "TaxiCaller Integration")
     */
    String displayName;

    /**
     * Plugin version
     */
    String version;

    /**
     * Plugin type
     */
    PluginType type;

    /**
     * Set of capabilities this plugin provides
     * (e.g., "DRIVER_SHIFTS", "TRIPS", "ACCOUNT_CHARGES", "CREDIT_CARD_SYNC")
     */
    Set<String> capabilities;

    /**
     * Configuration fields required by this plugin
     */
    List<ConfigField> configFields;

    /**
     * Plugin description
     */
    String description;

    /**
     * Icon URL for UI display
     */
    String iconUrl;

    /**
     * Vendor/provider name
     */
    String vendor;
}
