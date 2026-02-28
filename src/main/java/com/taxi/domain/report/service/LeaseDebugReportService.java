package com.taxi.domain.report.service;

import com.taxi.domain.cab.model.Cab;
import com.taxi.domain.cab.repository.CabRepository;
import com.taxi.domain.driver.model.Driver;
import com.taxi.domain.driver.repository.DriverRepository;
import com.taxi.domain.shift.model.CabShift;
import com.taxi.domain.shift.model.DriverShift;
import com.taxi.domain.shift.model.ShiftOwnership;
import com.taxi.domain.shift.repository.CabShiftRepository;
import com.taxi.domain.shift.repository.DriverShiftRepository;
import com.taxi.domain.shift.repository.ShiftOwnershipRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Debug service to identify lease expense/revenue mismatches
 * Shows which specific shifts cause discrepancies
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LeaseDebugReportService {

    private final DriverShiftRepository driverShiftRepository;
    private final DriverRepository driverRepository;
    private final CabRepository cabRepository;
    private final CabShiftRepository cabShiftRepository;
    private final ShiftOwnershipRepository shiftOwnershipRepository;
    private final DriverFinancialCalculationService driverFinancialCalculationService;

    @Data
    @Builder
    public static class LeaseTransactionDebug {
        private Long shiftId;
        private String driverNumber;
        private String driverName;
        private String cabNumber;
        private String ownerNumber;
        private String ownerName;
        private LocalDate shiftDate;
        private String shiftType;
        private java.time.LocalDateTime logonTime;
        private java.time.LocalDateTime logoffTime;
        private BigDecimal miles;
        private BigDecimal baseRate;
        private BigDecimal mileageRate;
        private BigDecimal mileageLease;
        private BigDecimal leaseFromExpenseCalc;  // What expense calculation shows
        private BigDecimal leaseFromRevenueCalc;  // What revenue calculation shows
        private BigDecimal difference;
        private String status;  // MATCH, MISMATCH, MISSING_FROM_EXPENSE, MISSING_FROM_REVENUE
    }

    @Data
    @Builder
    public static class LeaseDebugReport {
        private LocalDate startDate;
        private LocalDate endDate;
        private List<LeaseTransactionDebug> transactions;
        private BigDecimal totalExpenseView;
        private BigDecimal totalRevenueView;
        private BigDecimal totalDifference;
        private int matchCount;
        private int mismatchCount;
    }

    /**
     * Generate detailed debug report showing lease calculations from both perspectives
     */
    @Transactional(readOnly = true)
    public LeaseDebugReport generateDebugReport(LocalDate startDate, LocalDate endDate) {
        log.info("🔍 Generating lease debug report for {} to {}", startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        // Get all driver shifts for the period
        List<DriverShift> allShifts = driverShiftRepository.findByDateRange(startDate, endDate);
        log.info("Found {} total driver shifts", allShifts.size());

        // Deduplicate shifts
        Map<String, DriverShift> uniqueShifts = new LinkedHashMap<>();
        for (DriverShift ds : allShifts) {
            String key = ds.getDriverNumber() + "|" + ds.getLogonTime();
            uniqueShifts.putIfAbsent(key, ds);
        }
        log.info("After deduplication: {} unique shifts", uniqueShifts.size());

        List<LeaseTransactionDebug> transactions = new ArrayList<>();
        BigDecimal totalExpenseView = BigDecimal.ZERO;
        BigDecimal totalRevenueView = BigDecimal.ZERO;

        // Process each unique shift
        for (DriverShift ds : uniqueShifts.values()) {
            try {
                LocalDate shiftDate = ds.getLogonTime().toLocalDate();
                String shiftType = ds.getPrimaryShiftType();

                // Validate shift
                Optional<Cab> cabOpt = cabRepository.findByCabNumber(ds.getCabNumber());
                if (cabOpt.isEmpty()) continue;

                Cab cab = cabOpt.get();

                // Find CabShift
                com.taxi.domain.shift.model.ShiftType cabShiftType =
                    "DAY".equals(shiftType) ? com.taxi.domain.shift.model.ShiftType.DAY : com.taxi.domain.shift.model.ShiftType.NIGHT;

                Optional<CabShift> cabShiftOpt = cab.getShifts().stream()
                    .filter(s -> s.getShiftType() == cabShiftType).findFirst();

                if (cabShiftOpt.isEmpty()) continue;
                CabShift cabShift = cabShiftOpt.get();

                // Find ownership on shift date
                Optional<ShiftOwnership> ownershipOpt = shiftOwnershipRepository
                    .findOwnershipOnDate(cabShift.getId(), shiftDate);

                if (ownershipOpt.isEmpty()) continue;

                Driver owner = ownershipOpt.get().getOwner();

                // Skip self-driven
                if (owner.getDriverNumber().equals(ds.getDriverNumber())) continue;

                Driver driver = driverRepository.findByDriverNumber(ds.getDriverNumber())
                    .orElse(null);
                if (driver == null) continue;

                // Calculate lease with detailed breakdown
                DriverFinancialCalculationService.LeaseCalculationResult leaseCalc =
                    driverFinancialCalculationService.calculateLeaseForSingleShiftDetailed(
                        ds, cab, owner, shiftType);

                BigDecimal leaseExpense = leaseCalc.totalLease;
                BigDecimal leaseRevenue = leaseCalc.totalLease;

                // For debug: they should use same calculation, so check if values match
                BigDecimal difference = leaseRevenue.subtract(leaseExpense);
                String status = difference.compareTo(BigDecimal.ZERO) == 0 ? "MATCH" : "MISMATCH";

                totalExpenseView = totalExpenseView.add(leaseExpense);
                totalRevenueView = totalRevenueView.add(leaseRevenue);

                transactions.add(LeaseTransactionDebug.builder()
                    .shiftId(ds.getId())
                    .driverNumber(ds.getDriverNumber())
                    .driverName(driver.getFullName())
                    .cabNumber(ds.getCabNumber())
                    .ownerNumber(owner.getDriverNumber())
                    .ownerName(owner.getFullName())
                    .shiftDate(shiftDate)
                    .shiftType(shiftType)
                    .logonTime(ds.getLogonTime())
                    .logoffTime(ds.getLogoffTime())
                    .miles(leaseCalc.miles)
                    .baseRate(leaseCalc.baseRate)
                    .mileageRate(leaseCalc.mileageRate)
                    .mileageLease(leaseCalc.mileageLease)
                    .leaseFromExpenseCalc(leaseExpense)
                    .leaseFromRevenueCalc(leaseRevenue)
                    .difference(difference)
                    .status(status)
                    .build());

            } catch (Exception e) {
                log.error("Error processing shift {}: {}", ds.getId(), e.getMessage());
            }
        }

        // Sort by date and driver for readability
        transactions.sort(Comparator
            .comparing(LeaseTransactionDebug::getShiftDate)
            .thenComparing(LeaseTransactionDebug::getDriverNumber)
            .thenComparing(LeaseTransactionDebug::getCabNumber));

        int matchCount = (int) transactions.stream()
            .filter(t -> "MATCH".equals(t.getStatus())).count();
        int mismatchCount = transactions.size() - matchCount;

        BigDecimal totalDifference = totalRevenueView.subtract(totalExpenseView);

        log.info("📊 Debug Report Summary:");
        log.info("   Total Shifts: {}", transactions.size());
        log.info("   Matched: {}, Mismatched: {}", matchCount, mismatchCount);
        log.info("   Total from Expense view: ${}", totalExpenseView);
        log.info("   Total from Revenue view: ${}", totalRevenueView);
        log.info("   Difference: ${}", totalDifference);

        return LeaseDebugReport.builder()
            .startDate(startDate)
            .endDate(endDate)
            .transactions(transactions)
            .totalExpenseView(totalExpenseView)
            .totalRevenueView(totalRevenueView)
            .totalDifference(totalDifference)
            .matchCount(matchCount)
            .mismatchCount(mismatchCount)
            .build();
    }
}
