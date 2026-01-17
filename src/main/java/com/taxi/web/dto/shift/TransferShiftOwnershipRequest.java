package com.taxi.web.dto.shift;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for transferring shift ownership
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferShiftOwnershipRequest {

    @NotNull(message = "New owner ID is required")
    private Long newOwnerId;

    private LocalDate transferDate;  // Defaults to today if not provided
    
    private String acquisitionType;  // PURCHASE, TRANSFER, etc.
    private BigDecimal salePrice;  // Sale price from current owner
    private BigDecimal acquisitionPrice;  // Purchase price for new owner
    
    private String notes;
}
