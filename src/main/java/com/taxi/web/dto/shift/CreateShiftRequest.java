package com.taxi.web.dto.shift;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for creating a new cab shift
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateShiftRequest {

    @NotNull(message = "Cab ID is required")
    private Long cabId;

    @NotBlank(message = "Shift type is required")
    private String shiftType;  // DAY or NIGHT

    @NotBlank(message = "Start time is required")
    private String startTime;  // e.g., "06:00"

    @NotBlank(message = "End time is required")
    private String endTime;    // e.g., "18:00"

    @NotNull(message = "Owner ID is required")
    private Long ownerId;

    // Financial information for initial ownership
    private String acquisitionType;  // PURCHASE, INITIAL_ASSIGNMENT, etc.
    private BigDecimal acquisitionPrice;
    
    private String notes;
}
