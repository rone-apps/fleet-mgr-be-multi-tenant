package com.taxi.web.dto.lease;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for creating/updating lease rate overrides
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaseRateOverrideDTO {
    
    private Long id;
    
    /**
     * Owner driver number (required)
     */
    private String ownerDriverNumber;
    
    /**
     * Specific cab number (optional - null means all owner's cabs)
     */
    private String cabNumber;
    
    /**
     * Shift type: "DAY", "NIGHT", or null for both
     */
    private String shiftType;
    
    /**
     * Day of week: "MONDAY", "TUESDAY", etc., or null for all days
     */
    private String dayOfWeek;
    
    /**
     * Custom lease rate
     */
    private BigDecimal leaseRate;
    
    /**
     * Start date (required)
     */
    private LocalDate startDate;
    
    /**
     * End date (optional - null means ongoing)
     */
    private LocalDate endDate;
    
    /**
     * Active flag
     */
    private Boolean isActive;
    
    /**
     * Priority (optional - auto-calculated if not provided)
     */
    private Integer priority;
    
    /**
     * Notes/description
     */
    private String notes;
    
    /**
     * Display-friendly summary
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Cab: ").append(cabNumber != null ? cabNumber : "All");
        sb.append(", Shift: ").append(shiftType != null ? shiftType : "Both");
        sb.append(", Day: ").append(dayOfWeek != null ? dayOfWeek : "All");
        sb.append(", Rate: $").append(leaseRate);
        sb.append(", ").append(endDate != null ? startDate + " to " + endDate : "Ongoing from " + startDate);
        return sb.toString();
    }
}

/**
 * DTO for bulk creating overrides
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class BulkLeaseRateOverrideDTO {
    
    private String ownerDriverNumber;
    private String cabNumber;
    private String shiftType;
    private java.util.List<String> daysOfWeek; // List of days: ["MONDAY", "TUESDAY", ...]
    private BigDecimal leaseRate;
    private LocalDate startDate;
    private LocalDate endDate;
    private String notes;
}

/**
 * DTO for lease rate lookup response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class LeaseRateLookupDTO {
    
    private BigDecimal leaseRate;
    private boolean isOverride; // true if using custom override, false if default
    private String source; // Description of which rule was applied
    private Long overrideId; // ID of override if applicable
}
