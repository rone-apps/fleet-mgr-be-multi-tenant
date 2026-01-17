package com.taxi.domain.cab.model;

/**
 * Enum representing share type classification for cabs
 */
public enum ShareType {
    VOTING_SHARE("Voting Share"),
    NON_VOTING_SHARE("Non-Voting Share");
    
    private final String displayName;
    
    ShareType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public boolean isVoting() {
        return this == VOTING_SHARE;
    }
}
