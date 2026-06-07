package com.taxi.web.dto.cab;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * DTO for cab reactivation request
 * Supports two modes:
 * 1. Same Owners: keepSameOwners=true, uses shiftOwnerAssignments (simple map)
 * 2. New Owners: keepSameOwners=false, uses shifts[] array (full configuration)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReactivateCabRequest {

    @NotNull(message = "Reactivation date is required")
    private LocalDate reactivationDate;

    private String reason; // e.g., "Repairs completed", "Lease renewed", "Repurchased"

    private String reactivatedBy; // Admin username (can derive from auth context)

    /**
     * If true: reactivate with same owners (quick mode)
     * If false: reactivate with new owners and full shift configuration
     */
    private Boolean keepSameOwners;

    /**
     * OPTION 1: Same Owners Mode
     * Map of shift IDs to owner IDs (keep same owners, just reactivate)
     * Used when keepSameOwners = true
     *
     * Example: { "123": 456, "124": 789 }
     * Shift 123 (DAY) keeps Driver 456
     * Shift 124 (NIGHT) keeps Driver 789
     */
    private Map<Long, Long> shiftOwnerAssignments;

    /**
     * OPTION 2: New Owners Mode
     * Full shift configuration with new owners, profiles, attributes
     * Used when keepSameOwners = false
     */
    @Valid
    private List<ReactivateShiftRequest> shifts;
}
