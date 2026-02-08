package com.taxi.domain.expense.model;

import com.taxi.domain.cab.model.CabShiftType;
import com.taxi.domain.cab.model.CabType;
import com.taxi.domain.shift.model.CabShift.ShiftStatus;
import com.taxi.domain.cab.model.ShareType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * MatchingCriteria - POJO for storing attribute-based matching criteria as JSON
 * Supports static cab attributes and dynamic/custom attributes
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchingCriteria {

    /**
     * Static cab attributes
     */
    private ShareTypeRule shareType;           // VOTING_SHARE, NON_VOTING_SHARE, null (any)
    private Boolean hasAirportLicense;         // true, false, null (any)
    private CabShiftTypeRule cabShiftType;     // SINGLE, DOUBLE, null (any)
    private CabTypeRule cabType;               // SEDAN, HANDICAP_VAN, null (any)
    private CabStatusRule status;              // ACTIVE, MAINTENANCE, RETIRED, null (any)

    /**
     * Dynamic attributes (custom cab attributes)
     */
    private List<DynamicAttributeRule> dynamicAttributes;

    /**
     * ShareType matching rule with negation support
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ShareTypeRule {
        private ShareType value;
        @Builder.Default
        private Boolean negate = false;  // true = NOT this share type
    }

    /**
     * CabShiftType matching rule
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CabShiftTypeRule {
        private CabShiftType value;
        @Builder.Default
        private Boolean negate = false;
    }

    /**
     * CabType matching rule
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CabTypeRule {
        private CabType value;
        @Builder.Default
        private Boolean negate = false;
    }

    /**
     * ShiftStatus matching rule (status is now at shift level)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CabStatusRule {
        private ShiftStatus value;
        @Builder.Default
        private Boolean negate = false;
    }

    /**
     * Dynamic attribute matching rule
     * Supports custom cab attributes with temporal tracking
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DynamicAttributeRule {
        private Long attributeTypeId;           // Reference to CabAttributeType
        private String attributeCode;           // For readability (e.g., "TRANSPONDER")
        @Builder.Default
        private Boolean mustHave = true;        // true = must have, false = must not have
        private String expectedValue;           // Optional: specific value to match
    }
}
