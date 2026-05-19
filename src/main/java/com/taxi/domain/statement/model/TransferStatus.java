package com.taxi.domain.statement.model;

/**
 * Status of a balance transfer
 */
public enum TransferStatus {
    /**
     * Transfer is active and will be applied to applicable statement periods
     */
    ACTIVE,

    /**
     * Transfer has been fully completed (all amounts transferred)
     * For ONE_TIME: applied to the specified period
     * For RECURRING: reached end date or transfer amount exhausted
     */
    COMPLETED,

    /**
     * Transfer has been cancelled and will no longer be applied
     * Previous applications remain in history
     */
    CANCELLED,

    /**
     * Transfer is temporarily suspended (e.g., during dispute resolution)
     * Can be resumed to ACTIVE status
     */
    SUSPENDED
}
