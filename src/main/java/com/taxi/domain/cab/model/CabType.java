package com.taxi.domain.cab.model;

/**
 * Enum representing taxi vehicle types
 */
public enum CabType {
    SEDAN("Sedan"),
    HANDICAP_VAN("Handicap Van");
    
    private final String displayName;
    
    CabType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public boolean isVan() {
        return this == HANDICAP_VAN;
    }
}
