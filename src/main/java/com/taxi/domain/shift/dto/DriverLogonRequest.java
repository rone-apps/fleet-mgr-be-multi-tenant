package com.taxi.domain.shift.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for logging on a driver
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverLogonRequest {
    @NotBlank(message = "Driver number is required")
    private String driverNumber;  // Changed from Long driverId
    
    @NotBlank(message = "Cab number is required")
    private String cabNumber;
    
    private Long createdBy;
}