package com.taxi.domain.expense.model;

/**
 * ApplicationType - Simplified enum for how an expense category applies to entities
 *
 * Replaces the complex attribute-based matching system with 5 straightforward application types.
 * Each type determines which entities receive the expense when it's created.
 */
public enum ApplicationType {
    /**
     * SHIFT_PROFILE: Apply to all shifts with a specific profile
     * Requires: expenseCategory.shiftProfileId
     */
    SHIFT_PROFILE("Shift Profile", "Apply to all shifts with a specific profile"),

    /**
     * SPECIFIC_SHIFT: Apply to one specific shift only
     * Requires: expenseCategory.specificShiftId
     */
    SPECIFIC_SHIFT("Specific Shift", "Apply to one specific shift"),

    /**
     * SPECIFIC_OWNER_DRIVER: Apply to a specific owner or driver
     * Requires: exactly one of expenseCategory.specificOwnerId or expenseCategory.specificDriverId
     */
    SPECIFIC_OWNER_DRIVER("Specific Owner/Driver", "Apply to a specific owner or driver"),

    /**
     * ALL_ACTIVE_SHIFTS: Apply to all currently active shifts
     * No additional fields required - automatically applies to all active shifts
     */
    ALL_ACTIVE_SHIFTS("All Active Shifts", "Apply to all currently active shifts"),

    /**
     * ALL_NON_OWNER_DRIVERS: Apply to all drivers who are not owners
     * No additional fields required - automatically applies to all non-owner drivers
     */
    ALL_NON_OWNER_DRIVERS("All Non-Owner Drivers", "Apply to all drivers who are not owners");

    private final String displayName;
    private final String description;

    ApplicationType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
