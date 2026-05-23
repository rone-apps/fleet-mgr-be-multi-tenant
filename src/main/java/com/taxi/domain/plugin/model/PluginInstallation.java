package com.taxi.domain.plugin.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Plugin installation record (per-tenant schema).
 * Tracks which plugins are installed and activated for the current tenant.
 */
@Entity
@Table(name = "plugin_installation")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PluginInstallation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Plugin identifier (e.g., "taxicaller", "icabbi", "moneris")
     */
    @Column(name = "plugin_id", nullable = false, length = 50)
    private String pluginId;

    /**
     * Whether this plugin installation is active
     */
    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    /**
     * Installed plugin version
     */
    @Column(name = "version", length = 20)
    private String version;

    /**
     * Instance-specific configuration (JSON format)
     * Used for per-resource configs like Moneris per-cab credentials
     */
    @Column(name = "instance_config", columnDefinition = "JSON")
    private String instanceConfig;

    /**
     * Installation timestamp
     */
    @Column(name = "installed_at", nullable = false, updatable = false)
    private LocalDateTime installedAt;

    /**
     * Last successful execution timestamp
     */
    @Column(name = "last_executed_at")
    private LocalDateTime lastExecutedAt;

    /**
     * Last execution status (SUCCESS, FAILED, PARTIAL)
     */
    @Column(name = "last_execution_status", length = 20)
    private String lastExecutionStatus;

    @PrePersist
    protected void onCreate() {
        if (installedAt == null) {
            installedAt = LocalDateTime.now();
        }
    }
}
