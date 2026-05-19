package com.taxi.domain.statement.model;

/**
 * Direction of balance transfer - determines when transfer should be applied
 */
public enum BalanceDirection {
    /**
     * Transfer only when source person has positive balance (is owed money by company)
     * Example: Driver has $500 payable from company, wants to transfer to another driver
     */
    POSITIVE_ONLY,

    /**
     * Transfer both positive and negative balances
     * Applies regardless of whether source owes company or company owes source
     */
    BOTH
}
