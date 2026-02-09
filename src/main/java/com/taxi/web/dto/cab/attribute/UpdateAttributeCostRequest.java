package com.taxi.web.dto.cab.attribute;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for updating attribute costs
 * Note: effectiveFrom cannot be changed (it's the temporal key)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAttributeCostRequest {

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.00", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;

    @NotNull(message = "Billing unit is required")
    @Pattern(regexp = "MONTHLY|DAILY", message = "Billing unit must be MONTHLY or DAILY")
    private String billingUnit;

    // effectiveTo can be null (meaning ongoing), or a future date
    private LocalDate effectiveTo;
}
