package com.taxi.domain.profile.model;

/**
 * Enum for per-unit expense rate types.
 * Defines what unit is used to calculate per-unit expenses (e.g., per mile, per airport trip).
 */
public enum ItemRateUnitType {
    MILEAGE("Miles driven", "mi"),
    AIRPORT_TRIP("Airport trips", "trip"),
    INSURANCE("Insurance (per mile)", "mi");

    private final String displayName;
    private final String symbol;

    ItemRateUnitType(String displayName, String symbol) {
        this.displayName = displayName;
        this.symbol = symbol;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSymbol() {
        return symbol;
    }
}
