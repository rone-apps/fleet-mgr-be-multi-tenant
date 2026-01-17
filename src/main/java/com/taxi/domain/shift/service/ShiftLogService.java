package com.taxi.domain.shift.service;

import com.taxi.domain.expense.model.ShiftExpense;
import com.taxi.domain.lease.service.LeaseCalculationService;
import com.taxi.domain.revenue.model.Revenue;
import com.taxi.domain.shift.model.CabShift;
import com.taxi.domain.shift.model.DriverSegment;
import com.taxi.domain.shift.model.ShiftLog;
import com.taxi.domain.shift.repository.ShiftLogRepository;
import com.taxi.domain.cab.model.Cab;
import com.taxi.domain.driver.model.Driver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Domain service for managing shift logs and operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShiftLogService {

    private final ShiftLogRepository shiftLogRepository;
    private final LeaseCalculationService leaseCalculationService;
    private final ShiftOwnershipService ownershipService;

    /**
     * Start a new shift - create initial shift log
     * 
     * @param cab The cab being operated
     * @param shift The shift being operated
     * @param driver The driver starting the shift
     * @param logDate The date of the shift
     * @param startMeterReading Starting odometer reading
     * @return The created ShiftLog
     */
    @Transactional
    public ShiftLog startShift(Cab cab, CabShift shift, Driver driver, LocalDate logDate, BigDecimal startMeterReading) {
        log.info("Starting shift - Cab: {}, Shift: {}, Driver: {}, Date: {}", 
            cab.getCabNumber(), shift.getShiftType(), driver.getDriverNumber(), logDate);

        // Check if shift log already exists for this date
        if (shiftLogRepository.findByCabAndShiftAndLogDate(cab, shift, logDate).isPresent()) {
            throw new ShiftLogException("Shift log already exists for this cab, shift, and date");
        }

        // Get the owner of the shift on this date
        Driver owner = ownershipService.getOwnerOnDate(shift, logDate);

        // Create the shift log
        ShiftLog shiftLog = ShiftLog.builder()
            .cab(cab)
            .shift(shift)
            .owner(owner)
            .logDate(logDate)
            .startMeterReading(startMeterReading)
            .settlementStatus(ShiftLog.SettlementStatus.PENDING)
            .build();

        shiftLog = shiftLogRepository.save(shiftLog);

        // Create first driver segment
        DriverSegment segment = DriverSegment.builder()
            .shiftLog(shiftLog)
            .driver(driver)
            .sequenceNumber(1)
            .startTime(LocalDateTime.now())
            .startMeterReading(startMeterReading)
            .build();

        shiftLog.addSegment(segment);
        shiftLog = shiftLogRepository.save(shiftLog);

        log.info("Shift started successfully - ShiftLog ID: {}", shiftLog.getId());
        return shiftLog;
    }

    /**
     * Handle driver handover - end current segment and start new one
     */
    @Transactional
    public ShiftLog addDriverHandover(ShiftLog shiftLog, Driver newDriver, BigDecimal meterReading) {
        log.info("Processing driver handover - ShiftLog: {}, New Driver: {}", 
            shiftLog.getId(), newDriver.getDriverNumber());

        // End current active segment
        DriverSegment currentSegment = shiftLog.getSegments().stream()
            .filter(DriverSegment::isActive)
            .findFirst()
            .orElseThrow(() -> new ShiftLogException("No active segment found for handover"));

        currentSegment.endSegment(LocalDateTime.now(), meterReading);

        // Create new segment
        int nextSequence = shiftLog.getSegments().size() + 1;
        DriverSegment newSegment = DriverSegment.builder()
            .shiftLog(shiftLog)
            .driver(newDriver)
            .sequenceNumber(nextSequence)
            .startTime(LocalDateTime.now())
            .startMeterReading(meterReading)
            .build();

        shiftLog.addSegment(newSegment);
        shiftLog = shiftLogRepository.save(shiftLog);

        log.info("Driver handover completed - Previous: {}, New: {}", 
            currentSegment.getDriverName(), newSegment.getDriverName());
        return shiftLog;
    }

    /**
     * Add revenue to a shift log
     */
    @Transactional
    public ShiftLog addRevenue(ShiftLog shiftLog, Revenue revenue) {
        log.debug("Adding revenue to ShiftLog {} - Type: {}, Amount: {}", 
            shiftLog.getId(), revenue.getRevenueType(), revenue.getAmount());

        shiftLog.addRevenue(revenue);
        return shiftLogRepository.save(shiftLog);
    }

    /**
     * Add expense to a shift log
     */
    @Transactional
    public ShiftLog addExpense(ShiftLog shiftLog, ShiftExpense expense) {
        log.debug("Adding expense to ShiftLog {} - Type: {}, Amount: {}", 
            shiftLog.getId(), expense.getExpenseType(), expense.getAmount());

        shiftLog.addExpense(expense);
        return shiftLogRepository.save(shiftLog);
    }

    /**
     * Complete shift and calculate all financials
     */
    @Transactional
    public ShiftLog completeShift(ShiftLog shiftLog, BigDecimal endMeterReading) {
        log.info("Completing shift - ShiftLog: {}, End Meter: {}", shiftLog.getId(), endMeterReading);

        // End the last active segment
        DriverSegment lastSegment = shiftLog.getSegments().stream()
            .filter(DriverSegment::isActive)
            .findFirst()
            .orElseThrow(() -> new ShiftLogException("No active segment found to complete"));

        lastSegment.endSegment(LocalDateTime.now(), endMeterReading);

        // Set end meter reading on shift log
        shiftLog.setEndMeterReading(endMeterReading);
        shiftLog.calculateTotalMiles();

        // Calculate lease (if applicable)
        boolean isOwnerDriving = shiftLog.getSegments().size() == 1 
            && shiftLog.getSegments().get(0).getDriver().getId().equals(shiftLog.getOwner().getId());

        if (!isOwnerDriving) {
            // Calculate lease since it's not owner driving own shift
            LeaseCalculationService.LeaseAmount leaseAmount = leaseCalculationService.calculateLease(
                shiftLog.getCab(), 
                shiftLog.getShift(), 
                shiftLog.getLogDate(), 
                shiftLog.getTotalMiles()
            );

            // Set lease calculation on shift log
            ShiftLog.LeaseCalculation leaseCalc = ShiftLog.LeaseCalculation.builder()
                .baseRate(leaseAmount.getBaseRate())
                .mileageRate(leaseAmount.getMileageRate())
                .mileageCharge(leaseAmount.getMileageCharge())
                .totalLeaseAmount(leaseAmount.getTotalLeaseAmount())
                .leasePlanSnapshot(leaseAmount.getLeasePlanName())
                .build();

            shiftLog.setLeaseCalculation(leaseCalc);

            // Calculate proportional lease for each segment
            final BigDecimal totalMiles = shiftLog.getTotalMiles();
            final BigDecimal totalLeaseAmount = leaseAmount.getTotalLeaseAmount();
            
            shiftLog.getSegments().forEach(segment -> {
                segment.calculateSegmentMiles();
                segment.calculateLeaseShare(totalMiles, totalLeaseAmount);
            });
        } else {
            // Owner driving own shift - zero lease
            ShiftLog.LeaseCalculation zeroLease = ShiftLog.LeaseCalculation.builder()
                .baseRate(BigDecimal.ZERO)
                .mileageRate(BigDecimal.ZERO)
                .mileageCharge(BigDecimal.ZERO)
                .totalLeaseAmount(BigDecimal.ZERO)
                .leasePlanSnapshot("Owner Self-Drive (No Lease)")
                .build();
            shiftLog.setLeaseCalculation(zeroLease);
        }

        // Calculate financial summary
        shiftLog.completeAndSettle();

        shiftLog = shiftLogRepository.save(shiftLog);

        log.info("Shift completed - Total Miles: {}, Total Revenue: {}, Total Expenses: {}, Lease: {}", 
            shiftLog.getTotalMiles(),
            shiftLog.getFinancialSummary().getTotalRevenue(),
            shiftLog.getFinancialSummary().getTotalExpenses(),
            shiftLog.getLeaseCalculation().getTotalLeaseAmount());

        return shiftLog;
    }

    /**
     * Mark shift as disputed
     */
    @Transactional
    public ShiftLog markAsDisputed(ShiftLog shiftLog, String reason) {
        log.warn("Marking ShiftLog {} as disputed - Reason: {}", shiftLog.getId(), reason);
        shiftLog.markAsDisputed(reason);
        return shiftLogRepository.save(shiftLog);
    }

    /**
     * Get shift log by ID
     */
    @Transactional(readOnly = true)
    public ShiftLog getShiftLog(Long id) {
        return shiftLogRepository.findById(id)
            .orElseThrow(() -> new ShiftLogException("ShiftLog not found with ID: " + id));
    }

    /**
     * Get shift logs for a date range
     */
    @Transactional(readOnly = true)
    public List<ShiftLog> getShiftLogs(Long cabId, LocalDate startDate, LocalDate endDate) {
        return shiftLogRepository.findByCabIdAndDateRange(cabId, startDate, endDate);
    }

    /**
     * Get shift logs for an owner in a date range
     */
    @Transactional(readOnly = true)
    public List<ShiftLog> getOwnerShiftLogs(Long ownerId, LocalDate startDate, LocalDate endDate) {
        return shiftLogRepository.findByOwnerIdAndDateRange(ownerId, startDate, endDate);
    }

    /**
     * Get shift logs where a driver operated
     */
    @Transactional(readOnly = true)
    public List<ShiftLog> getDriverShiftLogs(Long driverId, LocalDate startDate, LocalDate endDate) {
        return shiftLogRepository.findByDriverIdAndDateRange(driverId, startDate, endDate);
    }

    /**
     * Get all unsettled shifts
     */
    @Transactional(readOnly = true)
    public List<ShiftLog> getUnsettledShifts() {
        return shiftLogRepository.findUnsettled();
    }

    /**
     * Get all disputed shifts
     */
    @Transactional(readOnly = true)
    public List<ShiftLog> getDisputedShifts() {
        return shiftLogRepository.findDisputed();
    }

    /**
     * Exception for shift log operations
     */
    public static class ShiftLogException extends RuntimeException {
        public ShiftLogException(String message) {
            super(message);
        }

        public ShiftLogException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
