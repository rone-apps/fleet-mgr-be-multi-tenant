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
     * Requires: shiftProfileId
     */
    SHIFT_PROFILE("Shift Profile", "Apply to all shifts with a specific profile"),

    /**
     * SPECIFIC_SHIFT: Apply to one specific shift only
     * Requires: specificShiftId
     */
    SPECIFIC_SHIFT("Specific Shift", "Apply to one specific shift"),

    /**
     * SPECIFIC_PERSON: Apply to a specific driver or owner
     * Requires: specificPersonId
     */
    SPECIFIC_PERSON("Specific Person", "Apply to a specific driver or owner"),

    /**
     * ALL_OWNERS: Apply to all owners
     * No additional fields required
     */
    ALL_OWNERS("All Owners", "Apply to all owners"),

    /**
     * ALL_DRIVERS: Apply to all drivers
     * No additional fields required
     */
    ALL_DRIVERS("All Drivers", "Apply to all drivers"),

    /**
     * ALL_ACTIVE_SHIFTS: Apply to all currently active shifts (owners only)
     * No additional fields required
     */
    ALL_ACTIVE_SHIFTS("All Active Shifts", "Apply to all currently active shifts"),

    /**
     * SHIFTS_WITH_ATTRIBUTE: Apply to all shifts that have a specific attribute
     * Requires: attributeTypeId
     */
    SHIFTS_WITH_ATTRIBUTE("Shifts with Attribute", "Apply to all shifts with a specific attribute");

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
