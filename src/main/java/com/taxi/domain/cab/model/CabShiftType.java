package com.taxi.domain.cab.model;

/**
 * Enum representing shift type configuration for cabs
 * SINGLE - Cab operates with single shift per day
 * DOUBLE - Cab operates with two shifts per day (day/night)
 */
public enum CabShiftType {
    SINGLE("Single Shift"),
    DOUBLE("Double Shift");
    
    private final String displayName;
    
    CabShiftType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public boolean isSingleShift() {
        return this == SINGLE;
    }
    
    public boolean isDoubleShift() {
        return this == DOUBLE;
    }
}
