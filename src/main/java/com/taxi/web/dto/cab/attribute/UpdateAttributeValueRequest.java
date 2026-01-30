package com.taxi.web.dto.cab.attribute;

import jakarta.validation.constraints.Size;
import lombok.*;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateAttributeValueRequest {

    @Size(max = 255)
    private String attributeValue;

    private LocalDate startDate;

    private LocalDate endDate;

    @Size(max = 500)
    private String notes;
}
