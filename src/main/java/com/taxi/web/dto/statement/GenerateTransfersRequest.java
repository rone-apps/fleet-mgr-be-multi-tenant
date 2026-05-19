package com.taxi.web.dto.statement;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO for generating transfer executions for a period
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateTransfersRequest {

    @NotNull(message = "Period from date is required")
    private LocalDate periodFrom;

    @NotNull(message = "Period to date is required")
    private LocalDate periodTo;
}
