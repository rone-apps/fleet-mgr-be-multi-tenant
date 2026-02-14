package com.taxi.web.dto.expense;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverPerformanceDTO {

    private Long driverId;
    private String driverName;
    private String driverNumber;
    private Boolean isOwner;

    private BigDecimal totalRevenues;
    private BigDecimal totalExpenses;
    private BigDecimal netAmount;

    private BigDecimal profitMargin;  // Percentage (0-100)
}
