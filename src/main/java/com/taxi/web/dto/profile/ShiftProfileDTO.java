package com.taxi.web.dto.profile;

import com.taxi.domain.cab.model.CabType;
import com.taxi.domain.cab.model.ShareType;
import com.taxi.domain.profile.model.ShiftProfile;
import com.taxi.domain.shift.model.ShiftType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ShiftProfileDTO - Data Transfer Object for shift profiles
 * Used in API responses to send profile data to frontend
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = "id")
public class ShiftProfileDTO {

    private Long id;
    private String profileCode;
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

    // Status
    private Boolean isActive;
    private Boolean isSystemProfile;
    private Integer usageCount;

    // Dynamic attributes
    private List<ShiftProfileAttributeDTO> profileAttributes;

    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    /**
     * Convert entity to DTO
     */
    public static ShiftProfileDTO fromEntity(ShiftProfile entity) {
        if (entity == null) {
            return null;
        }

        List<ShiftProfileAttributeDTO> attributes = entity.getProfileAttributes() != null
                ? entity.getProfileAttributes().stream()
                .map(ShiftProfileAttributeDTO::fromEntity)
                .collect(Collectors.toList())
                : List.of();

        return ShiftProfileDTO.builder()
                .id(entity.getId())
                .profileCode(entity.getProfileCode())
                .profileName(entity.getProfileName())
                .description(entity.getDescription())
                .cabType(entity.getCabType())
                .shareType(entity.getShareType())
                .hasAirportLicense(entity.getHasAirportLicense())
                .shiftType(entity.getShiftType())
                .category(entity.getCategory())
                .colorCode(entity.getColorCode())
                .displayOrder(entity.getDisplayOrder())
                .isActive(entity.getIsActive())
                .isSystemProfile(entity.getIsSystemProfile())
                .usageCount(entity.getUsageCount())
                .profileAttributes(attributes)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }
}
