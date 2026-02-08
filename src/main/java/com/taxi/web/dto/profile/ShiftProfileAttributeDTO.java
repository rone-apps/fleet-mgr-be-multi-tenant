package com.taxi.web.dto.profile;

import com.taxi.domain.profile.model.ShiftProfileAttribute;
import lombok.*;

/**
 * ShiftProfileAttributeDTO - DTO for profile attribute associations
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = "id")
public class ShiftProfileAttributeDTO {

    private Long id;
    private Long profileId;
    private Long attributeTypeId;
    private String attributeName;  // Denormalized from CabAttributeType
    private String attributeCode;  // Denormalized from CabAttributeType
    private Boolean isRequired;
    private String expectedValue;

    /**
     * Convert entity to DTO
     */
    public static ShiftProfileAttributeDTO fromEntity(ShiftProfileAttribute entity) {
        if (entity == null) {
            return null;
        }

        return ShiftProfileAttributeDTO.builder()
                .id(entity.getId())
                .profileId(entity.getProfile() != null ? entity.getProfile().getId() : null)
                .attributeTypeId(entity.getAttributeType() != null ? entity.getAttributeType().getId() : null)
                .attributeName(entity.getAttributeType() != null ? entity.getAttributeType().getAttributeName() : null)
                .attributeCode(entity.getAttributeType() != null ? entity.getAttributeType().getAttributeCode() : null)
                .isRequired(entity.getIsRequired())
                .expectedValue(entity.getExpectedValue())
                .build();
    }
}
