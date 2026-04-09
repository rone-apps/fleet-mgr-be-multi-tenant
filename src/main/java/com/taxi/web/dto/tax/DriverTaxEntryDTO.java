package com.taxi.web.dto.tax;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverTaxEntryDTO {
    private Long id;
    private Long driverId;
    private String driverName;
    private Integer taxYear;
    private String entryType;  // T_SLIP, RRSP, DONATION, OTHER_DEDUCTION
    private String slipType;   // T4, T4A, T4A-OAS, T5, T3, T4E, RL-1, RL-3, etc.
    private String boxLabel;   // e.g., "Box 14 - Employment Income"
    private String issuerName; // Employer, institution, charity name, etc.
    private BigDecimal amount;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getEntryTypeLabel() {
        return switch (entryType) {
            case "T_SLIP" -> "T Slip";
            case "RRSP" -> "RRSP Contribution";
            case "DONATION" -> "Charitable Donation";
            case "OTHER_DEDUCTION" -> "Other Deduction";
            default -> entryType;
        };
    }
}
