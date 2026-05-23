package com.taxi.domain.plugin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for installing/configuring a plugin.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PluginInstallRequest {

    /**
     * Plugin configuration data (will be serialized to JSON)
     */
    private Map<String, Object> configData;

    /**
     * Scheduling configuration (optional)
     */
    private Map<String, Object> scheduleConfig;

    /**
     * Whether to enable the plugin immediately
     */
    private boolean enabled = true;
}
