package com.taxi.domain.plugin.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Plugin execution audit log (per-tenant schema).
 * Records all plugin execution attempts with status and statistics.
 */
@Entity
@Table(name = "plugin_execution")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PluginExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Plugin identifier
     */
    @Column(name = "plugin_id", nullable = false, length = 50)
    private String pluginId;

    /**
     * Execution trigger type (SCHEDULED, MANUAL, WEBHOOK)
     */
    @Column(name = "execution_type", nullable = false, length = 30)
    private String executionType;

    /**
     * Execution start time
     */
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    /**
     * Execution end time
     */
    @Column(name = "end_time")
    private LocalDateTime endTime;

    /**
     * Execution status (SUCCESS, FAILED, PARTIAL, RUNNING)
     */
    @Column(name = "status", length = 20)
    private String status;

    /**
     * Total records attempted
     */
    @Column(name = "records_processed", nullable = false)
    @Builder.Default
    private int recordsProcessed = 0;

    /**
     * Successfully processed records
     */
    @Column(name = "records_success", nullable = false)
    @Builder.Default
    private int recordsSuccess = 0;

    /**
     * Failed records
     */
    @Column(name = "records_failed", nullable = false)
    @Builder.Default
    private int recordsFailed = 0;

    /**
     * Error message if execution failed
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Execution metadata (parameters, date ranges, filters, etc.)
     */
    @Column(name = "execution_metadata", columnDefinition = "JSON")
    private String executionMetadata;

    /**
     * User or system that triggered the execution
     */
    @Column(name = "created_by", length = 100)
    private String createdBy;

    @PrePersist
    protected void onCreate() {
        if (startTime == null) {
            startTime = LocalDateTime.now();
        }
        if (status == null) {
            status = "RUNNING";
        }
    }
}
