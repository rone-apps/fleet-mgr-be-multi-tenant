package com.taxi.infrastructure.plugin.config;

import com.taxi.infrastructure.multitenancy.TenantContext;
import com.taxi.infrastructure.plugin.core.PluginException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for managing plugin configurations.
 * Note: plugin_config table exists ONLY in the default schema (fareflow).
 * This service queries the default schema directly to avoid circular dependency with tenant resolution.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PluginConfigService {

    private final DataSource dataSource;
    private static final String DEFAULT_SCHEMA = "fareflow";

    /**
     * Get plugin configuration for the current tenant.
     *
     * @param pluginId Plugin identifier
     * @return Plugin configuration, or empty if not found
     */
    @Cacheable(cacheNames = "plugin_config", key = "T(com.taxi.infrastructure.multitenancy.TenantContext).getCurrentTenant() + '_' + #pluginId")
    public Optional<PluginConfig> getPluginConfig(String pluginId) {
        String tenantId = TenantContext.getCurrentTenant();
        return getPluginConfig(tenantId, pluginId);
    }

    /**
     * Get plugin configuration for a specific tenant.
     *
     * @param tenantId Tenant identifier
     * @param pluginId Plugin identifier
     * @return Plugin configuration, or empty if not found
     */
    @Cacheable(cacheNames = "plugin_config", key = "#tenantId + '_' + #pluginId")
    public Optional<PluginConfig> getPluginConfig(String tenantId, String pluginId) {
        String sql = "SELECT id, tenant_id, plugin_id, config_data, enabled, " +
                     "schedule_config, created_at, updated_at, created_by, updated_by " +
                     "FROM " + DEFAULT_SCHEMA + ".plugin_config " +
                     "WHERE tenant_id = ? AND plugin_id = ?";

        log.debug("Fetching plugin config for tenant={}, plugin={}", tenantId, pluginId);

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, tenantId);
            ps.setString(2, pluginId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    PluginConfig config = PluginConfig.builder()
                            .id(rs.getLong("id"))
                            .tenantId(rs.getString("tenant_id"))
                            .pluginId(rs.getString("plugin_id"))
                            .configData(rs.getString("config_data"))
                            .enabled(rs.getBoolean("enabled"))
                            .scheduleConfig(rs.getString("schedule_config"))
                            .createdAt(rs.getTimestamp("created_at") != null ?
                                      rs.getTimestamp("created_at").toLocalDateTime() : null)
                            .updatedAt(rs.getTimestamp("updated_at") != null ?
                                      rs.getTimestamp("updated_at").toLocalDateTime() : null)
                            .createdBy(rs.getString("created_by"))
                            .updatedBy(rs.getString("updated_by"))
                            .build();

                    log.debug("Found plugin config for tenant={}, plugin={}", tenantId, pluginId);
                    return Optional.of(config);
                }
            }
        } catch (Exception e) {
            log.error("Error fetching plugin config for tenant={}, plugin={}", tenantId, pluginId, e);
        }

        return Optional.empty();
    }

    /**
     * Save or update plugin configuration.
     *
     * @param tenantId Tenant identifier
     * @param pluginId Plugin identifier
     * @param configData Configuration JSON data
     * @param enabled Whether plugin is enabled
     * @return Saved configuration
     */
    @CacheEvict(cacheNames = "plugin_config", key = "#tenantId + '_' + #pluginId")
    public PluginConfig savePluginConfig(String tenantId, String pluginId,
                                          String configData, boolean enabled) {
        return savePluginConfig(tenantId, pluginId, configData, null, enabled, null);
    }

    /**
     * Save or update plugin configuration with full details.
     *
     * @param tenantId Tenant identifier
     * @param pluginId Plugin identifier
     * @param configData Configuration JSON data
     * @param scheduleConfig Schedule configuration JSON data
     * @param enabled Whether plugin is enabled
     * @param userId User performing the operation
     * @return Saved configuration
     */
    @CacheEvict(cacheNames = "plugin_config", key = "#tenantId + '_' + #pluginId")
    public PluginConfig savePluginConfig(String tenantId, String pluginId,
                                          String configData, String scheduleConfig,
                                          boolean enabled, String userId) {
        // Check if config exists
        Optional<PluginConfig> existing = getPluginConfigUncached(tenantId, pluginId);

        if (existing.isPresent()) {
            // Update existing config
            return updatePluginConfig(existing.get().getId(), configData, scheduleConfig, enabled, userId);
        } else {
            // Insert new config
            return insertPluginConfig(tenantId, pluginId, configData, scheduleConfig, enabled, userId);
        }
    }

    /**
     * Insert a new plugin configuration.
     */
    private PluginConfig insertPluginConfig(String tenantId, String pluginId,
                                             String configData, String scheduleConfig,
                                             boolean enabled, String userId) {
        String sql = "INSERT INTO " + DEFAULT_SCHEMA + ".plugin_config " +
                     "(tenant_id, plugin_id, config_data, schedule_config, enabled, created_at, created_by) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, tenantId);
            ps.setString(2, pluginId);
            ps.setString(3, configData);
            ps.setString(4, scheduleConfig);
            ps.setBoolean(5, enabled);
            ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(7, userId);

            int affected = ps.executeUpdate();

            if (affected == 0) {
                throw new PluginException("Failed to insert plugin configuration");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    Long id = generatedKeys.getLong(1);
                    log.info("Inserted plugin config: id={}, tenant={}, plugin={}", id, tenantId, pluginId);
                    return getPluginConfig(tenantId, pluginId).orElseThrow();
                }
            }
        } catch (Exception e) {
            log.error("Error inserting plugin config for tenant={}, plugin={}", tenantId, pluginId, e);
            throw new PluginException("Failed to save plugin configuration", e);
        }

        throw new PluginException("Failed to insert plugin configuration");
    }

    /**
     * Update an existing plugin configuration.
     */
    private PluginConfig updatePluginConfig(Long id, String configData, String scheduleConfig,
                                             boolean enabled, String userId) {
        String sql = "UPDATE " + DEFAULT_SCHEMA + ".plugin_config " +
                     "SET config_data = ?, schedule_config = ?, enabled = ?, " +
                     "updated_at = ?, updated_by = ? " +
                     "WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, configData);
            ps.setString(2, scheduleConfig);
            ps.setBoolean(3, enabled);
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(5, userId);
            ps.setLong(6, id);

            int affected = ps.executeUpdate();

            if (affected == 0) {
                throw new PluginException("Failed to update plugin configuration");
            }

            log.info("Updated plugin config: id={}", id);

            // Fetch updated config - need to get tenant_id and plugin_id from the existing record
            return getPluginConfigById(id).orElseThrow();

        } catch (Exception e) {
            log.error("Error updating plugin config id={}", id, e);
            throw new PluginException("Failed to update plugin configuration", e);
        }
    }

    /**
     * Get plugin configuration by ID (used internally after updates).
     */
    private Optional<PluginConfig> getPluginConfigById(Long id) {
        String sql = "SELECT id, tenant_id, plugin_id, config_data, enabled, " +
                     "schedule_config, created_at, updated_at, created_by, updated_by " +
                     "FROM " + DEFAULT_SCHEMA + ".plugin_config WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        } catch (Exception e) {
            log.error("Error fetching plugin config by id={}", id, e);
        }

        return Optional.empty();
    }

    /**
     * Get plugin config without cache (used internally).
     */
    private Optional<PluginConfig> getPluginConfigUncached(String tenantId, String pluginId) {
        String sql = "SELECT id, tenant_id, plugin_id, config_data, enabled, " +
                     "schedule_config, created_at, updated_at, created_by, updated_by " +
                     "FROM " + DEFAULT_SCHEMA + ".plugin_config " +
                     "WHERE tenant_id = ? AND plugin_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, tenantId);
            ps.setString(2, pluginId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        } catch (Exception e) {
            log.error("Error fetching plugin config for tenant={}, plugin={}", tenantId, pluginId, e);
        }

        return Optional.empty();
    }

    /**
     * Map ResultSet to PluginConfig entity.
     */
    private PluginConfig mapResultSet(ResultSet rs) throws SQLException {
        return PluginConfig.builder()
                .id(rs.getLong("id"))
                .tenantId(rs.getString("tenant_id"))
                .pluginId(rs.getString("plugin_id"))
                .configData(rs.getString("config_data"))
                .enabled(rs.getBoolean("enabled"))
                .scheduleConfig(rs.getString("schedule_config"))
                .createdAt(rs.getTimestamp("created_at") != null ?
                          rs.getTimestamp("created_at").toLocalDateTime() : null)
                .updatedAt(rs.getTimestamp("updated_at") != null ?
                          rs.getTimestamp("updated_at").toLocalDateTime() : null)
                .createdBy(rs.getString("created_by"))
                .updatedBy(rs.getString("updated_by"))
                .build();
    }

    /**
     * Check if a plugin is enabled for the current tenant.
     *
     * @param pluginId Plugin identifier
     * @return true if plugin is enabled
     */
    public boolean isPluginEnabled(String pluginId) {
        String tenantId = TenantContext.getCurrentTenant();
        return isPluginEnabled(tenantId, pluginId);
    }

    /**
     * Check if a plugin is enabled for a specific tenant.
     *
     * @param tenantId Tenant identifier
     * @param pluginId Plugin identifier
     * @return true if plugin is enabled
     */
    public boolean isPluginEnabled(String tenantId, String pluginId) {
        return getPluginConfig(tenantId, pluginId)
                .map(PluginConfig::isEnabled)
                .orElse(false);
    }

    /**
     * Toggle plugin enabled status.
     *
     * @param pluginId Plugin identifier
     */
    @CacheEvict(cacheNames = "plugin_config", key = "T(com.taxi.infrastructure.multitenancy.TenantContext).getCurrentTenant() + '_' + #pluginId")
    public void togglePlugin(String pluginId) {
        String tenantId = TenantContext.getCurrentTenant();
        togglePlugin(tenantId, pluginId);
    }

    /**
     * Toggle plugin enabled status for a specific tenant.
     *
     * @param tenantId Tenant identifier
     * @param pluginId Plugin identifier
     */
    @CacheEvict(cacheNames = "plugin_config", key = "#tenantId + '_' + #pluginId")
    public void togglePlugin(String tenantId, String pluginId) {
        Optional<PluginConfig> config = getPluginConfig(tenantId, pluginId);
        if (config.isPresent()) {
            boolean newStatus = !config.get().isEnabled();
            updatePluginConfig(config.get().getId(),
                             config.get().getConfigData(),
                             config.get().getScheduleConfig(),
                             newStatus,
                             null);
            log.info("Toggled plugin {} for tenant {} to {}", pluginId, tenantId, newStatus);
        } else {
            throw new PluginException("Plugin configuration not found: " + pluginId);
        }
    }

    /**
     * Get all plugin configurations for the current tenant.
     *
     * @return List of all plugin configurations
     */
    public java.util.List<PluginConfig> getAllPluginConfigs() {
        String tenantId = TenantContext.getCurrentTenant();
        return getAllPluginConfigs(tenantId);
    }

    /**
     * Get all plugin configurations for a specific tenant.
     *
     * @param tenantId Tenant identifier
     * @return List of all plugin configurations
     */
    public java.util.List<PluginConfig> getAllPluginConfigs(String tenantId) {
        String sql = "SELECT id, tenant_id, plugin_id, config_data, enabled, " +
                     "schedule_config, created_at, updated_at, created_by, updated_by " +
                     "FROM " + DEFAULT_SCHEMA + ".plugin_config " +
                     "WHERE tenant_id = ? " +
                     "ORDER BY plugin_id";

        log.debug("Fetching all plugin configs for tenant={}", tenantId);

        java.util.List<PluginConfig> configs = new java.util.ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, tenantId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PluginConfig config = PluginConfig.builder()
                            .id(rs.getLong("id"))
                            .tenantId(rs.getString("tenant_id"))
                            .pluginId(rs.getString("plugin_id"))
                            .configData(rs.getString("config_data"))
                            .enabled(rs.getBoolean("enabled"))
                            .scheduleConfig(rs.getString("schedule_config"))
                            .createdAt(rs.getTimestamp("created_at") != null ?
                                      rs.getTimestamp("created_at").toLocalDateTime() : null)
                            .updatedAt(rs.getTimestamp("updated_at") != null ?
                                      rs.getTimestamp("updated_at").toLocalDateTime() : null)
                            .createdBy(rs.getString("created_by"))
                            .updatedBy(rs.getString("updated_by"))
                            .build();

                    configs.add(config);
                }
            }

            log.debug("Found {} plugin configs for tenant={}", configs.size(), tenantId);
        } catch (Exception e) {
            log.error("Error fetching all plugin configs for tenant={}", tenantId, e);
        }

        return configs;
    }
}
