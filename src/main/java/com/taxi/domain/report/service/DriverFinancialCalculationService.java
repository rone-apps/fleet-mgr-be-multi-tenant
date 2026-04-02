package com.taxi.domain.report.service;

import com.taxi.domain.account.model.AccountCharge;
import com.taxi.domain.account.repository.AccountChargeRepository;
import com.taxi.domain.cab.model.Cab;
import com.taxi.domain.cab.model.CabAttributeType;
import com.taxi.domain.cab.model.CabAttributeValue;
import com.taxi.domain.cab.repository.CabAttributeTypeRepository;
import com.taxi.domain.cab.repository.CabAttributeValueRepository;
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
import com.taxi.domain.airport.model.AirportTrip;
import com.taxi.domain.airport.repository.AirportTripDriverRepository;
import com.taxi.domain.airport.repository.AirportTripRepository;
import com.taxi.domain.mileage.repository.MileageRecordRepository;
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
import java.util.Optional;

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

    // ✅ NEW: Airport charge service for calculating airport trip charges
    private final com.taxi.domain.airport.service.AirportChargeService airportChargeService;

    // ✅ NEW: Mileage record repository for looking up actual miles when DriverShift.TotalDistance is missing
    private final MileageRecordRepository mileageRecordRepository;

    // Attribute repositories for resolving airport trip rates per attribute
    private final CabAttributeTypeRepository cabAttributeTypeRepository;
    private final CabAttributeValueRepository cabAttributeValueRepository;

    // Pre-computed driver trip assignments
    private final AirportTripDriverRepository airportTripDriverRepository;

    // Direct airport trips table (fallback for legacy data without driver assignments)
    private final AirportTripRepository airportTripRepository;

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
            Cab cab,
            com.taxi.domain.cab.model.CabType cabType,
            boolean hasAirportLicense) {

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

        // ✅ STEP 2: No override - use default rate with CabShift attributes
        log.debug("   💰 No override found, using DEFAULT rate for cab={}, shift={}",
            cabNumber, shiftType);
        return getDefaultLeaseRate(cab, shiftDateTime, cabType, hasAirportLicense);
    }
    
    /**
     * Get default lease rate from lease_rates table
     * Uses CabShift attributes (cabType, hasAirportLicense) for correct rate lookup
     */
    private BigDecimal getDefaultLeaseRate(Cab cab, LocalDateTime shiftDateTime,
                                           com.taxi.domain.cab.model.CabType cabType, boolean hasAirportLicense) {
        try {
            LeaseRate leaseRate = leaseCalculationService.findApplicableRate(
                cabType, hasAirportLicense, shiftDateTime);

            if (leaseRate == null) {
                log.warn("   ⚠️ No default lease rate found for cabType={}, airport={}, using fallback $50",
                    cabType, hasAirportLicense);
                return new BigDecimal("50.00"); // Fallback rate
            }

            BigDecimal baseRate = leaseRate.getBaseRate();
            log.debug("   💰 Using DEFAULT base rate: ${} (cabType={}, airport={})", baseRate, cabType, hasAirportLicense);
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

        // ✅ FIX: Use DATE-AWARE query to find ownerships active during the report period
        // This ensures we only count lease revenue for shifts owned during the date range
        List<ShiftOwnership> ownerships = shiftOwnershipRepository.findOwnershipsInRange(
                owner.getId(), startDate, endDate);

        log.debug("   ✓ Driver owned {} shifts during period {} to {}",
                ownerships.size(), startDate, endDate);

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
        java.util.Set<Long> processedShiftIds = new java.util.HashSet<>();  // Track processed shifts

        for (ShiftOwnership ownership : ownerships) {
            CabShift cabShift = ownership.getShift();

            // ✅ FIX: Skip if we've already processed this shift (deduplicate)
            if (processedShiftIds.contains(cabShift.getId())) {
                log.debug("   ⊘ SKIP: Already processed shift {} ", cabShift.getId());
                continue;
            }
            processedShiftIds.add(cabShift.getId());

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

            // ✅ FIX: Deduplicate driver shifts by (driver, logonTime) to prevent adding duplicates
            java.util.Map<String, DriverShift> uniqueDriverShifts = new java.util.LinkedHashMap<>();
            for (DriverShift ds : driverShifts) {
                String key = ds.getDriverNumber() + "|" + ds.getLogonTime();
                uniqueDriverShifts.putIfAbsent(key, ds);
            }

            for (DriverShift driverShift : uniqueDriverShifts.values()) {
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
                    driverShift, cab, owner, cabShift.getShiftType().name(), cabShift);

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
        return calculateShiftLeaseAmount(shift, cab, owner, shiftType, null);
    }

    private LeaseCalculationResult calculateShiftLeaseAmount(
            DriverShift shift, Cab cab, Driver owner, String shiftType, CabShift cabShift) {
        // ✅ FIX: Extract CabShift attributes for correct LeaseRate lookup
        com.taxi.domain.cab.model.CabType cabType = (cabShift != null) ? cabShift.getCabType() : null;
        boolean hasAirportLicense = (cabShift != null && cabShift.getHasAirportLicense() != null)
                ? cabShift.getHasAirportLicense() : false;

        // ✅ Get applicable rate (override or default) - now passes CabShift attributes
        BigDecimal baseRate = getApplicableLeaseRate(
            owner.getDriverNumber(),
            cab.getCabNumber(),
            shiftType,
            shift.getLogonTime(),
            cab,
            cabType,
            hasAirportLicense
        );

        LeaseRate leaseRate = leaseCalculationService.findApplicableRate(
            cabType,
            hasAirportLicense,
            shift.getLogonTime()
        );

        BigDecimal mileageRate = (leaseRate != null && leaseRate.getMileageRate() != null) ?
            leaseRate.getMileageRate() : BigDecimal.ZERO;

        // ✅ FIX: Get actual miles from DriverShift or MileageRecord
        // Do NOT default to 10 miles - use actual recorded miles or 0
        BigDecimal miles = shift.getTotalDistance();

        // If DriverShift doesn't have miles, try to fetch from MileageRecord
        if (miles == null || miles.compareTo(BigDecimal.ZERO) == 0) {
            try {
                List<com.taxi.domain.mileage.model.MileageRecord> mileageRecords =
                    mileageRecordRepository.findByDriverNumberAndShiftTimes(
                        shift.getDriverNumber(),
                        shift.getLogonTime(),
                        shift.getLogoffTime()
                    );

                if (!mileageRecords.isEmpty()) {
                    // ✅ Sum mileageA (Tariff 1 / Flag fall) to match detail modal calculation
                    // This ensures lease and insurance use the same mileage value
                    miles = mileageRecords.stream()
                        .map(m -> m.getMileageA() != null ? m.getMileageA() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                    if (miles.compareTo(BigDecimal.ZERO) > 0) {
                        log.debug("   Miles (mileageA) for shift {} (driver {}, cab {}): ${} from MileageRecord (not from DriverShift.TotalDistance)",
                            shift.getId(), shift.getDriverNumber(), shift.getCabNumber(), miles);
                    }
                }
            } catch (Exception e) {
                log.warn("   Error fetching mileage records for shift {}: {}", shift.getId(), e.getMessage());
            }
        }

        // If still no miles found, use 0 (not 10)
        if (miles == null || miles.compareTo(BigDecimal.ZERO) == 0) {
            miles = BigDecimal.ZERO;
            log.debug("   No miles recorded for shift {} (driver {}, cab {}), using 0 for mileage calculation",
                shift.getId(), shift.getDriverNumber(), shift.getCabNumber());
        }

        BigDecimal mileageLease = mileageRate.multiply(miles);
        BigDecimal totalLease = baseRate.add(mileageLease);

        return new LeaseCalculationResult(baseRate, mileageRate, miles, mileageLease, totalLease);
    }

    /**
     * Simple result object for lease calculation (made public for debugging)
     */
    public static class LeaseCalculationResult {
        public final BigDecimal baseRate;
        public final BigDecimal mileageRate;
        public final BigDecimal miles;
        public final BigDecimal mileageLease;
        public final BigDecimal totalLease;

        public LeaseCalculationResult(BigDecimal baseRate, BigDecimal mileageRate, BigDecimal miles,
                BigDecimal mileageLease, BigDecimal totalLease) {
            this.baseRate = baseRate;
            this.mileageRate = mileageRate;
            this.miles = miles;
            this.mileageLease = mileageLease;
            this.totalLease = totalLease;
        }
    }

    private LeaseRevenueDTO calculateLeaseForShift(
            DriverShift shift, Cab cab, Driver owner, String shiftType, CabShift cabShift) {
        try {
            // ✅ Use shared calculation with CabShift for correct mileage rate lookup
            LeaseCalculationResult leaseCalc = calculateShiftLeaseAmount(shift, cab, owner, shiftType, cabShift);
            
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
                    .transactionId(transaction.getId())
                    .transactionDate(transaction.getTransactionDate())
                    .transactionTime(transaction.getTransactionTime())
                    .authorizationCode(transaction.getAuthorizationCode())
                    .terminalId(transaction.getTerminalId())
                    .merchantId(transaction.getMerchantId())
                    .batchNumber(transaction.getBatchNumber())
                    .cabNumber(transaction.getCabNumber())
                    .driverNumber(transaction.getDriverNumber())
                    .cardType(transaction.getCardType())
                    .cardLastFour(transaction.getCardLastFour())
                    .cardholderNumber(transaction.getCardholderNumber())
                    .captureMethod(transaction.getCaptureMethod())
                    .amount(amount)
                    .tipAmount(tipAmount)
                    .totalAmount(totalAmount)
                    .processingFee(transaction.getProcessingFee())
                    .netAmount(transaction.getNetAmount())
                    .isSettled(transaction.getIsSettled())
                    .isRefunded(transaction.getIsRefunded())
                    .refundAmount(transaction.getRefundAmount())
                    .settlementDate(transaction.getSettlementDate())
                    .transactionStatus(transaction.getTransactionStatus().name())
                    .receiptNumber(transaction.getReceiptNumber())
                    .notes(transaction.getNotes())
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

        // ✅ DEDUPLICATION: Prevent duplicate shifts from being counted multiple times
        // This matches the deduplication in calculateLeaseRevenue for consistency
        java.util.Map<String, DriverShift> uniqueDriverShifts = new java.util.LinkedHashMap<>();
        for (DriverShift ds : driverShifts) {
            String key = ds.getDriverNumber() + "|" + ds.getLogonTime();
            uniqueDriverShifts.putIfAbsent(key, ds);
        }

        LeaseExpenseReportDTO report = LeaseExpenseReportDTO.builder()
                .workingDriverNumber(driverNumber)
                .workingDriverName(driver.getFullName())
                .startDate(startDate)
                .endDate(endDate)
                .build();

        int skippedInactive = 0;
        int skippedOwnShift = 0;
        int totalProcessed = 0;

        for (DriverShift driverShift : uniqueDriverShifts.values()) {
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

            // ✅ CRITICAL FIX: Get the owner at the time of the shift, not the current owner
            // This ensures consistency with lease revenue which calculates based on shift ownership at time of driving
            LocalDate shiftDate = driverShift.getLogonTime().toLocalDate();
            java.util.Optional<ShiftOwnership> ownershipAtTime = shiftOwnershipRepository.findOwnershipOnDate(
                    cabShift.getId(), shiftDate);

            if (ownershipAtTime.isEmpty()) {
                log.debug("   ⊘ SKIP NO OWNERSHIP: {} on {} (no owner record found)",
                        cabShift.getCab().getCabNumber(), shiftDate);
                continue;
            }

            Driver shiftOwner = ownershipAtTime.get().getOwner();

            if (shiftOwner.getDriverNumber().equals(driverNumber)) {
                skippedOwnShift++;
                continue; // Driver drove own shift
            }

            // ✅ CONSISTENCY CHECK: Verify shift types match
            // This ensures lease expense uses the same logic as lease revenue calculation
            if (!cabShift.getShiftType().name().equals(driverShift.getPrimaryShiftType())) {
                log.debug("   ⊘ SKIP SHIFT TYPE MISMATCH: {} {} (driver shift type: {})",
                        cabShift.getCab().getCabNumber(), cabShift.getShiftType(),
                        driverShift.getPrimaryShiftType());
                continue;
            }

            LeaseExpenseDTO leaseItem = calculateLeaseExpenseForShift(
                    driverShift, cabShift.getCab(), shiftOwner, cabShift.getShiftType().name(), cabShift);

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
            DriverShift shift, Cab cab, Driver owner, String shiftType, CabShift cabShift) {
        try {
            // ✅ Use shared calculation with CabShift for correct mileage rate lookup
            LeaseCalculationResult leaseCalc = calculateShiftLeaseAmount(shift, cab, owner, shiftType, cabShift);

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

    /**
     * Resolve the airport-related attribute type ID for a cab shift.
     * Checks for AIRPORT_PLATE first, then TRANSPONDER.
     * Returns null if the shift has neither attribute.
     */
    private Long resolveAirportAttributeTypeId(String cabNumber, LocalDate date) {
        try {
            // Check all shifts for this cab (DAY and NIGHT)
            List<CabShift> cabShifts = cabShiftRepository.findByCabNumber(cabNumber);
            if (cabShifts.isEmpty()) return null;

            CabAttributeType airportPlateType = cabAttributeTypeRepository.findByAttributeCode("AIRPORT_PLATE").orElse(null);
            CabAttributeType transponderType = cabAttributeTypeRepository.findByAttributeCode("TRANSPONDER").orElse(null);

            // Check AIRPORT_PLATE first (takes priority)
            for (CabShift cabShift : cabShifts) {
                if (airportPlateType != null) {
                    Optional<CabAttributeValue> ap = cabAttributeValueRepository.findAttributeOnDateByShift(
                        cabShift, airportPlateType, date);
                    if (ap.isPresent()) return airportPlateType.getId();
                }
            }

            // Then TRANSPONDER
            for (CabShift cabShift : cabShifts) {
                if (transponderType != null) {
                    Optional<CabAttributeValue> tr = cabAttributeValueRepository.findAttributeOnDateByShift(
                        cabShift, transponderType, date);
                    if (tr.isPresent()) return transponderType.getId();
                }
            }

            return null;
        } catch (Exception e) {
            log.debug("Error resolving airport attribute for cab {}: {}", cabNumber, e.getMessage());
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
            // ✅ Delegate to calculateLeaseExpense which already computes totalLease per shift
            // totalLease = baseRate + mileageLease (calculated in calculateShiftLeaseAmount)
            LeaseExpenseReportDTO report = calculateLeaseExpense(driverNumber, startDate, endDate);

            // ✅ Simply sum the pre-calculated totalLease from each lease item
            // This already includes BOTH base rate + mileage lease, calculated correctly
            // in calculateShiftLeaseAmount() using actual miles from DriverShift/MileageRecord
            BigDecimal total = report.getLeaseExpenseItems() == null ? BigDecimal.ZERO
                : report.getLeaseExpenseItems().stream()
                    .map(LeaseExpenseDTO::getTotalLease)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            log.info("   ✅ Total lease expense for driver {}: ${} ({} shifts)",
                driverNumber, total,
                report.getLeaseExpenseItems() != null ? report.getLeaseExpenseItems().size() : 0);
            return total;
        } catch (Exception e) {
            log.error("   ❌ Error calculating total lease for driver {}: {}", driverNumber, e.getMessage(), e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * ✅ Calculate lease amount for a single driver shift
     * Used by Lease Reconciliation to show correct lease amounts
     * Ensures consistency with driver summary and lease revenue/expense calculations
     */
    public BigDecimal calculateLeaseForSingleShift(
            DriverShift shift, Cab cab, Driver owner, String shiftType) {
        return calculateLeaseForSingleShift(shift, cab, owner, shiftType, null);
    }

    public BigDecimal calculateLeaseForSingleShift(
            DriverShift shift, Cab cab, Driver owner, String shiftType, CabShift cabShift) {
        try {
            LeaseCalculationResult leaseCalc = calculateShiftLeaseAmount(shift, cab, owner, shiftType, cabShift);
            return leaseCalc.totalLease;
        } catch (Exception e) {
            log.error("Error calculating lease for shift {}: {}", shift.getId(), e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /**
     * ✅ Get detailed lease calculation breakdown (for debugging)
     * Returns all components: baseRate, mileageRate, miles, mileageLease, totalLease
     */
    public LeaseCalculationResult calculateLeaseForSingleShiftDetailed(
            DriverShift shift, Cab cab, Driver owner, String shiftType) {
        return calculateLeaseForSingleShiftDetailed(shift, cab, owner, shiftType, null);
    }

    public LeaseCalculationResult calculateLeaseForSingleShiftDetailed(
            DriverShift shift, Cab cab, Driver owner, String shiftType, CabShift cabShift) {
        try {
            return calculateShiftLeaseAmount(shift, cab, owner, shiftType, cabShift);
        } catch (Exception e) {
            log.error("Error calculating lease for shift {}: {}", shift.getId(), e.getMessage());
            return new LeaseCalculationResult(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }

    /**
     * ✅ Determine lease reconciliation status (SINGLE SOURCE OF TRUTH)
     * Matches the logic in LeaseReconciliationService
     *
     * Rules:
     * - SELF_DRIVEN: Driver owns the shift (driver == owner) → NO LEASE
     * - MATCHED: Driver doesn't own shift (driver != owner) → LEASE CHARGED
     *
     * This ensures all reports (expense, revenue, reconciliation) use identical logic
     */
    public String determineLeaseStatus(String driverNumber, Driver owner) {
        if (owner == null) {
            return "NO_OWNER";
        }

        if (owner.getDriverNumber().equals(driverNumber)) {
            return "SELF_DRIVEN";  // Driver owns cab, no lease
        }

        return "MATCHED";  // Driver doesn't own cab, must pay lease
    }

    /**
     * ✅ Check if lease should be charged for this shift
     * Returns true if lease should be calculated (driver != owner)
     * Returns false if no lease (driver == owner)
     */
    public boolean shouldChargeLeaseForShift(String driverNumber, Driver owner) {
        return "MATCHED".equals(determineLeaseStatus(driverNumber, owner));
    }

    /**
     * ✅ Count total airport trips for a driver over a date range.
     *
     * ✅ CRITICAL FIX: Calculate trips by summing results from calculateAirportChargeForShiftDetailed()
     * which includes both trip count AND charge, ensuring accuracy and consistency with charge calculation.
     * This matches the detail modal's approach exactly.
     *
     * @param driverNumber The driver number
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return Total airport trip count for the period
     */
    public int countTotalAirportTrips(String driverNumber, LocalDate startDate, LocalDate endDate) {
        if (driverNumber == null || startDate == null || endDate == null) {
            return 0;
        }

        int totalTrips = 0;

        // PRIMARY: Use pre-computed driver assignments from airport_trip_driver table
        // Track which cabs are handled so fallback only covers remaining cabs
        java.util.Set<String> cabsHandled = new java.util.HashSet<>();
        List<com.taxi.domain.airport.model.AirportTripDriver> assignments =
                airportTripDriverRepository.findByDriverNumberAndTripDateBetweenOrderByTripDateAscHourAsc(
                        driverNumber, startDate, endDate);

        for (com.taxi.domain.airport.model.AirportTripDriver atd : assignments) {
            totalTrips += atd.getTripCount();
            cabsHandled.add(atd.getCabNumber());
        }

        if (!cabsHandled.isEmpty()) {
            log.debug("Airport trips for driver {} ({} to {}): {} from airport_trip_driver for cabs {}",
                    driverNumber, startDate, endDate, totalTrips, cabsHandled);
        }

        // FALLBACK: For owned cabs NOT handled by airport_trip_driver, use airport_trips directly
        int fallbackTrips = countAirportTripsFallback(driverNumber, startDate, endDate, cabsHandled);
        totalTrips += fallbackTrips;

        if (fallbackTrips > 0) {
            log.debug("Airport trips fallback for driver {} ({} to {}): {} additional trips from airport_trips",
                    driverNumber, startDate, endDate, fallbackTrips);
        }

        return totalTrips;
    }

    /**
     * Fallback: count airport trips from airport_trips table directly for driver's owned cabs
     * that are NOT already handled by airport_trip_driver.
     */
    private int countAirportTripsFallback(String driverNumber, LocalDate startDate, LocalDate endDate,
                                           java.util.Set<String> cabsAlreadyHandled) {
        try {
            Optional<Driver> driverOpt = driverRepository.findByDriverNumber(driverNumber);
            if (driverOpt.isEmpty()) return 0;

            List<ShiftOwnership> ownerships = shiftOwnershipRepository.findOwnershipsInRange(
                    driverOpt.get().getId(), startDate, endDate);
            if (ownerships.isEmpty()) return 0;

            java.util.Set<String> processedCabs = new java.util.HashSet<>();
            int total = 0;

            for (ShiftOwnership ownership : ownerships) {
                String cabNumber = ownership.getShift().getCab().getCabNumber();
                if (cabNumber == null || !processedCabs.add(cabNumber)) continue;
                if (cabsAlreadyHandled.contains(cabNumber)) continue;

                List<AirportTrip> trips = airportTripRepository.findByCabNumberAndTripDateBetweenOrderByTripDateDesc(
                        cabNumber, startDate, endDate);
                for (AirportTrip trip : trips) {
                    if (trip.getGrandTotal() != null) {
                        total += trip.getGrandTotal();
                    }
                }
            }
            return total;
        } catch (Exception e) {
            log.warn("Error in airport trips fallback for driver {}: {}", driverNumber, e.getMessage());
            return 0;
        }
    }

    /**
     * Calculate total airport trip charges for a driver over a date range.
     * Uses pre-computed airport_trip_driver assignments to determine which trips
     * belong to this driver, then applies the attribute-specific rate per cab.
     */
    public BigDecimal calculateAirportExpense(String driverNumber, LocalDate startDate, LocalDate endDate) {
        if (driverNumber == null || startDate == null || endDate == null) {
            return BigDecimal.ZERO;
        }

        // PRIMARY: Use pre-computed airport_trip_driver assignments, track handled cabs
        List<com.taxi.domain.airport.model.AirportTripDriver> assignments =
                airportTripDriverRepository.findByDriverNumberAndTripDateBetweenOrderByTripDateAscHourAsc(
                        driverNumber, startDate, endDate);

        java.util.Map<String, Integer> tripsByCab = new java.util.HashMap<>();
        java.util.Set<String> cabsHandled = new java.util.HashSet<>();

        for (com.taxi.domain.airport.model.AirportTripDriver atd : assignments) {
            tripsByCab.merge(atd.getCabNumber(), atd.getTripCount(), Integer::sum);
            cabsHandled.add(atd.getCabNumber());
        }

        BigDecimal totalExpense = BigDecimal.ZERO;
        for (java.util.Map.Entry<String, Integer> entry : tripsByCab.entrySet()) {
            String cabNumber = entry.getKey();
            int trips = entry.getValue();

            Long attributeTypeId = resolveAirportAttributeTypeId(cabNumber, startDate);
            ItemRate rate = airportChargeService.getAirportTripRateForAttribute(attributeTypeId, startDate);

            if (rate != null) {
                totalExpense = totalExpense.add(rate.getRate().multiply(BigDecimal.valueOf(trips)));
            }
        }

        // FALLBACK: For owned cabs NOT handled by airport_trip_driver, use airport_trips directly
        BigDecimal fallbackExpense = calculateAirportExpenseFallback(driverNumber, startDate, endDate, cabsHandled);
        totalExpense = totalExpense.add(fallbackExpense);

        if (totalExpense.compareTo(BigDecimal.ZERO) > 0) {
            log.debug("Airport expense for driver {} ({} to {}): ${} (primary) + ${} (fallback)",
                    driverNumber, startDate, endDate,
                    totalExpense.subtract(fallbackExpense), fallbackExpense);
        }
        return totalExpense;
    }

    /**
     * Fallback: calculate airport expense from airport_trips table directly for driver's owned cabs
     * that are NOT already handled by airport_trip_driver.
     */
    private BigDecimal calculateAirportExpenseFallback(String driverNumber, LocalDate startDate, LocalDate endDate,
                                                        java.util.Set<String> cabsAlreadyHandled) {
        try {
            Optional<Driver> driverOpt = driverRepository.findByDriverNumber(driverNumber);
            if (driverOpt.isEmpty()) return BigDecimal.ZERO;

            List<ShiftOwnership> ownerships = shiftOwnershipRepository.findOwnershipsInRange(
                    driverOpt.get().getId(), startDate, endDate);
            if (ownerships.isEmpty()) return BigDecimal.ZERO;

            java.util.Set<String> processedCabs = new java.util.HashSet<>();
            BigDecimal totalExpense = BigDecimal.ZERO;

            for (ShiftOwnership ownership : ownerships) {
                String cabNumber = ownership.getShift().getCab().getCabNumber();
                if (cabNumber == null || !processedCabs.add(cabNumber)) continue;
                if (cabsAlreadyHandled.contains(cabNumber)) continue;

                List<AirportTrip> trips = airportTripRepository.findByCabNumberAndTripDateBetweenOrderByTripDateDesc(
                        cabNumber, startDate, endDate);
                if (trips.isEmpty()) continue;

                Long attributeTypeId = resolveAirportAttributeTypeId(cabNumber, startDate);
                ItemRate rate = airportChargeService.getAirportTripRateForAttribute(attributeTypeId, startDate);

                if (rate == null) {
                    log.warn("No AIRPORT_TRIP rate found for cab {} on {} (fallback)", cabNumber, startDate);
                    continue;
                }

                BigDecimal ratePerTrip = rate.getRate();
                for (AirportTrip trip : trips) {
                    int dayTrips = trip.getGrandTotal() != null ? trip.getGrandTotal() : 0;
                    if (dayTrips == 0) continue;
                    totalExpense = totalExpense.add(ratePerTrip.multiply(BigDecimal.valueOf(dayTrips)));
                }

                log.info("Airport expense (fallback): Cab {} for driver {} using airport_trips directly", cabNumber, driverNumber);
            }
            return totalExpense;
        } catch (Exception e) {
            log.warn("Error in airport expense fallback for driver {}: {}", driverNumber, e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /**
     * Wrapper for countTotalAirportTrips for clarity
     */
    public int countAirportTrips(String driverNumber, LocalDate startDate, LocalDate endDate) {
        return countTotalAirportTrips(driverNumber, startDate, endDate);
    }
}