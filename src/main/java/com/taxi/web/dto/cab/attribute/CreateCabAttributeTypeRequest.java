package com.taxi.web.dto.cab.attribute;

import com.taxi.domain.cab.model.CabAttributeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCabAttributeTypeRequest {

    @NotBlank(message = "Attribute code is required")
    @Size(max = 50)
    private String attributeCode;

    @NotBlank(message = "Attribute name is required")
    @Size(max = 100)
    private String attributeName;

    @Size(max = 500)
    private String description;

    @NotBlank(message = "Category is required")
    private String category;

    @NotBlank(message = "Data type is required")
    private String dataType;

    private Boolean requiresValue;

    @Size(max = 255)
    private String validationPattern;

    @Size(max = 500)
    private String helpText;

    public CabAttributeType toEntity() {
        return CabAttributeType.builder()
                .attributeCode(attributeCode.toUpperCase())
                .attributeName(attributeName)
                .description(description)
                .category(CabAttributeType.AttributeCategory.valueOf(category.toUpperCase()))
                .dataType(CabAttributeType.DataType.valueOf(dataType.toUpperCase()))
                .requiresValue(requiresValue != null ? requiresValue : false)
                .validationPattern(validationPattern)
                .helpText(helpText)
                .isActive(true)
                .build();
    }
}
