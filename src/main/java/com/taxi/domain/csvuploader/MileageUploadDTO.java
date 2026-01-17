package com.taxi.domain.csvuploader;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MileageUploadDTO {
    
    private String cabNumber;
    private String driverNumber;
    private LocalDateTime logonTime;
    private LocalDateTime logoffTime;
    
    private BigDecimal mileageA;
    private BigDecimal mileageB;
    private BigDecimal mileageC;
    private BigDecimal totalMileage;
    private BigDecimal shiftHours;
    
    private boolean valid = true;
    private String validationMessage;
    
    private boolean cabLookupSuccess;
    private String cabLookupMessage;
    
    private boolean driverLookupSuccess;
    private String driverLookupMessage;
    
    private String rawLogonTime;
    private String rawLogoffTime;
    
    private int rowNumber;
}
