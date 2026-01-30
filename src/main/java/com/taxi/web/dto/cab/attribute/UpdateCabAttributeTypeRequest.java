package com.taxi.web.dto.cab.attribute;

import com.taxi.domain.cab.model.CabAttributeType;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCabAttributeTypeRequest {

    @Size(max = 100)
    private String attributeName;

    @Size(max = 500)
    private String description;

    private String category;

    @Size(max = 255)
    private String validationPattern;

    @Size(max = 500)
    private String helpText;

    public CabAttributeType toEntity() {
        return CabAttributeType.builder()
                .attributeName(attributeName)
                .description(description)
                .category(category != null ? CabAttributeType.AttributeCategory.valueOf(category.toUpperCase()) : null)
                .validationPattern(validationPattern)
                .helpText(helpText)
                .build();
    }
}
