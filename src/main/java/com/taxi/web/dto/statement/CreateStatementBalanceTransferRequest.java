package com.taxi.web.dto.statement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for creating a new statement balance transfer
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateStatementBalanceTransferRequest {

    @NotNull(message = "Source person ID is required")
    private Long sourcePersonId;

    @NotNull(message = "Target person ID is required")
    private Long targetPersonId;

    @NotNull(message = "Transfer type is required")
    private String transferType; // ONE_TIME, RECURRING

    @NotNull(message = "Balance direction is required")
    private String balanceDirection; // POSITIVE_ONLY, BOTH

    @NotNull(message = "Transfer amount is required")
    @DecimalMin(value = "0.01", message = "Transfer amount must be greater than zero")
    private BigDecimal transferAmount;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    private LocalDate endDate;

    private LocalDate statementPeriodFrom;

    private LocalDate statementPeriodTo;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;

    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    private String reason;
}
