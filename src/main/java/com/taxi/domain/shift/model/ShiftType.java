package com.taxi.domain.shift.model;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Enum representing shift types
 */
public enum ShiftType {
    DAY("Day Shift", "06:00", "18:00"),
    NIGHT("Night Shift", "18:00", "06:00");
    
    private final String displayName;
    private final String startTime;
    private final String endTime;
    
    ShiftType(String displayName, String startTime, String endTime) {
        this.displayName = displayName;
        this.startTime = startTime;
        this.endTime = endTime;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getStartTime() {
        return startTime;
    }
    
    public String getEndTime() {
        return endTime;
    }
    
    /**
     * Get formatted hours string
     */
    public String getHours() {
        return startTime + " - " + endTime;
    }

    @JsonCreator
    public static ShiftType fromJson(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return ShiftType.valueOf(trimmed.toUpperCase());
    }
}
