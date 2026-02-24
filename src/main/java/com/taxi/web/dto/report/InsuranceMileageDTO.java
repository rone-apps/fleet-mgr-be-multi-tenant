package com.taxi.web.dto.report;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for a single insurance mileage expense item
 * Represents mileage-based insurance cost for a shift
 * Mileage insurance is charged to the driver as an expense
 * and appears as income to the owner
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsuranceMileageDTO {

    // Shift identification
    private Long shiftId;
    private LocalDate shiftDate;
    private LocalDateTime logonTime;
    private LocalDateTime logoffTime;

    // Shift details
    private String cabNumber;
    private String shiftType;          // "DAY" or "NIGHT"

    // Owner information (who receives the insurance payment)
    private String ownerDriverNumber;
    private String ownerDriverName;

    // Insurance mileage calculation (only mileage component, no fixed amount)
    private BigDecimal miles;                    // Miles driven
    private BigDecimal mileageRate;              // Per-mile insurance rate
    private BigDecimal totalInsuranceMileage;    // miles × mileageRate (total amount)

    // Additional context
    private String cabType;            // SEDAN, VAN, etc.
    private Boolean hasAirportLicense;
}
