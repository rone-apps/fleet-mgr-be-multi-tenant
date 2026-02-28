package com.taxi.domain.report.service;

import com.taxi.domain.cab.model.Cab;
import com.taxi.domain.cab.repository.CabRepository;
import com.taxi.domain.driver.model.Driver;
import com.taxi.domain.driver.repository.DriverRepository;
import com.taxi.domain.report.dto.LeaseReconciliationReportDTO;
import com.taxi.domain.report.dto.LeaseReconciliationRowDTO;
import com.taxi.domain.shift.model.CabShift;
import com.taxi.domain.shift.model.DriverShift;
import com.taxi.domain.shift.model.ShiftOwnership;
import com.taxi.domain.shift.repository.CabShiftRepository;
import com.taxi.domain.shift.repository.DriverShiftRepository;
import com.taxi.domain.shift.repository.ShiftOwnershipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Service to generate lease reconciliation report.
 * Shows shift-by-shift breakdown of driver lease expenses vs. owner lease revenues.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LeaseReconciliationService {

    private final DriverShiftRepository driverShiftRepository;
    private final ShiftOwnershipRepository shiftOwnershipRepository;
    private final DriverRepository driverRepository;
    private final CabRepository cabRepository;
    private final CabShiftRepository cabShiftRepository;
    private final DriverFinancialCalculationService driverFinancialCalculationService;

    /**
     * Generate lease reconciliation report for a date range.
     * Shows each driver shift with corresponding cab owner and lease amount.
     */
    @Transactional(readOnly = true)
    public LeaseReconciliationReportDTO generateReport(LocalDate startDate, LocalDate endDate) {
        log.info("Generating lease reconciliation report for {} to {}", startDate, endDate);

        List<DriverShift> allShifts = driverShiftRepository.findByDateRange(startDate, endDate);
        log.info("Found {} driver shifts in range", allShifts.size());

        // ✅ DEDUPLICATION: Prevent duplicate shifts from being counted multiple times
        // This matches the deduplication in lease revenue/expense calculations for consistency
        java.util.Map<String, DriverShift> uniqueShifts = new java.util.LinkedHashMap<>();
        for (DriverShift ds : allShifts) {
            String key = ds.getDriverNumber() + "|" + ds.getLogonTime();
            uniqueShifts.putIfAbsent(key, ds);
        }

        log.info("After deduplication: {} unique driver shifts", uniqueShifts.size());

        List<LeaseReconciliationRowDTO> rows = new ArrayList<>();

        for (DriverShift ds : uniqueShifts.values()) {
            LocalDate shiftDate = ds.getLogonTime().toLocalDate();
            String shiftType = ds.getPrimaryShiftType(); // "DAY" or "NIGHT"

            try {
                // Resolve driver name
                String driverName = driverRepository.findByDriverNumber(ds.getDriverNumber())
                    .map(d -> d.getFirstName() + " " + d.getLastName())
                    .orElse("Unknown");

                // Find cab + CabShift (DAY/NIGHT) → resolve owner on that date
                Optional<Cab> cabOpt = cabRepository.findByCabNumber(ds.getCabNumber());
                if (cabOpt.isEmpty()) {
                    log.warn("Cab {} not found for shift {}", ds.getCabNumber(), ds.getId());
                    rows.add(buildRow(ds, shiftDate, shiftType, driverName, null, null, BigDecimal.ZERO, "CAB_NOT_FOUND"));
                    continue;
                }
                Cab cab = cabOpt.get();

                // Find the appropriate CabShift (DAY or NIGHT)
                com.taxi.domain.shift.model.ShiftType cabShiftType =
                    "DAY".equals(shiftType) ? com.taxi.domain.shift.model.ShiftType.DAY : com.taxi.domain.shift.model.ShiftType.NIGHT;

                Optional<CabShift> cabShiftOpt = cab.getShifts().stream()
                    .filter(s -> s.getShiftType() == cabShiftType).findFirst();

                if (cabShiftOpt.isEmpty()) {
                    log.warn("CabShift {} not found for cab {}", cabShiftType, ds.getCabNumber());
                    rows.add(buildRow(ds, shiftDate, shiftType, driverName, null, null, BigDecimal.ZERO, "CAB_NOT_FOUND"));
                    continue;
                }
                CabShift cabShift = cabShiftOpt.get();

                // Find ownership on the shift date
                Optional<ShiftOwnership> ownershipOpt = shiftOwnershipRepository
                    .findOwnershipOnDate(cabShift.getId(), shiftDate);

                if (ownershipOpt.isEmpty()) {
                    log.debug("No owner found for cab shift {} on date {}", cabShift.getId(), shiftDate);
                    rows.add(buildRow(ds, shiftDate, shiftType, driverName, null, null, BigDecimal.ZERO, "NO_OWNER"));
                    continue;
                }

                Driver owner = ownershipOpt.get().getOwner();

                // Check if driver is the owner (self-driven, no lease)
                if (owner.getDriverNumber().equals(ds.getDriverNumber())) {
                    log.debug("Driver {} owns cab {}, no lease required", ds.getDriverNumber(), ds.getCabNumber());
                    rows.add(buildRow(ds, shiftDate, shiftType, driverName, owner.getDriverNumber(),
                        owner.getFirstName() + " " + owner.getLastName(), BigDecimal.ZERO, "SELF_DRIVEN"));
                    continue;
                }

                // ✅ CRITICAL FIX: Calculate FULL lease amount (baseRate + mileage), not just base rate
                // This ensures lease reconciliation matches driver summary calculations
                BigDecimal totalLease = driverFinancialCalculationService.calculateLeaseForSingleShift(
                    ds, cab, owner, shiftType);

                rows.add(buildRow(ds, shiftDate, shiftType, driverName, owner.getDriverNumber(),
                    owner.getFirstName() + " " + owner.getLastName(), totalLease, "MATCHED"));

            } catch (Exception e) {
                log.error("Error processing shift {}: {}", ds.getId(), e.getMessage(), e);
            }
        }

        // Sort by cabNumber, shiftDate, shiftType
        rows.sort(Comparator.comparing(LeaseReconciliationRowDTO::getCabNumber)
            .thenComparing(LeaseReconciliationRowDTO::getShiftDate)
            .thenComparing(LeaseReconciliationRowDTO::getShiftType));

        // Compute totals
        BigDecimal totalLease = rows.stream()
            .map(r -> r.getLeaseAmount() != null ? r.getLeaseAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        int matchedCount = (int) rows.stream().filter(r -> "MATCHED".equals(r.getStatus())).count();
        int noOwnerCount = (int) rows.stream().filter(r -> "NO_OWNER".equals(r.getStatus())).count();
        int selfDrivenCount = (int) rows.stream().filter(r -> "SELF_DRIVEN".equals(r.getStatus())).count();
        int cabNotFoundCount = (int) rows.stream().filter(r -> "CAB_NOT_FOUND".equals(r.getStatus())).count();

        log.info("Lease reconciliation complete: {} matched, {} no_owner, {} self_driven, {} cab_not_found",
            matchedCount, noOwnerCount, selfDrivenCount, cabNotFoundCount);

        return LeaseReconciliationReportDTO.builder()
            .startDate(startDate)
            .endDate(endDate)
            .rows(rows)
            .totalLeaseAmount(totalLease)
            .totalShifts(allShifts.size())
            .matchedCount(matchedCount)
            .noOwnerCount(noOwnerCount)
            .selfDrivenCount(selfDrivenCount)
            .cabNotFoundCount(cabNotFoundCount)
            .build();
    }

    private LeaseReconciliationRowDTO buildRow(DriverShift ds, LocalDate shiftDate, String shiftType,
                                               String driverName, String ownerNumber, String ownerName,
                                               BigDecimal leaseAmount, String status) {
        return LeaseReconciliationRowDTO.builder()
            .driverShiftId(ds.getId())
            .cabNumber(ds.getCabNumber())
            .shiftDate(shiftDate)
            .shiftType(shiftType)
            .driverNumber(ds.getDriverNumber())
            .driverName(driverName)
            .ownerNumber(ownerNumber)
            .ownerName(ownerName)
            .leaseAmount(leaseAmount)
            .status(status)
            .build();
    }
}
