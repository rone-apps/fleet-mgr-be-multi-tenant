package com.taxi.infrastructure.plugin.core;

/**
 * Status of a plugin execution.
 */
public enum ExecutionStatus {
    /**
     * Execution completed successfully
     */
    SUCCESS,

    /**
     * Execution failed completely
     */
    FAILED,

    /**
     * Execution completed with some errors (partial success)
     */
    PARTIAL,

    /**
     * Execution is currently running
     */
    RUNNING
}
