package com.taxi.web.dto.cab.attribute;

import com.taxi.domain.cab.model.CabAttributeType;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CabAttributeTypeDTO {
    private Long id;
    private String attributeCode;
    private String attributeName;
    private String description;
    private String category;
    private String dataType;
    private boolean requiresValue;
    private String validationPattern;
    private String helpText;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CabAttributeTypeDTO fromEntity(CabAttributeType entity) {
        if (entity == null) return null;

        return CabAttributeTypeDTO.builder()
                .id(entity.getId())
                .attributeCode(entity.getAttributeCode())
                .attributeName(entity.getAttributeName())
                .description(entity.getDescription())
                .category(entity.getCategory() != null ? entity.getCategory().name() : null)
                .dataType(entity.getDataType() != null ? entity.getDataType().name() : null)
                .requiresValue(entity.isRequiresValue())
                .validationPattern(entity.getValidationPattern())
                .helpText(entity.getHelpText())
                .isActive(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
