package com.taxi.domain.plugin.service;

import com.taxi.domain.plugin.dto.PluginExecutionSummary;
import com.taxi.domain.plugin.model.PluginExecution;
import com.taxi.domain.plugin.model.PluginInstallation;
import com.taxi.domain.plugin.repository.PluginExecutionRepository;
import com.taxi.domain.plugin.repository.PluginInstallationRepository;
import com.taxi.infrastructure.multitenancy.TenantContext;
import com.taxi.infrastructure.plugin.core.*;
import com.taxi.infrastructure.plugin.registry.PluginRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Orchestration service for plugin execution and management.
 * Handles plugin execution lifecycle, logging, and history tracking.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PluginOrchestrationService {

    private final PluginRegistry pluginRegistry;
    private final PluginExecutionRepository executionRepository;
    private final PluginInstallationRepository installationRepository;

    /**
     * Execute a plugin with the given context.
     * Records execution start/end and logs results to plugin_execution table.
     *
     * @param pluginId Plugin identifier
     * @param context Execution context
     * @return Execution result
     * @throws PluginException if plugin not found or execution fails
     */
    @Transactional
    public PluginExecutionResult executePlugin(String pluginId, PluginContext context) {
        String tenantId = context.getTenantId() != null ? context.getTenantId() : TenantContext.getCurrentTenant();

        log.info("Executing plugin: {} for tenant: {}", pluginId, tenantId);

        // Get plugin from registry
        Plugin plugin = pluginRegistry.getPlugin(pluginId)
                .orElseThrow(() -> new PluginException("Plugin not found: " + pluginId));

        // Check if plugin is enabled
        if (!plugin.isEnabled(tenantId)) {
            throw new PluginException("Plugin not enabled for tenant: " + tenantId);
        }

        // Create execution record
        PluginExecution execution = PluginExecution.builder()
                .pluginId(pluginId)
                .executionType(context.getExecutionType() != null ? context.getExecutionType() : "MANUAL")
                .startTime(LocalDateTime.now())
                .status("RUNNING")
                .build();

        execution = executionRepository.save(execution);
        log.info("Created execution record: id={}", execution.getId());

        PluginExecutionResult result;

        try {
            // Execute plugin
            result = plugin.execute(context);

            // Update execution record with results
            execution.setEndTime(result.getEndTime());
            execution.setStatus(result.getStatus().name());
            execution.setRecordsProcessed(result.getRecordsProcessed());
            execution.setRecordsSuccess(result.getRecordsSuccess());
            execution.setRecordsFailed(result.getRecordsFailed());

            if (result.getError() != null) {
                execution.setErrorMessage(result.getError());
            }

            executionRepository.save(execution);

            // Update plugin installation with last execution info
            updatePluginInstallation(pluginId, result);

            log.info("Plugin execution completed: plugin={}, status={}, processed={}, success={}, failed={}",
                     pluginId, result.getStatus(), result.getRecordsProcessed(),
                     result.getRecordsSuccess(), result.getRecordsFailed());

            return result;

        } catch (Exception e) {
            log.error("Plugin execution failed: plugin={}", pluginId, e);

            // Update execution record with failure
            execution.setEndTime(LocalDateTime.now());
            execution.setStatus("FAILED");
            execution.setErrorMessage(e.getMessage());
            executionRepository.save(execution);

            // Update plugin installation
            updatePluginInstallationWithFailure(pluginId);

            throw new PluginException("Plugin execution failed: " + e.getMessage(), e);
        }
    }

    /**
     * Update plugin installation record with successful execution info.
     */
    private void updatePluginInstallation(String pluginId, PluginExecutionResult result) {
        try {
            PluginInstallation installation = installationRepository.findByPluginId(pluginId)
                    .orElseGet(() -> createPluginInstallation(pluginId));

            installation.setLastExecutedAt(result.getEndTime());
            installation.setLastExecutionStatus(result.getStatus().name());

            installationRepository.save(installation);
        } catch (Exception e) {
            log.warn("Failed to update plugin installation for {}", pluginId, e);
        }
    }

    /**
     * Update plugin installation with failure status.
     */
    private void updatePluginInstallationWithFailure(String pluginId) {
        try {
            PluginInstallation installation = installationRepository.findByPluginId(pluginId)
                    .orElseGet(() -> createPluginInstallation(pluginId));

            installation.setLastExecutedAt(LocalDateTime.now());
            installation.setLastExecutionStatus("FAILED");

            installationRepository.save(installation);
        } catch (Exception e) {
            log.warn("Failed to update plugin installation for {}", pluginId, e);
        }
    }

    /**
     * Create a new plugin installation record.
     */
    private PluginInstallation createPluginInstallation(String pluginId) {
        Plugin plugin = pluginRegistry.getPlugin(pluginId).orElseThrow();
        PluginMetadata metadata = plugin.getMetadata();

        return PluginInstallation.builder()
                .pluginId(pluginId)
                .active(true)
                .version(metadata.getVersion())
                .installedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Get execution history for a plugin.
     *
     * @param tenantId Tenant identifier (used for logging, not filtering)
     * @param pluginId Plugin identifier
     * @param limit Maximum number of records to return
     * @return List of execution summaries
     */
    public List<PluginExecutionSummary> getExecutionHistory(String tenantId, String pluginId, int limit) {
        log.debug("Fetching execution history for plugin={}, tenant={}, limit={}", pluginId, tenantId, limit);

        List<PluginExecution> executions = executionRepository.findByPluginIdOrderByStartTimeDesc(
                pluginId,
                PageRequest.of(0, limit)
        );

        return executions.stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    /**
     * Convert PluginExecution entity to PluginExecutionSummary DTO.
     */
    private PluginExecutionSummary toSummary(PluginExecution execution) {
        return PluginExecutionSummary.builder()
                .id(execution.getId())
                .pluginId(execution.getPluginId())
                .executionType(execution.getExecutionType())
                .startTime(execution.getStartTime())
                .endTime(execution.getEndTime())
                .status(execution.getStatus())
                .recordsProcessed(execution.getRecordsProcessed())
                .recordsSuccess(execution.getRecordsSuccess())
                .recordsFailed(execution.getRecordsFailed())
                .errorMessage(execution.getErrorMessage())
                .build();
    }

    /**
     * Get plugin installation for a specific plugin.
     *
     * @param pluginId Plugin identifier
     * @return Plugin installation, or null if not found
     */
    public PluginInstallation getPluginInstallation(String pluginId) {
        return installationRepository.findByPluginId(pluginId).orElse(null);
    }

    /**
     * Get all active plugin installations.
     *
     * @return List of active installations
     */
    public List<PluginInstallation> getActiveInstallations() {
        return installationRepository.findByActive(true);
    }
}
