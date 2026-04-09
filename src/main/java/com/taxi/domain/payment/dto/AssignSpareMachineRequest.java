package com.taxi.domain.payment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignSpareMachineRequest {
    @NotNull(message = "Real cab number is required")
    private Integer realCabNumber;

    private String shift;  // "Day", "Night", or "BOTH"

    private LocalDateTime assignedAt;  // Optional, defaults to now

    private String notes;
}
