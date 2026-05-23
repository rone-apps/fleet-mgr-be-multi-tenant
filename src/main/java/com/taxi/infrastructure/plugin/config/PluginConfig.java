package com.taxi.infrastructure.plugin.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Plugin configuration entity.
 * Stores per-tenant plugin configuration in the shared 'fareflow' schema.
 */
@Entity
@Table(name = "plugin_config", schema = "fareflow")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PluginConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Tenant ID this configuration belongs to
     */
    @Column(name = "tenant_id", nullable = false, length = 63)
    private String tenantId;

    /**
     * Plugin ID (e.g., "taxicaller", "icabbi", "moneris")
     */
    @Column(name = "plugin_id", nullable = false, length = 50)
    private String pluginId;

    /**
     * Plugin-specific configuration (JSON format)
     */
    @Column(name = "config_data", nullable = false, columnDefinition = "JSON")
    private String configData;

    /**
     * Whether this plugin is enabled for this tenant
     */
    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private boolean enabled = true;

    /**
     * Scheduling configuration (JSON format)
     */
    @Column(name = "schedule_config", columnDefinition = "JSON")
    private String scheduleConfig;

    /**
     * Creation timestamp
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Last update timestamp
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * User who created this configuration
     */
    @Column(name = "created_by", length = 100)
    private String createdBy;

    /**
     * User who last updated this configuration
     */
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
