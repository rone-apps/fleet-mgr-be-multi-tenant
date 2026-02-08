package com.taxi.web.dto.shift;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

/**
 * Request DTO for activating a shift
 *
 * Used to activate a shift and create a new status history record.
 * The effective date determines when the activation becomes effective.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivateShiftRequest {

    /**
     * The date when this activation becomes effective
     * Cannot be null and should be today or a future date
     */
    @NotNull(message = "Effective from date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate effectiveFrom;

    /**
     * Optional reason for activating the shift
     * Useful for audit trail and tracking why shifts are being activated
     * Examples:
     * - "Returning from maintenance"
     * - "New shift assignment"
     * - "Seasonal activation"
     */
    private String reason;
}
