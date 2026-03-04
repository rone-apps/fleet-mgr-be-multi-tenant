package com.taxi.domain.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaseReconciliationRowDTO {
    private Long driverShiftId;
    private String cabNumber;
    private LocalDate shiftDate;
    private String shiftType;        // "DAY" or "NIGHT"

    // Driver side
    private String driverNumber;
    private String driverName;

    // Owner side (nullable if no owner found)
    private String ownerNumber;
    private String ownerName;

    // Lease breakdown
    private BigDecimal fixedLease;   // base rate from lease plan
    private BigDecimal mileageLease; // mileageRate × miles
    private BigDecimal leaseAmount;  // fixedLease + mileageLease (total)

    // Status
    private String status;           // MATCHED | NO_OWNER | SELF_DRIVEN | CAB_NOT_FOUND
}
