package com.taxi.web.dto.profile;

import com.taxi.domain.cab.model.CabType;
import com.taxi.domain.cab.model.ShareType;
import com.taxi.domain.shift.model.ShiftType;
import lombok.*;

import java.util.List;

/**
 * UpdateShiftProfileRequest - Request DTO for updating an existing shift profile
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class UpdateShiftProfileRequest {

    private String profileName;
    private String description;

    // Static attributes
    private CabType cabType;
    private ShareType shareType;
    private Boolean hasAirportLicense;
    private ShiftType shiftType;

    // Metadata
    private String category;
    private String colorCode;
    private Integer displayOrder;

    // Dynamic attributes
    private List<DynamicAttributeRequest> dynamicAttributes;

    /**
     * Dynamic attribute request
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @ToString
    public static class DynamicAttributeRequest {
        private Long attributeTypeId;
        private Boolean isRequired;
        private String expectedValue;
    }
}
