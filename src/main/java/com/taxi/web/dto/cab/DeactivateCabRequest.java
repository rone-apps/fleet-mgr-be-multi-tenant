package com.taxi.web.dto.cab;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

/**
 * DTO for cab deactivation request
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeactivateCabRequest {

    @NotNull(message = "Deactivation date is required")
    private LocalDate deactivationDate;

    private String reason; // Optional - why the cab is being deactivated

    private String deactivatedBy; // Optional - who deactivated it (can derive from auth context)
}
