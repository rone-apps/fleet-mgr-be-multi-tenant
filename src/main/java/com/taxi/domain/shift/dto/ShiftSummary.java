package com.taxi.domain.shift.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for shift summary
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShiftSummary {
    private String driverNumber;
    private String driverName;
    private BigDecimal totalHours;
    private BigDecimal dayShifts;
    private BigDecimal nightShifts;
    private BigDecimal totalShifts;
    private Integer totalTrips;
    private BigDecimal totalRevenue;
    private BigDecimal totalDistance;
}