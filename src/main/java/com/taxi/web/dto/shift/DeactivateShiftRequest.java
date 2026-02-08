package com.taxi.web.dto.shift;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

/**
 * Request DTO for deactivating a shift
 *
 * Used to deactivate a shift and create a new status history record.
 * The effective date determines when the deactivation becomes effective.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeactivateShiftRequest {

    /**
     * The date when this deactivation becomes effective
     * Cannot be null and should be today or a future date
     */
    @NotNull(message = "Effective from date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate effectiveFrom;

    /**
     * Optional reason for deactivating the shift
     * Useful for audit trail and tracking why shifts are being deactivated
     * Examples:
     * - "Sending for maintenance"
     * - "Seasonal closure"
     * - "Owner requested deactivation"
     * - "Vehicle retired"
     */
    private String reason;
}
