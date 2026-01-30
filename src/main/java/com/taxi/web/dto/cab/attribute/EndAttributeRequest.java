package com.taxi.web.dto.cab.attribute;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EndAttributeRequest {

    @NotNull(message = "End date is required")
    private LocalDate endDate;
}
