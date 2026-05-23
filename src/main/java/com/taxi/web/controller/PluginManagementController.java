package com.taxi.web.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxi.domain.plugin.dto.PluginExecutionSummary;
import com.taxi.domain.plugin.dto.PluginInstallRequest;
import com.taxi.domain.plugin.model.PluginInstallation;
import com.taxi.domain.plugin.service.PluginOrchestrationService;
import com.taxi.infrastructure.multitenancy.TenantContext;
import com.taxi.infrastructure.plugin.config.PluginConfig;
import com.taxi.infrastructure.plugin.config.PluginConfigService;
import com.taxi.infrastructure.plugin.core.Plugin;
import com.taxi.infrastructure.plugin.core.PluginContext;
import com.taxi.infrastructure.plugin.core.PluginExecutionResult;
import com.taxi.infrastructure.plugin.core.PluginMetadata;
import com.taxi.infrastructure.plugin.registry.PluginRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST API for plugin management.
 * Provides endpoints for listing, configuring, enabling/disabling, and executing plugins.
 */
@RestController
@RequestMapping("/plugins")
@RequiredArgsConstructor
@Slf4j
public class PluginManagementController {

    private final PluginRegistry pluginRegistry;
    private final PluginConfigService pluginConfigService;
    private final PluginOrchestrationService pluginOrchestrationService;
    private final ObjectMapper objectMapper;

    /**
     * List all available plugins.
     * Returns metadata for all registered plugins.
     *
     * @return Map of plugin ID to metadata
     */
    @GetMapping
    public ResponseEntity<Map<String, PluginMetadata>> listPlugins() {
        log.info("Listing all available plugins");
        Map<String, PluginMetadata> plugins = pluginRegistry.getAllPluginMetadata();
        return ResponseEntity.ok(plugins);
    }

    /**
     * List all plugin configurations for the current tenant.
     *
     * @return List of plugin configurations
     */
    @GetMapping("/configs")
    public ResponseEntity<List<PluginConfig>> listPluginConfigs() {
        String tenantId = TenantContext.getCurrentTenant();
        log.info("Listing all plugin configs for tenant: {}", tenantId);

        List<PluginConfig> configs = pluginConfigService.getAllPluginConfigs();
        return ResponseEntity.ok(configs);
    }

    /**
     * Get metadata for a specific plugin.
     *
     * @param pluginId Plugin identifier
     * @return Plugin metadata
     */
    @GetMapping("/{pluginId}")
    public ResponseEntity<PluginMetadata> getPlugin(@PathVariable String pluginId) {
        log.info("Fetching plugin metadata for: {}", pluginId);

        Optional<Plugin> plugin = pluginRegistry.getPlugin(pluginId);

        if (plugin.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(plugin.get().getMetadata());
    }

    /**
     * Install or configure a plugin for the current tenant.
     * Requires ADMIN role.
     *
     * @param pluginId Plugin identifier
     * @param request Installation request with configuration
     * @return Saved plugin configuration
     */
    @PostMapping("/{pluginId}/install")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<PluginConfig> installPlugin(
            @PathVariable String pluginId,
            @RequestBody PluginInstallRequest request) {

        String tenantId = TenantContext.getCurrentTenant();
        log.info("Installing plugin {} for tenant {}", pluginId, tenantId);

        // Verify plugin exists
        Optional<Plugin> plugin = pluginRegistry.getPlugin(pluginId);
        if (plugin.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            // Convert config data to JSON
            String configDataJson = objectMapper.writeValueAsString(request.getConfigData());
            String scheduleConfigJson = request.getScheduleConfig() != null ?
                    objectMapper.writeValueAsString(request.getScheduleConfig()) : null;

            // Save plugin configuration
            PluginConfig config = pluginConfigService.savePluginConfig(
                    tenantId,
                    pluginId,
                    configDataJson,
                    scheduleConfigJson,
                    request.isEnabled(),
                    null // userId will be set from security context in future
            );

            log.info("Plugin {} installed successfully for tenant {}", pluginId, tenantId);
            return ResponseEntity.ok(config);

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize plugin configuration", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Configure a plugin for the current tenant (alias for install).
     * Requires ADMIN role.
     *
     * @param pluginId Plugin identifier
     * @param request Installation request with configuration
     * @return Saved plugin configuration
     */
    @PostMapping("/{pluginId}/configure")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<PluginConfig> configurePlugin(
            @PathVariable String pluginId,
            @RequestBody PluginInstallRequest request) {
        // Delegate to install method (same functionality)
        return installPlugin(pluginId, request);
    }

    /**
     * Get plugin configuration for the current tenant.
     *
     * @param pluginId Plugin identifier
     * @return Plugin configuration
     */
    @GetMapping("/{pluginId}/config")
    public ResponseEntity<PluginConfig> getPluginConfig(@PathVariable String pluginId) {
        String tenantId = TenantContext.getCurrentTenant();
        log.info("Fetching plugin config for plugin={}, tenant={}", pluginId, tenantId);

        Optional<PluginConfig> config = pluginConfigService.getPluginConfig(pluginId);

        if (config.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(config.get());
    }

    /**
     * Toggle plugin enabled/disabled status.
     * Requires ADMIN role.
     *
     * @param pluginId Plugin identifier
     * @return Success response
     */
    @PutMapping("/{pluginId}/toggle")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> togglePlugin(@PathVariable String pluginId) {
        String tenantId = TenantContext.getCurrentTenant();
        log.info("Toggling plugin {} for tenant {}", pluginId, tenantId);

        pluginConfigService.togglePlugin(pluginId);

        boolean newStatus = pluginConfigService.isPluginEnabled(pluginId);
        log.info("Plugin {} is now {} for tenant {}", pluginId, newStatus ? "enabled" : "disabled", tenantId);

        return ResponseEntity.ok().build();
    }

    /**
     * Execute a plugin manually with provided parameters.
     * Requires ADMIN role.
     *
     * @param pluginId Plugin identifier
     * @param parameters Execution parameters (date ranges, filters, etc.)
     * @return Execution result
     */
    @PostMapping("/{pluginId}/execute")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<PluginExecutionResult> executePlugin(
            @PathVariable String pluginId,
            @RequestBody(required = false) Map<String, Object> parameters) {

        String tenantId = TenantContext.getCurrentTenant();
        log.info("Executing plugin {} for tenant {} with parameters: {}", pluginId, tenantId, parameters);

        // Build plugin context
        PluginContext.PluginContextBuilder contextBuilder = PluginContext.builder()
                .tenantId(tenantId)
                .executionType("MANUAL");

        if (parameters != null) {
            parameters.forEach(contextBuilder::parameter);
        }

        PluginContext context = contextBuilder.build();

        try {
            PluginExecutionResult result = pluginOrchestrationService.executePlugin(pluginId, context);
            log.info("Plugin {} execution completed with status: {}", pluginId, result.getStatus());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Plugin {} execution failed", pluginId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get execution history for a plugin.
     *
     * @param pluginId Plugin identifier
     * @param limit Maximum number of records (default: 10)
     * @return List of execution summaries
     */
    @GetMapping("/{pluginId}/executions")
    public ResponseEntity<List<PluginExecutionSummary>> getExecutionHistory(
            @PathVariable String pluginId,
            @RequestParam(defaultValue = "10") int limit) {

        String tenantId = TenantContext.getCurrentTenant();
        log.info("Fetching execution history for plugin={}, tenant={}, limit={}", pluginId, tenantId, limit);

        List<PluginExecutionSummary> history = pluginOrchestrationService.getExecutionHistory(
                tenantId,
                pluginId,
                limit
        );

        return ResponseEntity.ok(history);
    }

    /**
     * Get plugin installation info for the current tenant.
     *
     * @param pluginId Plugin identifier
     * @return Plugin installation details
     */
    @GetMapping("/{pluginId}/installation")
    public ResponseEntity<PluginInstallation> getPluginInstallation(@PathVariable String pluginId) {
        log.info("Fetching installation info for plugin: {}", pluginId);

        PluginInstallation installation = pluginOrchestrationService.getPluginInstallation(pluginId);

        if (installation == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(installation);
    }

    /**
     * Get all active plugin installations for the current tenant.
     *
     * @return List of active installations
     */
    @GetMapping("/installations/active")
    public ResponseEntity<List<PluginInstallation>> getActiveInstallations() {
        String tenantId = TenantContext.getCurrentTenant();
        log.info("Fetching active installations for tenant: {}", tenantId);

        List<PluginInstallation> installations = pluginOrchestrationService.getActiveInstallations();
        return ResponseEntity.ok(installations);
    }
}
