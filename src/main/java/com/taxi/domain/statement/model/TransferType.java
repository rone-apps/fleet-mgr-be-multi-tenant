package com.taxi.domain.statement.model;

/**
 * Type of balance transfer between drivers/owners
 */
public enum TransferType {
    /**
     * One-time transfer for a specific statement period
     * Requires statementPeriodFrom and statementPeriodTo to be set
     */
    ONE_TIME,

    /**
     * Recurring transfer that applies to multiple statement periods
     * Uses startDate and endDate to determine applicable periods
     */
    RECURRING
}
