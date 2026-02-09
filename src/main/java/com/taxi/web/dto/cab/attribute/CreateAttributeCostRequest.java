package com.taxi.web.dto.cab.attribute;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for creating attribute costs
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAttributeCostRequest {

    @NotNull(message = "Attribute type ID is required")
    private Long attributeTypeId;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.00", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;

    @NotNull(message = "Billing unit is required")
    @Pattern(regexp = "MONTHLY|DAILY", message = "Billing unit must be MONTHLY or DAILY")
    private String billingUnit;

    @NotNull(message = "Effective from date is required")
    @FutureOrPresent(message = "Effective from date cannot be in the past")
    private LocalDate effectiveFrom;

    @Future(message = "Effective to date must be in the future if provided")
    private LocalDate effectiveTo;
}
