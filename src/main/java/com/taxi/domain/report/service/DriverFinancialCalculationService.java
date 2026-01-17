package com.taxi.domain.report.service;

import com.taxi.domain.account.model.AccountCharge;
import com.taxi.domain.account.repository.AccountChargeRepository;
import com.taxi.domain.cab.model.Cab;
import com.taxi.domain.driver.model.Driver;
import com.taxi.domain.driver.repository.DriverRepository;
import com.taxi.domain.lease.model.LeaseRate;
import com.taxi.domain.lease.repository.LeaseRateRepository;
import com.taxi.domain.lease.service.LeaseCalculationService;
import com.taxi.domain.lease.service.LeaseRateOverrideService;
import com.taxi.domain.payment.model.CreditCardTransaction;
import com.taxi.domain.payment.repository.CreditCardTransactionRepository;
import com.taxi.domain.shift.model.CabShift;
import com.taxi.domain.shift.model.DriverShift;
import com.taxi.domain.shift.model.ShiftOwnership;
import com.taxi.domain.shift.model.ShiftType;
import com.taxi.domain.shift.repository.CabShiftRepository;
import com.taxi.domain.shift.repository.DriverShiftRepository;
import com.taxi.domain.shift.service.ShiftOwnershipService;
import com.taxi.web.dto.report.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * SINGLE SOURCE OF TRUTH FOR ALL DRIVER FINANCIAL CALCULATIONS
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * ENHANCED VERSION WITH:
 * - Detailed logging for debugging discrepancies
 * - Lease rate override support for custom owner rates
 * 
 * This service contains ALL financial calculation logic used by:
 * 
 * 1. INDIVIDUAL DRIVER REPORTS (detailed with line items)
 * 2. DRIVER SUMMARY REPORT (all active drivers with totals)
 * 
 * BUSINESS RULE: INACTIVE CAB/SHIFT FILTERING
 * ✅ Expenses tied to INACTIVE shifts are excluded
 * ✅ Expenses tied to RETIRED or MAINTENANCE cabs are excluded
 * ✅ Only ACTIVE cabs with ACTIVE shifts generate revenue/expense
 * 
 * LEASE RATE LOGIC:
 * ✅ Checks for custom owner override first
 * ✅ Falls back to default lease rate if no override
 * ✅ Logs which rate type was used for transparency
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DriverFinancialCalculationService {

    private final DriverRepository driverRepository;
    private final DriverShiftRepository driverShiftRepository;
    private final LeaseCalculationService leaseCalculationService;
    private final ShiftOwnershipService shiftOwnershipService;
    private final AccountChargeRepository accountChargeRepository;
    private final CreditCardTransactionRepository creditCardTransactionRepository;
    private final CabShiftRepository cabShiftRepository;
    private final FixedExpenseReportService fixedExpenseReportService;
    private final LeaseRateRepository leaseRateRepository;
    
    // ✅ NEW: Lease rate override service for custom owner rates
    private final LeaseRateOverrideService leaseRateOverrideService;

    /**
     * ═══════════════════════════════════════════════════════════════════════
     * BUSINESS RULE: CAB AND SHIFT STATUS VALIDATION
     * ═══════════════════════════════════════════════════════════════════════
     */
    
    private boolean isCabActive(Cab cab) {
        if (cab == null) return false;
        return cab.getStatus() == Cab.CabStatus.ACTIVE;
    }
    
    private boolean isShiftActive(CabShift shift) {
        if (shift == null) return false;
        return shift.getStatus() == CabShift.ShiftStatus.ACTIVE;
    }
    
    private boolean isCabShiftActive(CabShift cabShift) {
        if (cabShift == null) return false;
        return isCabActive(cabShift.getCab()) && isShiftActive(cabShift);
    }

    /**
     * ═══════════════════════════════════════════════════════════════════════
     * LEASE RATE CALCULATION - WITH OVERRIDE SUPPORT
     * ═══════════════════════════════════════════════════════════════════════
     * 
     * This method determines the applicable lease rate for a shift.
     * 
     * LOGIC:
     * 1. Check for owner's custom override (specific to cab/shift/day)
     * 2. If override exists, use it
     * 3. If no override, fall back to default lease rate from lease_rates table
     * 
     * This allows owners to customize rates while maintaining backwards
     * compatibility with existing default rate system.
     */
    private BigDecimal getApplicableLeaseRate(
            String ownerDriverNumber,
            String cabNumber,
            String shiftType,
            LocalDateTime shiftDateTime,
            Cab cab) {
        
        LocalDate shiftDate = shiftDateTime.toLocalDate();
        
        // ✅ STEP 1: Check for custom override FIRST
        BigDecimal overrideRate = leaseRateOverrideService.getApplicableLeaseRate(
            ownerDriverNumber, 
            cabNumber, 
            shiftType, 
            shiftDate
        );
        
        if (overrideRate != null) {
            log.debug("   💰 Using CUSTOM override rate: ${} for owner={}, cab={}, shift={}, date={}", 
                overrideRate, ownerDriverNumber, cabNumber, shiftType, shiftDate);
            return overrideRate;
        }
        
        // ✅ STEP 2: No override - use default rate
        log.debug("   💰 No override found, using DEFAULT rate for cab={}, shift={}", 
            cabNumber, shiftType);
        return getDefaultLeaseRate(cab, shiftDateTime);
    }
    
    /**
     * Get default lease rate from lease_rates table
     * This is the existing logic - no changes to default rate calculation
     */
    private BigDecimal getDefaultLeaseRate(Cab cab, LocalDateTime shiftDateTime) {
        try {
            LeaseRate leaseRate = leaseCalculationService.findApplicableRate(
                cab.getCabType(),
                cab.getHasAirportLicense() != null ? cab.getHasAirportLicense() : false,
                shiftDateTime
            );
            
            if (leaseRate == null) {
                log.warn("   ⚠️ No default lease rate found, using fallback $50");
                return new BigDecimal("50.00"); // Fallback rate
            }
            
            BigDecimal baseRate = leaseRate.getBaseRate();
            log.debug("   💰 Using DEFAULT base rate: ${}", baseRate);
            return baseRate;
            
        } catch (Exception e) {
            log.error("   ❌ Error getting default lease rate: {}", e.getMessage());
            return new BigDecimal("50.00"); // Fallback on error
        }
    }

    /**
     * ═══════════════════════════════════════════════════════════════════════
     * LEASE REVENUE CALCULATION
     * ═══════════════════════════════════════════════════════════════════════
     */
    @Transactional(readOnly = true)
    public LeaseRevenueReportDTO calculateLeaseRevenue(
            String ownerDriverNumber,
            LocalDate startDate,
            LocalDate endDate) {
        
        log.info("📊 [LEASE REVENUE] Driver: {} | Dates: {} to {}", 
                ownerDriverNumber, startDate, endDate);
        
        Driver owner = driverRepository.findByDriverNumber(ownerDriverNumber)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Driver not found: " + ownerDriverNumber));
        
        List<ShiftOwnership> ownerships = shiftOwnershipService.getCurrentOwnerships(owner);
        
        log.debug("   ✓ Driver owns {} shifts", ownerships.size());
        
        LeaseRevenueReportDTO report = LeaseRevenueReportDTO.builder()
                .ownerDriverNumber(ownerDriverNumber)
                .ownerDriverName(owner.getFullName())
                .startDate(startDate)
                .endDate(endDate)
                .build();
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        
        int skippedInactive = 0;
        int totalProcessed = 0;
        
        for (ShiftOwnership ownership : ownerships) {
            CabShift cabShift = ownership.getShift();
            Cab cab = cabShift.getCab();
            
            if (!isCabShiftActive(cabShift)) {
                skippedInactive++;
                log.debug("   ⊘ SKIP: {} {} (cab: {}, shift: {})",
                        cab.getCabNumber(), cabShift.getShiftType(),
                        cab.getStatus(), cabShift.getStatus());
                continue;
            }
            
            List<DriverShift> driverShifts = driverShiftRepository
                    .findByCabNumberAndLogonTimeBetween(
                            cab.getCabNumber(), startDateTime, endDateTime);
            
            for (DriverShift driverShift : driverShifts) {
                if (driverShift.getDriverNumber().equals(ownerDriverNumber)) {
                    continue; // Owner drove own shift
                }
                
                if (!"COMPLETED".equals(driverShift.getStatus())) {
                    continue;
                }
                
                if (!cabShift.getShiftType().name().equals(driverShift.getPrimaryShiftType())) {
                    continue;
                }
                
                LeaseRevenueDTO leaseItem = calculateLeaseForShift(
                    driverShift, cab, owner, cabShift.getShiftType().name());
                
                if (leaseItem != null) {
                    totalProcessed++;
                    report.addLeaseItem(leaseItem);
                    log.debug("   ✓ Added: {} {} on {} = ${}",
                            cab.getCabNumber(), cabShift.getShiftType(),
                            leaseItem.getShiftDate(), leaseItem.getTotalLease());
                }
            }
        }
        
        report.calculateSummary();
        
        log.info("   ✅ RESULT: {} shifts processed, {} inactive skipped, TOTAL: ${}",
                totalProcessed, skippedInactive, report.getGrandTotalLease());
        
        return report;
    }

    private LeaseRevenueDTO calculateLeaseForShift(
            DriverShift shift, Cab cab, Driver owner, String shiftType) {
        try {
            // ✅ NEW: Get applicable rate (override or default)
            BigDecimal baseRate = getApplicableLeaseRate(
                owner.getDriverNumber(),
                cab.getCabNumber(),
                shiftType,
                shift.getLogonTime(),
                cab
            );
            
            // Get mileage rate from default lease rate
            LeaseRate leaseRate = leaseCalculationService.findApplicableRate(
                cab.getCabType(),
                cab.getHasAirportLicense() != null ? cab.getHasAirportLicense() : false,
                shift.getLogonTime()
            );
            
            BigDecimal mileageRate = (leaseRate != null) ? 
                leaseRate.getMileageRate() : BigDecimal.ZERO;
            
            BigDecimal miles = shift.getTotalDistance();
            if (miles == null || miles.compareTo(BigDecimal.ZERO) == 0) {
                miles = BigDecimal.TEN;
            }
            
            BigDecimal mileageLease = mileageRate.multiply(miles);
            BigDecimal totalLease = baseRate.add(mileageLease);
            
            // Look up driver name from repository (DriverShift.driverName is transient/not stored)
            String workingDriverName = driverRepository.findByDriverNumber(shift.getDriverNumber())
                    .map(Driver::getFullName)
                    .orElse(shift.getDriverNumber()); // fallback to driver number if not found
            
            return LeaseRevenueDTO.builder()
                    .shiftDate(shift.getLogonTime().toLocalDate())
                    .cabNumber(cab.getCabNumber())
                    .driverNumber(shift.getDriverNumber())
                    .driverName(workingDriverName)
                    .shiftType(shift.getPrimaryShiftType())
                    .miles(miles)
                    .baseRate(baseRate)
                    .mileageRate(mileageRate)
                    .mileageLease(mileageLease)
                    .totalLease(totalLease)
                    .build();
                    
        } catch (Exception e) {
            log.error("   ❌ Error calculating lease for shift {}: {}", shift.getId(), e.getMessage());
            return null;
        }
    }

    /**
     * ═══════════════════════════════════════════════════════════════════════
     * CREDIT CARD REVENUE CALCULATION
     * ═══════════════════════════════════════════════════════════════════════
     */
    @Transactional(readOnly = true)
    public CreditCardRevenueReportDTO calculateCreditCardRevenue(
            String driverNumber,
            LocalDate startDate,
            LocalDate endDate) {
        
        log.info("📊 [CC REVENUE] Driver: {} | Dates: {} to {}", 
                driverNumber, startDate, endDate);
        
        Driver driver = driverRepository.findByDriverNumber(driverNumber)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found: " + driverNumber));
        
        List<CreditCardTransaction> transactions = creditCardTransactionRepository
                .findByDriverNumberAndDateRange(driverNumber, startDate, endDate);
        
        log.debug("   ✓ Found {} transactions", transactions.size());
        
        CreditCardRevenueReportDTO report = CreditCardRevenueReportDTO.builder()
                .driverNumber(driverNumber)
                .driverName(driver.getFullName())
                .startDate(startDate)
                .endDate(endDate)
                .build();
        
        BigDecimal runningTotal = BigDecimal.ZERO;
        
        for (CreditCardTransaction transaction : transactions) {
            BigDecimal amount = transaction.getAmount() != null ? transaction.getAmount() : BigDecimal.ZERO;
            BigDecimal tipAmount = transaction.getTipAmount() != null ? transaction.getTipAmount() : BigDecimal.ZERO;
            BigDecimal totalAmount = transaction.getTotalAmount();
            
            runningTotal = runningTotal.add(totalAmount);
            
            CreditCardRevenueDTO item = CreditCardRevenueDTO.builder()
                    .transactionDate(transaction.getTransactionDate())
                    .transactionTime(transaction.getTransactionTime())
                    .cabNumber(transaction.getCabNumber())
                    .cardType(transaction.getCardType())
                    .cardLastFour(transaction.getCardLastFour())
                    .amount(amount)
                    .tipAmount(tipAmount)
                    .totalAmount(totalAmount)
                    .processingFee(transaction.getProcessingFee())
                    .isSettled(transaction.getIsSettled())
                    .transactionStatus(transaction.getTransactionStatus().name())
                    .build();
            report.addTransactionItem(item);
        }
        
        report.calculateSummary();
        
        log.info("   ✅ RESULT: {} transactions, Running Total: ${}, Calculated Total: ${}",
                transactions.size(), runningTotal, report.getGrandTotal());
        
        // VALIDATION: Check if running total matches calculated total
        if (runningTotal.compareTo(report.getGrandTotal()) != 0) {
            log.error("   ❌ DISCREPANCY! Running: ${}, Calculated: ${}", 
                    runningTotal, report.getGrandTotal());
        }
        
        return report;
    }

    /**
     * ═══════════════════════════════════════════════════════════════════════
     * ACCOUNT CHARGES REVENUE CALCULATION
     * ═══════════════════════════════════════════════════════════════════════
     */
    @Transactional(readOnly = true)
    public ChargesRevenueReportDTO calculateChargesRevenue(
            String driverNumber,
            LocalDate startDate,
            LocalDate endDate) {
        
        log.info("📊 [CHARGES REVENUE] Driver: {} | Dates: {} to {}", 
                driverNumber, startDate, endDate);
        
        Driver driver = driverRepository.findByDriverNumber(driverNumber)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found: " + driverNumber));
        
        List<AccountCharge> charges = accountChargeRepository
                .findByDriverNumberAndDateRange(driverNumber, startDate, endDate);
        
        log.debug("   ✓ Found {} charges", charges.size());
        
        ChargesRevenueReportDTO report = ChargesRevenueReportDTO.builder()
                .driverNumber(driverNumber)
                .driverName(driver.getFullName())
                .startDate(startDate)
                .endDate(endDate)
                .build();
        
        BigDecimal runningTotal = BigDecimal.ZERO;
        
        for (AccountCharge charge : charges) {
            BigDecimal fareAmount = charge.getFareAmount() != null ? charge.getFareAmount() : BigDecimal.ZERO;
            BigDecimal tipAmount = charge.getTipAmount() != null ? charge.getTipAmount() : BigDecimal.ZERO;
            BigDecimal totalAmount = fareAmount.add(tipAmount);
            
            runningTotal = runningTotal.add(totalAmount);
            
            ChargesRevenueDTO item = ChargesRevenueDTO.builder()
                    .chargeId(charge.getId())
                    .tripDate(charge.getTripDate())
                    .startTime(charge.getStartTime())
                    .endTime(charge.getEndTime())
                    .customerName(charge.getAccountCustomer() != null ? 
                            charge.getAccountCustomer().getCompanyName() : null)
                    .accountId(charge.getAccountId())
                    .subAccount(charge.getSubAccount())
                    .jobCode(charge.getJobCode())
                    .pickupAddress(charge.getPickupAddress())
                    .dropoffAddress(charge.getDropoffAddress())
                    .passengerName(charge.getPassengerName())
                    .cabNumber(charge.getCab() != null ? charge.getCab().getCabNumber() : null)
                    .driverNumber(charge.getDriver() != null ? charge.getDriver().getDriverNumber() : driverNumber)
                    .driverName(charge.getDriver() != null ? charge.getDriver().getFullName() : driver.getFullName())
                    .fareAmount(fareAmount)
                    .tipAmount(tipAmount)
                    .totalAmount(totalAmount)
                    .isPaid(charge.isPaid())
                    .paidDate(charge.getPaidDate())
                    .invoiceNumber(charge.getInvoiceNumber())
                    .notes(charge.getNotes())
                    .build();
            report.addChargeItem(item);
        }
        
        report.calculateSummary();
        
        log.info("   ✅ RESULT: {} charges, Running Total: ${}, Calculated Total: ${}",
                charges.size(), runningTotal, report.getGrandTotal());
        
        // VALIDATION: Check if running total matches calculated total
        if (runningTotal.compareTo(report.getGrandTotal()) != 0) {
            log.error("   ❌ DISCREPANCY! Running: ${}, Calculated: ${}", 
                    runningTotal, report.getGrandTotal());
        }
        
        return report;
    }

    /**
     * ═══════════════════════════════════════════════════════════════════════
     * FIXED EXPENSES CALCULATION
     * ═══════════════════════════════════════════════════════════════════════
     */
    @Transactional(readOnly = true)
    public FixedExpenseReportDTO calculateFixedExpenses(
            String driverNumber,
            LocalDate startDate,
            LocalDate endDate) {
        
        log.info("📊 [FIXED EXPENSES] Driver: {} | Dates: {} to {}", 
                driverNumber, startDate, endDate);
        
        FixedExpenseReportDTO report = fixedExpenseReportService.generateFixedExpenseReport(
                driverNumber, startDate, endDate);
        
        log.info("   ✅ RESULT: {} expenses, TOTAL: ${}", 
                report.getTotalExpenses(), report.getTotalAmount());
        
        return report;
    }

    /**
     * ═══════════════════════════════════════════════════════════════════════
     * LEASE EXPENSE CALCULATION
     * ═══════════════════════════════════════════════════════════════════════
     */
    @Transactional(readOnly = true)
    public LeaseExpenseReportDTO calculateLeaseExpense(
            String driverNumber,
            LocalDate startDate,
            LocalDate endDate) {
        
        log.info("📊 [LEASE EXPENSE] Driver: {} | Dates: {} to {}", 
                driverNumber, startDate, endDate);
        
        Driver driver = driverRepository.findByDriverNumber(driverNumber)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found: " + driverNumber));
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        
        List<DriverShift> driverShifts = driverShiftRepository
                .findByDriverNumberAndLogonTimeBetween(driverNumber, startDateTime, endDateTime);
        
        log.debug("   ✓ Found {} driver shifts", driverShifts.size());
        
        LeaseExpenseReportDTO report = LeaseExpenseReportDTO.builder()
                .workingDriverNumber(driverNumber)
                .workingDriverName(driver.getFullName())
                .startDate(startDate)
                .endDate(endDate)
                .build();
        
        int skippedInactive = 0;
        int skippedOwnShift = 0;
        int totalProcessed = 0;
        
        for (DriverShift driverShift : driverShifts) {
            if (!"COMPLETED".equals(driverShift.getStatus())) {
                continue;
            }
            
            CabShift cabShift = findCabShiftForDriverShift(driverShift);
            if (cabShift == null) {
                continue;
            }
            
            if (!isCabShiftActive(cabShift)) {
                skippedInactive++;
                log.debug("   ⊘ SKIP INACTIVE: {} {} (cab: {}, shift: {})",
                        cabShift.getCab().getCabNumber(), cabShift.getShiftType(),
                        cabShift.getCab().getStatus(), cabShift.getStatus());
                continue;
            }
            
            Driver shiftOwner = cabShift.getCurrentOwner();
            
            if (shiftOwner.getDriverNumber().equals(driverNumber)) {
                skippedOwnShift++;
                continue; // Driver drove own shift
            }
            
            LeaseExpenseDTO leaseItem = calculateLeaseExpenseForShift(
                    driverShift, cabShift.getCab(), shiftOwner, cabShift.getShiftType().name());
            
            if (leaseItem != null) {
                totalProcessed++;
                report.addLeaseExpense(leaseItem);
                log.debug("   ✓ Added: {} {} on {} = ${}",
                        cabShift.getCab().getCabNumber(), cabShift.getShiftType(),
                        leaseItem.getShiftDate(), leaseItem.getTotalLease());
            }
        }
        
        report.calculateSummary();
        
        log.info("   ✅ RESULT: {} shifts processed, {} own shifts, {} inactive skipped, TOTAL: ${}",
                totalProcessed, skippedOwnShift, skippedInactive, report.getGrandTotalLease());
        
        return report;
    }

    private LeaseExpenseDTO calculateLeaseExpenseForShift(
            DriverShift shift, Cab cab, Driver owner, String shiftType) {
        try {
            // ✅ NEW: Get applicable rate (override or default)
            BigDecimal baseRate = getApplicableLeaseRate(
                owner.getDriverNumber(),
                cab.getCabNumber(),
                shiftType,
                shift.getLogonTime(),
                cab
            );
            
            // Get mileage rate from default lease rate
            LeaseRate leaseRate = leaseCalculationService.findApplicableRate(
                cab.getCabType(),
                cab.getHasAirportLicense() != null ? cab.getHasAirportLicense() : false,
                shift.getLogonTime()
            );
            
            BigDecimal mileageRate = (leaseRate != null) ? 
                leaseRate.getMileageRate() : BigDecimal.ZERO;
            
            BigDecimal miles = shift.getTotalDistance();
            if (miles == null || miles.compareTo(BigDecimal.ZERO) == 0) {
                miles = BigDecimal.TEN;
            }
            
            BigDecimal mileageLease = mileageRate.multiply(miles);
            BigDecimal totalLease = baseRate.add(mileageLease);
            
            return LeaseExpenseDTO.builder()
                    .shiftDate(shift.getLogonTime().toLocalDate())
                    .cabNumber(cab.getCabNumber())
                    .ownerDriverNumber(owner.getDriverNumber())
                    .ownerDriverName(owner.getFullName())
                    .shiftType(shift.getPrimaryShiftType())
                    .miles(miles)
                    .baseRate(baseRate)
                    .mileageRate(mileageRate)
                    .mileageLease(mileageLease)
                    .totalLease(totalLease)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error calculating lease expense for shift {}: {}", shift.getId(), e.getMessage());
            return null;
        }
    }

    private CabShift findCabShiftForDriverShift(DriverShift driverShift) {
        try {
            String cabNumber = driverShift.getCabNumber();
            String shiftTypeStr = driverShift.getPrimaryShiftType();
            
            if (cabNumber == null || shiftTypeStr == null) {
                return null;
            }
            
            ShiftType shiftType = ShiftType.valueOf(shiftTypeStr);
            
            return cabShiftRepository.findByCabNumberAndShiftType(cabNumber, shiftType)
                    .orElse(null);
                    
        } catch (Exception e) {
            log.error("Error finding cab shift: {}", e.getMessage());
            return null;
        }
    }
}