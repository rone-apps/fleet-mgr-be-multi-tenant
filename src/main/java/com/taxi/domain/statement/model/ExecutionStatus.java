package com.taxi.domain.statement.model;

/**
 * Status workflow for statement balance transfer executions
 *
 * Flow:
 * PENDING → APPROVED → APPLIED → FINALIZED
 * PENDING → REJECTED
 */
public enum ExecutionStatus {
    /**
     * Generated and calculated, waiting for admin approval
     */
    PENDING,

    /**
     * Admin approved, ready to apply during statement generation
     */
    APPROVED,

    /**
     * Applied to statements, waiting for finalization
     */
    APPLIED,

    /**
     * Statements finalized, record is immutable
     */
    FINALIZED,

    /**
     * Admin rejected, will not be applied
     */
    REJECTED
}
