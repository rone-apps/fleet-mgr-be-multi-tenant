package com.taxi.web.dto.cab.attribute;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignAttributeRequest {

    @NotNull(message = "Attribute type ID is required")
    private Long attributeTypeId;

    @Size(max = 255)
    private String attributeValue;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    private LocalDate endDate;

    @Size(max = 500)
    private String notes;
}
