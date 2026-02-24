package com.taxi.domain.profile.model;

/**
 * Enum for who a per-unit expense is charged to.
 * Determines whether the expense appears on the driver's or owner's financial statement.
 */
public enum ItemRateChargedTo {
    DRIVER("Charged to Driver"),
    OWNER("Charged to Owner");

    private final String displayName;

    ItemRateChargedTo(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
