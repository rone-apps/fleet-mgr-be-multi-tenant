package com.taxi.domain.shift.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for shift operations
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShiftResponse {
    private Long id;
    private String driverNumber;
    private String driverName;
    private String cabNumber;
    private LocalDateTime logonTime;
    private LocalDateTime logoffTime;
    private BigDecimal totalHours;
    private String primaryShiftType;
    private BigDecimal primaryShiftCount;
    private String secondaryShiftType;
    private BigDecimal secondaryShiftCount;
    private BigDecimal dayShifts;
    private BigDecimal nightShifts;
    private String status;
    private Integer totalTrips;
    private BigDecimal totalRevenue;
    private BigDecimal totalDistance;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}