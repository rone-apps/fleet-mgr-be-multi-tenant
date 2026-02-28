package com.taxi.domain.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaseReconciliationReportDTO {
    private LocalDate startDate;
    private LocalDate endDate;
    private List<LeaseReconciliationRowDTO> rows;

    // Summary statistics
    private BigDecimal totalLeaseAmount;
    private int totalShifts;
    private int matchedCount;
    private int noOwnerCount;
    private int selfDrivenCount;
    private int cabNotFoundCount;
}
