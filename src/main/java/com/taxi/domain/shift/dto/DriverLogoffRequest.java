package com.taxi.domain.shift.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request DTO for logging off a driver
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverLogoffRequest {
    private Integer totalTrips;
    private BigDecimal totalRevenue;
    private BigDecimal totalDistance;
    private String notes;
    private Long updatedBy;
}

