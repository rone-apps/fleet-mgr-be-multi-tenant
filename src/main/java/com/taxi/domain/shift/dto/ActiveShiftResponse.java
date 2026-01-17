package com.taxi.domain.shift.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for active shift display
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActiveShiftResponse {
    private Long id;
    private String driverNumber;
    private String driverName;
    private String cabNumber;
    private LocalDateTime logonTime;
    private String primaryShiftType;
    private Long hoursSoFar;
    private Integer totalTrips;
    private BigDecimal totalRevenue;
    private BigDecimal totalDistance;
}


















































































































