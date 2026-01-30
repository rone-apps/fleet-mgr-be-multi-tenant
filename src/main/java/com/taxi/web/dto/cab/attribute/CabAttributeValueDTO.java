package com.taxi.web.dto.cab.attribute;

import com.taxi.domain.cab.model.CabAttributeValue;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CabAttributeValueDTO {
    private Long id;
    private Long cabId;
    private String cabNumber;
    private Long attributeTypeId;
    private String attributeCode;
    private String attributeName;
    private String attributeTypeName;
    private String attributeValue;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean isCurrent;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    public static CabAttributeValueDTO fromEntity(CabAttributeValue entity) {
        if (entity == null) return null;

        return CabAttributeValueDTO.builder()
                .id(entity.getId())
                .cabId(entity.getCab() != null ? entity.getCab().getId() : null)
                .cabNumber(entity.getCab() != null ? entity.getCab().getCabNumber() : null)
                .attributeTypeId(entity.getAttributeType() != null ?
                    entity.getAttributeType().getId() : null)
                .attributeCode(entity.getAttributeType() != null ?
                    entity.getAttributeType().getAttributeCode() : null)
                .attributeName(entity.getAttributeType() != null ?
                    entity.getAttributeType().getAttributeName() : null)
                .attributeTypeName(entity.getAttributeType() != null ?
                    entity.getAttributeType().getAttributeName() : null)
                .attributeValue(entity.getAttributeValue())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .isCurrent(entity.isCurrent())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }
}
