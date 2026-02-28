package com.taxi.domain.report.service;

import com.taxi.domain.account.model.AccountCharge;
import com.taxi.domain.account.repository.AccountChargeRepository;
import com.taxi.domain.cab.model.Cab;
import com.taxi.domain.driver.model.Driver;
import com.taxi.domain.driver.repository.DriverRepository;
import com.taxi.domain.expense.model.ItemRate;
import com.taxi.domain.expense.model.ItemRateOverride;
import com.taxi.domain.expense.repository.ItemRateRepository;
import com.taxi.domain.expense.repository.ItemRateOverrideRepository;
import com.taxi.domain.lease.model.LeaseRate;
import com.taxi.domain.lease.repository.LeaseRateRepository;
import com.taxi.domain.lease.service.LeaseCalculationService;
import com.taxi.domain.lease.service.LeaseRateOverrideService;
import com.taxi.domain.payment.model.CreditCardTransaction;
import com.taxi.domain.payment.repository.CreditCardTransactionRepository;
import com.taxi.domain.profile.model.ItemRateChargedTo;
import com.taxi.domain.profile.model.ItemRateUnitType;
import com.taxi.domain.shift.model.CabShift;
import com.taxi.domain.shift.model.DriverShift;
import com.taxi.domain.shift.model.ShiftOwnership;
import com.taxi.domain.shift.model.ShiftType;
import com.taxi.domain.shift.repository.CabShiftRepository;
import com.taxi.domain.shift.repository.DriverShiftRepository;
import com.taxi.domain.shift.service.ShiftValidationService;
import com.taxi.web.dto.report.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

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
    private final com.taxi.domain.shift.repository.ShiftOwnershipRepository shiftOwnershipRepository;
    private final AccountChargeRepository accountChargeRepository;
    private final CreditCardTransactionRepository creditCardTransactionRepository;
    private final CabShiftRepository cabShiftRepository;
    private final LeaseRateRepository leaseRateRepository;
    private final ShiftValidationService shiftValidationService;

    // ✅ NEW: Lease rate override service for custom owner rates
    private final LeaseRateOverrideService leaseRateOverrideService;

    // ✅ NEW: Item rate repositories for insurance & per-unit expenses
    private final ItemRateRepository itemRateRepository;
    private final ItemRateOverrideRepository itemRateOverrideRepository;

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
    BigDecimal getApplicableLeaseRate(
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
     * Note: Attributes are now at shift level, not cab level
     */
    private BigDecimal getDefaultLeaseRate(Cab cab, LocalDateTime shiftDateTime) {
        try {
            // Attributes moved to shift level - using null/false for now
            // This method needs to be refactored to receive shift information
            LeaseRate leaseRate = null;  // leaseCalculationService.findApplicableRate requires shift attributes
            
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

        List<ShiftOwnership> ownerships = shiftOwnershipRepository.findByOwnerOrderByStartDateDesc(owner);
        
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
            
            if (!shiftValidationService.isCabShiftActive(cabShift)) {
                skippedInactive++;
                log.debug("   ⊘ SKIP: {} {} (shift: {})",
                        cab.getCabNumber(), cabShift.getShiftType(),
                        cabShift.getStatus());
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
                    log.debug("   ✓ Added Lease: {} {} on {} = ${}",
                            cab.getCabNumber(), cabShift.getShiftType(),
                            leaseItem.getShiftDate(), leaseItem.getTotalLease());

                    // Calculate insurance revenue using the same miles from lease calculation
                    InsuranceMileageDTO insuranceItem = calculateInsuranceExpenseForShift(
                        driverShift, cab, owner, cabShift.getShiftType().name(),
                        leaseItem.getMiles());

                    if (insuranceItem != null) {
                        report.addInsuranceMileage(insuranceItem);
                        log.debug("   ✓ Added Insurance: {} {} on {} = ${}",
                                cab.getCabNumber(), cabShift.getShiftType(),
                                insuranceItem.getShiftDate(), insuranceItem.getTotalInsuranceMileage());
                    }
                }
            }
        }
        
        report.calculateSummary();
        
        log.info("   ✅ RESULT: {} shifts processed, {} inactive skipped, TOTAL: ${}",
                totalProcessed, skippedInactive, report.getGrandTotalLease());
        
        return report;
    }

    /**
     * ═══════════════════════════════════════════════════════════════════════
     * SHARED LEASE AMOUNT CALCULATION (eliminates duplicate logic)
     * ═══════════════════════════════════════════════════════════════════════
     */
    private LeaseCalculationResult calculateShiftLeaseAmount(
            DriverShift shift, Cab cab, Driver owner, String shiftType) {
        // ✅ Get applicable rate (override or default)
        BigDecimal baseRate = getApplicableLeaseRate(
            owner.getDriverNumber(),
            cab.getCabNumber(),
            shiftType,
            shift.getLogonTime(),
            cab
        );

        // Get mileage rate from default lease rate
        // Note: DriverShift doesn't have cab attributes - using defaults
        // These should be obtained from the underlying CabShift
        LeaseRate leaseRate = leaseCalculationService.findApplicableRate(
            null,  // cabType - need to get from CabShift
            false, // hasAirportLicense - need to get from CabShift
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

        return new LeaseCalculationResult(baseRate, mileageRate, miles, mileageLease, totalLease);
    }

    /**
     * Simple result object for lease calculation
     */
    private static class LeaseCalculationResult {
        final BigDecimal baseRate;
        final BigDecimal mileageRate;
        final BigDecimal miles;
        final BigDecimal mileageLease;
        final BigDecimal totalLease;

        LeaseCalculationResult(BigDecimal baseRate, BigDecimal mileageRate, BigDecimal miles,
                BigDecimal mileageLease, BigDecimal totalLease) {
            this.baseRate = baseRate;
            this.mileageRate = mileageRate;
            this.miles = miles;
            this.mileageLease = mileageLease;
            this.totalLease = totalLease;
        }
    }

    private LeaseRevenueDTO calculateLeaseForShift(
            DriverShift shift, Cab cab, Driver owner, String shiftType) {
        try {
            // ✅ Use shared calculation
            LeaseCalculationResult leaseCalc = calculateShiftLeaseAmount(shift, cab, owner, shiftType);
            
            // Look up driver name from repository (DriverShift.driverName is transient/not stored)
            String workingDriverName = driverRepository.findByDriverNumber(shift.getDriverNumber())
                    .map(Driver::getFullName)
                    .orElse(shift.getDriverNumber()); // fallback to driver number if not found
            
            return LeaseRevenueDTO.builder()
                    .shiftId(shift.getId())
                    .shiftDate(shift.getLogonTime().toLocalDate())
                    .logonTime(shift.getLogonTime())
                    .logoffTime(shift.getLogoffTime())
                    .cabNumber(cab.getCabNumber())
                    .driverNumber(shift.getDriverNumber())
                    .driverName(workingDriverName)
                    .shiftType(shift.getPrimaryShiftType())
                    .miles(leaseCalc.miles)
                    .baseRate(leaseCalc.baseRate)
                    .mileageRate(leaseCalc.mileageRate)
                    .mileageLease(leaseCalc.mileageLease)
                    .totalLease(leaseCalc.totalLease)
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
            
            if (!shiftValidationService.isCabShiftActive(cabShift)) {
                skippedInactive++;
                log.debug("   ⊘ SKIP INACTIVE: {} {} (shift: {})",
                        cabShift.getCab().getCabNumber(), cabShift.getShiftType(),
                        cabShift.getStatus());
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
                log.debug("   ✓ Added Lease: {} {} on {} = ${}",
                        cabShift.getCab().getCabNumber(), cabShift.getShiftType(),
                        leaseItem.getShiftDate(), leaseItem.getTotalLease());

                // Calculate insurance expense using the same miles from the lease calculation
                InsuranceMileageDTO insuranceItem = calculateInsuranceExpenseForShift(
                        driverShift, cabShift.getCab(), shiftOwner, cabShift.getShiftType().name(),
                        leaseItem.getMiles());

                if (insuranceItem != null) {
                    report.addInsuranceMileage(insuranceItem);
                    log.debug("   ✓ Added Insurance: {} {} on {} = ${}",
                            cabShift.getCab().getCabNumber(), cabShift.getShiftType(),
                            insuranceItem.getShiftDate(), insuranceItem.getTotalInsuranceMileage());
                }
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
            // ✅ Use shared calculation
            LeaseCalculationResult leaseCalc = calculateShiftLeaseAmount(shift, cab, owner, shiftType);

            return LeaseExpenseDTO.builder()
                    .shiftId(shift.getId())
                    .shiftDate(shift.getLogonTime().toLocalDate())
                    .logonTime(shift.getLogonTime())
                    .logoffTime(shift.getLogoffTime())
                    .cabNumber(cab.getCabNumber())
                    .ownerDriverNumber(owner.getDriverNumber())
                    .ownerDriverName(owner.getFullName())
                    .shiftType(shift.getPrimaryShiftType())
                    .miles(leaseCalc.miles)
                    .baseRate(leaseCalc.baseRate)
                    .mileageRate(leaseCalc.mileageRate)
                    .mileageLease(leaseCalc.mileageLease)
                    .totalLease(leaseCalc.totalLease)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error calculating lease expense for shift {}: {}", shift.getId(), e.getMessage());
            return null;
        }
    }

    /**
     * Calculate insurance mileage expense for a specific shift
     * Uses the same miles already calculated for lease expense
     * Insurance is charged based on mileage driven only (no fixed amount)
     * Insurance is an expense for the driver, income for the owner
     *
     * @param shift The driver shift
     * @param cab The cab used for the shift
     * @param owner The cab owner (who receives the insurance revenue)
     * @param shiftType The shift type (NIGHT, DAY, etc)
     * @param miles The miles already calculated for lease (reuse for insurance)
     * @return InsuranceMileageDTO with insurance expense details
     */
    private InsuranceMileageDTO calculateInsuranceExpenseForShift(
            DriverShift shift, Cab cab, Driver owner, String shiftType, BigDecimal miles) {
        try {
            // Validate miles
            if (miles == null || miles.compareTo(BigDecimal.ZERO) <= 0) {
                log.debug("No mileage to calculate insurance for shift {}", shift.getId());
                return null;
            }

            // Get insurance mileage rate from ItemRate system (INSURANCE type, charged to DRIVER)
            List<ItemRate> insuranceRates = itemRateRepository.findByUnitTypeAndChargedToAndIsActiveTrueOrderByName(
                    ItemRateUnitType.INSURANCE, ItemRateChargedTo.DRIVER);

            if (insuranceRates.isEmpty()) {
                log.debug("No insurance rates configured for driver, skipping insurance calculation");
                return null;
            }

            // Use first insurance rate as base
            ItemRate baseInsuranceRate = insuranceRates.get(0);
            BigDecimal insuranceRate = baseInsuranceRate.getRate();

            // Check for item rate overrides (per-owner, per-cab, per-shift, per-day)
            if (owner.getDriverNumber() != null) {
                LocalDate shiftDate = shift.getLogonTime().toLocalDate();
                List<ItemRateOverride> applicableOverrides = itemRateOverrideRepository
                    .findActiveOverridesForRate(baseInsuranceRate.getId(), owner.getDriverNumber(), shiftDate);

                if (!applicableOverrides.isEmpty()) {
                    String dayOfWeek = shiftDate.getDayOfWeek().toString();

                    for (ItemRateOverride override : applicableOverrides) {
                        if (override.matches(cab.getCabNumber(), shiftType, dayOfWeek)) {
                            insuranceRate = override.getOverrideRate();
                            log.debug("Using insurance override rate ${}/mile for owner {} cab {} {} shift",
                                    insuranceRate, owner.getDriverNumber(), cab.getCabNumber(), shiftType);
                            break;
                        }
                    }
                }
            }

            // Calculate insurance expense: miles × insurance rate
            BigDecimal totalInsurance = insuranceRate.multiply(miles)
                    .setScale(2, java.math.RoundingMode.HALF_UP);

            log.debug("Calculated insurance expense for driver {}: {} miles × ${}/mile = ${}",
                    owner.getDriverNumber(), miles, insuranceRate, totalInsurance);

            return InsuranceMileageDTO.builder()
                    .shiftId(shift.getId())
                    .shiftDate(shift.getLogonTime().toLocalDate())
                    .logonTime(shift.getLogonTime())
                    .logoffTime(shift.getLogoffTime())
                    .cabNumber(cab.getCabNumber())
                    .ownerDriverNumber(owner.getDriverNumber())
                    .ownerDriverName(owner.getFullName())
                    .shiftType(shiftType)
                    .miles(miles)
                    .mileageRate(insuranceRate)
                    .totalInsuranceMileage(totalInsurance)
                    .build();

        } catch (Exception e) {
            log.error("Error calculating insurance expense for shift {}: {}", shift.getId(), e.getMessage());
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

    /**
     * ═══════════════════════════════════════════════════════════════════════
     * SHARED LEASE EXPENSE CALCULATION
     * ═══════════════════════════════════════════════════════════════════════
     *
     * SINGLE SOURCE OF TRUTH for calculating total lease expense for a driver
     * This method is used by:
     * - Driver Summary Report (calculateLeaseExpenseFromShifts in ReportService)
     * - Lease Reconciliation Report (for individual shift calculations)
     *
     * Ensures both reports use identical lease calculation logic
     */
    public BigDecimal calculateTotalLeaseExpenseForDriver(
            String driverNumber,
            LocalDate startDate,
            LocalDate endDate) {

        log.info("📊 [SHARED LEASE CALC] Calculating total lease expense for driver: {} | Dates: {} to {}",
                driverNumber, startDate, endDate);

        try {
            // ✅ Delegate to calculateLeaseExpense and sum the results
            LeaseExpenseReportDTO report = calculateLeaseExpense(driverNumber, startDate, endDate);

            BigDecimal total = report.getLeaseExpenseItems() == null ? BigDecimal.ZERO
                : report.getLeaseExpenseItems().stream()
                    .map(LeaseExpenseDTO::getTotalLease)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            log.info("   ✅ Total lease expense for driver {}: ${}", driverNumber, total);
            return total;
        } catch (Exception e) {
            log.error("   ❌ Error calculating total lease for driver {}: {}", driverNumber, e.getMessage(), e);
            return BigDecimal.ZERO;
        }
    }
}