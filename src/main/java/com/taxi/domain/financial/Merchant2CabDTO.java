package com.taxi.domain.financial;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Merchant2CabDTO {
    
    private Long id;
    
    @NotBlank(message = "Cab number is required")
    private String cabNumber;
    
    @NotBlank(message = "Merchant number is required")
    private String merchantNumber;
    
    @NotNull(message = "Start date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
    
    private String notes;
    
    private boolean active;
    
    // Cab details for display
    private String make;
    private String model;
    private Integer year;
    private String color;
    private String registrationNumber;
    private String cabType;
    private String cabStatus;
    
    // Owner driver details
    private Long ownerDriverId;
    private String ownerDriverName;
    private String ownerDriverLicenseNumber;
}
