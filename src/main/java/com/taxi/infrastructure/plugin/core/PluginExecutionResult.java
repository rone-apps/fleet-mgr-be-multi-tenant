package com.taxi.infrastructure.plugin.core;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Result of a plugin execution.
 * Contains status, record counts, errors, and timing information.
 */
@Value
@Builder
public class PluginExecutionResult {

    /**
     * Plugin ID that was executed
     */
    String pluginId;

    /**
     * Execution status
     */
    ExecutionStatus status;

    /**
     * Execution start time
     */
    LocalDateTime startTime;

    /**
     * Execution end time
     */
    LocalDateTime endTime;

    /**
     * Total records processed
     */
    @Builder.Default
    int recordsProcessed = 0;

    /**
     * Successfully processed records
     */
    @Builder.Default
    int recordsSuccess = 0;

    /**
     * Failed records
     */
    @Builder.Default
    int recordsFailed = 0;

    /**
     * Error message (if failed)
     */
    String error;

    /**
     * List of warnings or non-fatal errors
     */
    List<String> warnings;

    /**
     * Additional result data
     */
    Object resultData;

    /**
     * Check if execution was successful.
     *
     * @return true if status is SUCCESS
     */
    public boolean isSuccessful() {
        return status == ExecutionStatus.SUCCESS;
    }

    /**
     * Check if execution failed.
     *
     * @return true if status is FAILED
     */
    public boolean isFailed() {
        return status == ExecutionStatus.FAILED;
    }
}
