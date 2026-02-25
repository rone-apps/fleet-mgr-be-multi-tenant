package com.taxi.domain.expense.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.taxi.domain.cab.repository.CabRepository;
import com.taxi.domain.driver.model.Driver;
import com.taxi.domain.driver.repository.DriverRepository;
import com.taxi.domain.expense.model.ApplicationType;
import com.taxi.domain.expense.model.OneTimeExpense;
import com.taxi.domain.expense.model.RecurringExpense;
import com.taxi.domain.expense.model.ItemRate;
import com.taxi.domain.expense.model.ItemRateOverride;
import com.taxi.domain.expense.repository.ItemRateRepository;
import com.taxi.domain.expense.repository.ItemRateOverrideRepository;
import com.taxi.domain.mileage.model.MileageRecord;
import com.taxi.domain.mileage.repository.MileageRecordRepository;
import com.taxi.domain.profile.model.ItemRateChargedTo;
import com.taxi.domain.profile.model.ItemRateUnitType;
import com.taxi.domain.expense.repository.RecurringExpenseRepository;
import com.taxi.domain.revenue.entity.OtherRevenue;
import com.taxi.domain.revenue.repository.OtherRevenueRepository;
import com.taxi.domain.account.repository.AccountChargeRepository;
import com.taxi.domain.payment.repository.CreditCardTransactionRepository;
import com.taxi.domain.report.service.DriverFinancialCalculationService;
import com.taxi.web.dto.report.LeaseRevenueDTO;
import com.taxi.web.dto.report.LeaseExpenseDTO;
import com.taxi.domain.shift.model.CabShift;
import com.taxi.domain.shift.model.ShiftLog;
import com.taxi.domain.shift.model.ShiftOwnership;
import com.taxi.domain.shift.repository.CabShiftRepository;
import com.taxi.domain.shift.repository.ShiftLogRepository;
import com.taxi.domain.shift.repository.ShiftOwnershipRepository;
import com.taxi.domain.statement.model.Statement;
import com.taxi.domain.statement.model.StatementStatus;
import com.taxi.domain.statement.repository.StatementRepository;
import com.taxi.domain.cab.repository.CabAttributeValueRepository;
import com.taxi.web.dto.expense.OwnerReportDTO;
import com.taxi.web.dto.expense.StatementLineItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FinancialStatementService {

    private final RecurringExpenseRepository recurringExpenseRepository;
    private final CabShiftRepository cabShiftRepository;
    private final DriverRepository driverRepository;
    private final CabRepository cabRepository;
    private final ShiftOwnershipRepository shiftOwnershipRepository;
    private final OtherRevenueRepository otherRevenueRepository;
    private final ShiftLogRepository shiftLogRepository;
    private final CreditCardTransactionRepository creditCardTransactionRepository;
    private final AccountChargeRepository accountChargeRepository;
    private final DriverFinancialCalculationService driverFinancialCalculationService;
    private final StatementRepository statementRepository;
    private final ObjectMapper objectMapper;
    private final CabAttributeValueRepository cabAttributeValueRepository;
    private final com.taxi.domain.account.repository.StatementPaymentRepository statementPaymentRepository;
    private final ItemRateRepository itemRateRepository;
    private final ItemRateOverrideRepository itemRateOverrideRepository;
    private final MileageRecordRepository mileageRecordRepository;

    // ═══════════════════════════════════════════════════════════════════════
    // NEW CONSOLIDATED SERVICES - SINGLE SOURCE OF TRUTH
    // ═══════════════════════════════════════════════════════════════════════
    private final ExpenseCalculationService expenseCalculationService;
    /**
     * Generate a financial statement for a driver for a date period
     * Shows all applicable recurring (prorated) and one-time charges
     */
    /**
     * DEPRECATED: Old methods generateDriverStatement() and generateOwnerStatement() have been removed.
     * Use generateOwnerReport() instead for all financial report generation.
     * generateOwnerReport() works for both drivers and owners and returns complete financial data.
     */

    // Helper method for filtering shifts by date range (still used internally)
    private List<CabShift> filterShiftsByDateRange(List<CabShift> shifts, LocalDate from, LocalDate to) {
        return shifts.stream()
            .filter(shift -> shift.isActiveOn(from) || shift.isActiveOn(to))
            .toList();
    }

    /**
     * Calculate per-unit expenses (mileage, airport trips, etc.) for a given period
     * Returns a map of ItemRateChargedTo -> List of PerUnitExpenseLineItem
     *
     * Logic:
     * 1. Fetch all active item rates for the period
     * 2. For each rate, get overrides for this person (if owner)
     * 3. Use override rate if exists, otherwise use base rate
     * 4. Sum units (miles, trips) from shift logs
     * 5. Calculate amount = totalUnits × effectiveRate
     */
    private Map<ItemRateChargedTo, List<OwnerReportDTO.PerUnitExpenseLineItem>> calculatePerUnitExpenses(
            List<ShiftLog> shiftLogs, LocalDate from, LocalDate to, Driver person) {

        Map<ItemRateChargedTo, List<OwnerReportDTO.PerUnitExpenseLineItem>> result = new HashMap<>();
        result.put(ItemRateChargedTo.DRIVER, new ArrayList<>());
        result.put(ItemRateChargedTo.OWNER, new ArrayList<>());

        if (shiftLogs.isEmpty()) {
            log.info("No shift logs provided for per-unit expense calculation");
            return result;
        }

        // Fetch all active item rates during the period
        // Use mid-period date for determining active rates
        LocalDate checkDate = from.plusDays((ChronoUnit.DAYS.between(from, to) / 2));
        List<ItemRate> rates = itemRateRepository.findActiveOnDate(checkDate);

        log.info("Found {} item rates active on {} in period {} to {}",
                rates.size(), checkDate, from, to);

        // For each rate, accumulate units and calculate amount
        for (ItemRate rate : rates) {
            BigDecimal totalUnits = BigDecimal.ZERO;

            // Sum units from shift logs based on rate type
            if (rate.getUnitType() == ItemRateUnitType.MILEAGE) {
                totalUnits = shiftLogs.stream()
                        .map(ShiftLog::getTotalMiles)
                        .filter(java.util.Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            } else if (rate.getUnitType() == ItemRateUnitType.AIRPORT_TRIP) {
                totalUnits = shiftLogs.stream()
                        .map(log -> BigDecimal.valueOf(log.getAirportTripCount() != null ? log.getAirportTripCount() : 0))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }

            // Only add line item if there are units
            if (totalUnits.compareTo(BigDecimal.ZERO) > 0) {
                // Determine effective rate: use override if available, otherwise use base rate
                BigDecimal effectiveRate = rate.getRate();

                // Check for owner overrides if this is an owner
                if (Boolean.TRUE.equals(person.getIsOwner()) && person.getDriverNumber() != null) {
                    List<ItemRateOverride> overrides = itemRateOverrideRepository.findActiveOverridesForRate(
                            rate.getId(), person.getDriverNumber(), from);

                    if (!overrides.isEmpty()) {
                        // Use the highest priority override
                        effectiveRate = overrides.get(0).getOverrideRate();
                        log.debug("Using override rate {} for item rate {} for owner {}",
                                effectiveRate, rate.getName(), person.getDriverNumber());
                    }
                }

                BigDecimal amount = totalUnits.multiply(effectiveRate);

                OwnerReportDTO.PerUnitExpenseLineItem lineItem = OwnerReportDTO.PerUnitExpenseLineItem.builder()
                        .name(rate.getName())
                        .unitType(rate.getUnitType().toString())
                        .unitTypeDisplay(rate.getUnitType().getDisplayName())
                        .totalUnits(totalUnits)
                        .rate(effectiveRate)
                        .amount(amount)
                        .chargedTo(rate.getChargedTo().toString())
                        .build();

                result.get(rate.getChargedTo()).add(lineItem);

                log.debug("Added per-unit expense: {} - {} {} @ ${} = ${}",
                        rate.getName(), totalUnits, rate.getUnitType().getSymbol(),
                        effectiveRate, amount);
            }
        }

        return result;
    }

    /**
     * Generate a comprehensive financial report for a driver or owner
     * Shows revenues, expenses (recurring and one-time), and net amount
     */
    public OwnerReportDTO generateOwnerReport(Long personId, LocalDate from, LocalDate to) {
        Driver person = driverRepository.findById(personId)
            .orElseThrow(() -> new RuntimeException("Driver/Owner not found: " + personId));

        log.info("Generating report for {} {} from {} to {}",
            Boolean.TRUE.equals(person.getIsOwner()) ? "owner" : "driver", personId, from, to);

        OwnerReportDTO report = OwnerReportDTO.builder()
            .ownerId(person.getId())
            .ownerName(person.getFullName())
            .ownerNumber(person.getDriverNumber())
            .periodFrom(from)
            .periodTo(to)
            .revenues(new ArrayList<>())
            .recurringExpenses(new ArrayList<>())
            .oneTimeExpenses(new ArrayList<>())
            .build();

        List<CabShift> relevantShifts = new ArrayList<>();

        // ✅ KEY BUSINESS RULE: Shift-based charges (ALL_OWNERS, SHIFT_PROFILE, SPECIFIC_SHIFT, SHIFTS_WITH_ATTRIBUTE)
        // are ALWAYS charged to the OWNER of the shift, NEVER to drivers driving those shifts
        if (Boolean.TRUE.equals(person.getIsOwner())) {
            // For owners: get all shifts they own from shift_ownership table within the date range
            List<ShiftOwnership> ownerships = shiftOwnershipRepository.findOwnershipsInRange(personId, from, to);
            log.info("Owner {} has {} shift ownerships in period {} to {}", personId, ownerships.size(), from, to);

            // Extract the shifts from the ownerships
            relevantShifts = ownerships.stream()
                .map(ShiftOwnership::getShift)
                .filter(shift -> shift != null)
                .distinct()
                .toList();

            log.info("Owner {} has {} unique shifts in period", personId, relevantShifts.size());
        } else {
            // For drivers: DO NOT fetch shifts
            // ✅ Drivers should NOT receive shift-based charges (those go to the owner)
            // Drivers only get charges specifically assigned to them as a driver
            log.info("Driver {} - will only receive driver-specific charges and lease expenses", personId);
        }

        // 1. Add ALL_OWNERS expenses (only for owners - shift-based charges)
        if (Boolean.TRUE.equals(person.getIsOwner())) {
            List<RecurringExpense> allActiveExpenses = recurringExpenseRepository.findEffectiveBetweenForApplicationType(
                ApplicationType.ALL_OWNERS, from, to);

            for (RecurringExpense expense : allActiveExpenses) {
                BigDecimal proratedAmount = expense.calculateAmountForDateRange(from, to);
                log.info("Adding ALL_OWNERS expense {} (prorated: {})",
                    expense.getExpenseCategory().getCategoryName(), proratedAmount);

                report.getRecurringExpenses().add(StatementLineItem.builder()
                    .categoryCode(expense.getExpenseCategory().getCategoryCode())
                    .categoryName(expense.getExpenseCategory().getCategoryName())
                    .applicationType(expense.getApplicationTypeEnum() != null ? expense.getApplicationTypeEnum().toString() : "")
                    .entityDescription("All Active Shifts")
                    .billingMethod(expense.getBillingMethod())
                    .effectiveFrom(expense.getEffectiveFrom())
                    .effectiveTo(expense.getEffectiveTo())
                    .amount(proratedAmount)
                    .isRecurring(true)
                    .build());
            }
        }

        // 2. Add SHIFT_PROFILE and SPECIFIC_SHIFT expenses (per shift, only for owners)
        if (Boolean.TRUE.equals(person.getIsOwner())) {
            log.info("Processing per-shift recurring expenses for {} individual shifts", relevantShifts.size());

            for (CabShift shift : relevantShifts) {
            // SHIFT_PROFILE expenses for this shift's profile
            if (shift.getCurrentProfile() != null) {
                Long profileId = shift.getCurrentProfile().getId();
                List<RecurringExpense> profileExpenses = recurringExpenseRepository.findByApplicationTypeAndShiftProfileIdBetween(
                    ApplicationType.SHIFT_PROFILE, profileId, from, to);

                // Deduplicate per-shift (in case query returns duplicates)
                Map<Long, RecurringExpense> uniqueExpenses = new LinkedHashMap<>();
                for (RecurringExpense expense : profileExpenses) {
                    uniqueExpenses.putIfAbsent(expense.getId(), expense);
                }

                log.info("  Shift {}: Found {} SHIFT_PROFILE expenses for profile {} (after dedup: {})",
                    shift.getId(), profileExpenses.size(), profileId, uniqueExpenses.size());

                for (RecurringExpense expense : uniqueExpenses.values()) {
                    BigDecimal proratedAmount = expense.calculateAmountForDateRange(from, to);
                    log.info("    Adding SHIFT_PROFILE expense ID {} - {} for profile {} (prorated: {})",
                        expense.getId(), expense.getExpenseCategory().getCategoryName(), profileId, proratedAmount);

                    report.getRecurringExpenses().add(StatementLineItem.builder()
                        .categoryCode(expense.getExpenseCategory().getCategoryCode())
                        .categoryName(expense.getExpenseCategory().getCategoryName())
                        .applicationType(expense.getApplicationTypeEnum() != null ? expense.getApplicationTypeEnum().toString() : "")
                        .entityDescription("Cab " + shift.getCab().getCabNumber() + " - " + shift.getShiftType() + " (Profile: " + profileId + ")")
                        .billingMethod(expense.getBillingMethod())
                        .effectiveFrom(expense.getEffectiveFrom())
                        .effectiveTo(expense.getEffectiveTo())
                        .amount(proratedAmount)
                        .isRecurring(true)
                        .build());
                }
            }

            // SPECIFIC_SHIFT expenses for this shift
            List<RecurringExpense> shiftExpenses = recurringExpenseRepository.findByApplicationTypeAndSpecificShiftIdBetween(
                ApplicationType.SPECIFIC_SHIFT, shift.getId(), from, to);

            // Deduplicate per-shift (in case query returns duplicates)
            Map<Long, RecurringExpense> uniqueShiftExpenses = new LinkedHashMap<>();
            for (RecurringExpense expense : shiftExpenses) {
                uniqueShiftExpenses.putIfAbsent(expense.getId(), expense);
            }

            for (RecurringExpense expense : uniqueShiftExpenses.values()) {
                BigDecimal proratedAmount = expense.calculateAmountForDateRange(from, to);
                log.info("    Adding SPECIFIC_SHIFT expense ID {} - {} (prorated: {})",
                    expense.getId(), expense.getExpenseCategory().getCategoryName(), proratedAmount);

                report.getRecurringExpenses().add(StatementLineItem.builder()
                    .categoryCode(expense.getExpenseCategory().getCategoryCode())
                    .categoryName(expense.getExpenseCategory().getCategoryName())
                    .applicationType(expense.getApplicationTypeEnum() != null ? expense.getApplicationTypeEnum().toString() : "")
                    .entityDescription("Cab " + shift.getCab().getCabNumber() + " - " + shift.getShiftType())
                    .billingMethod(expense.getBillingMethod())
                    .effectiveFrom(expense.getEffectiveFrom())
                    .effectiveTo(expense.getEffectiveTo())
                    .amount(proratedAmount)
                    .isRecurring(true)
                    .build());
            }
        }  // End: for each shift
        }  // End: SHIFT_PROFILE and SPECIFIC_SHIFT expenses (owners only)

        // ═══════════════════════════════════════════════════════════════════════
        // USE NEW CONSOLIDATED EXPENSE CALCULATION SERVICE
        // ═══════════════════════════════════════════════════════════════════════

        // 2. Get all applicable person-specific recurring expenses
        List<RecurringExpense> personRecurringExpenses = expenseCalculationService
                .getApplicableRecurringExpenses(person, relevantShifts, from, to)
                .stream()
                .filter(e -> e.getApplicationTypeEnum() == null ||
                        (e.getApplicationTypeEnum().name().equals("SPECIFIC_PERSON") ||
                         e.getApplicationTypeEnum().name().equals("ALL_OWNERS") ||
                         e.getApplicationTypeEnum().name().equals("ALL_DRIVERS")))
                .toList();

        for (RecurringExpense expense : personRecurringExpenses) {
            BigDecimal proratedAmount = expense.calculateAmountForDateRange(from, to);

            report.getRecurringExpenses().add(StatementLineItem.builder()
                .categoryCode(expense.getExpenseCategory().getCategoryCode())
                .categoryName(expense.getExpenseCategory().getCategoryName())
                .applicationType(expense.getApplicationTypeEnum() != null ? expense.getApplicationTypeEnum().toString() : "")
                .entityDescription(Boolean.TRUE.equals(person.getIsOwner()) ? "Owner Specific" : "Driver Specific")
                .billingMethod(expense.getBillingMethod())
                .effectiveFrom(expense.getEffectiveFrom())
                .effectiveTo(expense.getEffectiveTo())
                .amount(proratedAmount)
                .isRecurring(true)
                .build());
        }

        // 3. ✅ Add all one-time expenses (uses consolidated service with ALL_ACTIVE_SHIFTS expansion)
        List<OneTimeExpense> oneTimeExpenses = expenseCalculationService
                .getApplicableOneTimeExpenses(person, relevantShifts, from, to);

        for (OneTimeExpense expense : oneTimeExpenses) {
            String categoryCode = expense.getExpenseCategory() != null
                    ? expense.getExpenseCategory().getCategoryCode()
                    : "AD_HOC";
            String categoryName = expense.getExpenseCategory() != null
                    ? expense.getExpenseCategory().getCategoryName()
                    : (expense.getName() != null ? expense.getName() : "Other Charge");

            // ✅ SPECIAL HANDLING FOR ALL_ACTIVE_SHIFTS: Expand per shift
            if (expense.getApplicationType() == ApplicationType.ALL_ACTIVE_SHIFTS && !relevantShifts.isEmpty()) {
                log.info("EXPANDING ALL_ACTIVE_SHIFTS one-time expense ID {} to {} shifts",
                        expense.getId(), relevantShifts.size());
                for (CabShift shift : relevantShifts) {
                    report.getOneTimeExpenses().add(StatementLineItem.builder()
                            .categoryCode(categoryCode)
                            .categoryName(categoryName)
                            .applicationType("ALL_ACTIVE_SHIFTS")
                            .entityDescription("Shift: Cab " + shift.getCab().getCabNumber() + " - " + shift.getShiftType())
                            .cabNumber(shift.getCab().getCabNumber())
                            .shiftType(shift.getShiftType() != null ? shift.getShiftType().toString() : "-")
                            .chargeTarget("All Active Shifts")
                            .date(expense.getExpenseDate())
                            .description(expense.getDescription() != null ? expense.getDescription() : expense.getName())
                            .amount(expense.getAmount())
                            .isRecurring(false)
                            .build());
                    log.debug("  - Added charge to shift: Cab {} - {}", shift.getCab().getCabNumber(), shift.getShiftType());
                }
            }
            // ✅ SPECIAL HANDLING FOR SHIFTS_WITH_ATTRIBUTE: Expand to shifts with that attribute
            else if (expense.getApplicationType() == ApplicationType.SHIFTS_WITH_ATTRIBUTE &&
                    expense.getAttributeTypeId() != null && !relevantShifts.isEmpty()) {

                log.info("EXPANDING SHIFTS_WITH_ATTRIBUTE one-time expense ID {} (attribute type {}) to applicable shifts",
                        expense.getId(), expense.getAttributeTypeId());

                // Find which shifts have this attribute
                int expandedCount = 0;
                for (CabShift shift : relevantShifts) {
                    var shiftAttributes = cabAttributeValueRepository.findCurrentAttributesByShiftId(shift.getId());
                    boolean hasAttribute = shiftAttributes.stream()
                            .anyMatch(attr -> attr.getAttributeType().getId().equals(expense.getAttributeTypeId()));

                    if (hasAttribute) {
                        report.getOneTimeExpenses().add(StatementLineItem.builder()
                                .categoryCode(categoryCode)
                                .categoryName(categoryName)
                                .applicationType("SHIFTS_WITH_ATTRIBUTE")
                                .entityDescription("Shift: Cab " + shift.getCab().getCabNumber() +
                                        " - " + shift.getShiftType() + " (with attribute)")
                                .cabNumber(shift.getCab().getCabNumber())
                                .shiftType(shift.getShiftType() != null ? shift.getShiftType().toString() : "-")
                                .chargeTarget("With Attribute")
                                .date(expense.getExpenseDate())
                                .description(expense.getDescription() != null ? expense.getDescription() : expense.getName())
                                .amount(expense.getAmount())
                                .isRecurring(false)
                                .build());
                        expandedCount++;
                        log.debug("  - Added charge to shift with attribute: Cab {} - {}", shift.getCab().getCabNumber(), shift.getShiftType());
                    }
                }
                log.debug("  - Expanded SHIFTS_WITH_ATTRIBUTE to {} shifts with the attribute", expandedCount);
            } else {
                // Standard handling for non-ALL_ACTIVE_SHIFTS and non-SHIFTS_WITH_ATTRIBUTE expenses
                report.getOneTimeExpenses().add(StatementLineItem.builder()
                        .categoryCode(categoryCode)
                        .categoryName(categoryName)
                        .applicationType(expense.getApplicationType() != null ? expense.getApplicationType().toString() : "")
                        .entityDescription("One-Time Charge")
                        .date(expense.getExpenseDate())
                        .description(expense.getDescription() != null ? expense.getDescription() : expense.getName())
                        .amount(expense.getAmount())
                        .isRecurring(false)
                        .build());
            }
        }

        // 3.5. Add lease expenses (for drivers only - rental cost for shifts)
        if (!Boolean.TRUE.equals(person.getIsOwner()) && person.getDriverNumber() != null) {
            try {
                com.taxi.web.dto.report.LeaseExpenseReportDTO leaseExpenseReport =
                    driverFinancialCalculationService.calculateLeaseExpense(
                        person.getDriverNumber(), from, to);

                log.info("Found {} lease expense items for driver {}",
                    leaseExpenseReport.getLeaseExpenseItems() != null ? leaseExpenseReport.getLeaseExpenseItems().size() : 0, personId);

                if (leaseExpenseReport.getLeaseExpenseItems() != null && !leaseExpenseReport.getLeaseExpenseItems().isEmpty()) {
                    for (LeaseExpenseDTO leaseItem : leaseExpenseReport.getLeaseExpenseItems()) {
                        String categoryCode = "LEASE_EXP";
                        String categoryName = "Lease Expense";
                        String description = "Lease - Cab " + leaseItem.getCabNumber() +
                            " (" + leaseItem.getShiftType() + ") - Owner: " + leaseItem.getOwnerDriverName() +
                            " (" + leaseItem.getOwnerDriverNumber() + ")";

                        // Get fixed lease amount (base rate)
                        BigDecimal fixedLeaseAmount = leaseItem.getBaseRate();

                        // Calculate mileage lease for this specific shift from mileage records
                        // Use the shift owner from shift ownership (already set in ownerDriverNumber from CabShift.getCurrentOwner())
                        MileageCalculationResult result = calculateMileageLeaseForDay(person, leaseItem.getLogonTime(), leaseItem.getLogoffTime(),
                                leaseItem.getOwnerDriverNumber(), leaseItem.getCabNumber(), leaseItem.getShiftType());

                        BigDecimal mileageLeaseAmount = result.mileageLease;

                        // Total lease = fixed + mileage
                        BigDecimal totalLease = fixedLeaseAmount.add(mileageLeaseAmount);

                        // Create lease breakdown with both fixed and mileage components
                        StatementLineItem.LeaseBreakdown breakdown = StatementLineItem.LeaseBreakdown.builder()
                            .fixedLeaseAmount(fixedLeaseAmount)
                            .mileageLeaseAmount(mileageLeaseAmount)
                            .build();

                        report.getOneTimeExpenses().add(StatementLineItem.builder()
                            .categoryCode(categoryCode)
                            .categoryName(categoryName)
                            .applicationType("LEASE_RENT")
                            .date(leaseItem.getShiftDate())
                            .description(description)
                            .amount(totalLease)
                            .leaseBreakdown(breakdown)
                            .isRecurring(false)
                            .build());

                        log.debug("Added lease expense: Cab {} Owner {} on {} = ${} (Fixed: ${}, Mileage: ${})",
                            leaseItem.getCabNumber(), leaseItem.getOwnerDriverNumber(), leaseItem.getShiftDate(),
                            totalLease, fixedLeaseAmount, mileageLeaseAmount);
                    }
                }
            } catch (Exception e) {
                log.warn("Error calculating lease/insurance expense for driver {}: {}", personId, e.getMessage());
                // Don't fail the whole report if lease calculation fails
            }
        }

        // 3.6. Add insurance expense (based on mileage records - applies to everyone)
        try {
            // Get insurance rate first
            java.util.Optional<ItemRate> insuranceRateOpt = itemRateRepository.findByName("INSURANCE_RATE");
            if (!insuranceRateOpt.isPresent()) {
                log.debug("INSURANCE_RATE not found in item_rate table, skipping insurance expense");
            } else {
                ItemRate insuranceRateObj = insuranceRateOpt.get();
                BigDecimal insuranceRate = insuranceRateObj.getRate();

                // Get all mileage records for this person (driver or owner) in the date range
                List<MileageRecord> mileageRecords = mileageRecordRepository.findByDriverNumberAndDateRange(
                        person.getDriverNumber(), from, to);

                log.info("Found {} mileage records for person {} between {} and {}",
                        mileageRecords.size(), personId, from, to);

                // Calculate insurance for each mileage record
                for (MileageRecord mileageRecord : mileageRecords) {
                    BigDecimal miles = mileageRecord.getMileageA();
                    if (miles != null && miles.compareTo(BigDecimal.ZERO) > 0) {
                        // Calculate insurance for this record
                        BigDecimal insuranceExpense = miles.multiply(insuranceRate)
                                .setScale(2, java.math.RoundingMode.HALF_UP);

                        String insuranceDescription = "Insurance - Mileage: " + miles.setScale(2, java.math.RoundingMode.HALF_UP) +
                                " miles @ $" + String.format("%.2f", insuranceRate) + "/mile";

                        report.getInsuranceMileageExpenses().add(StatementLineItem.builder()
                                .categoryCode("INSURANCE_MILEAGE")
                                .categoryName("Insurance Mileage")
                                .applicationType("INSURANCE")
                                .date(mileageRecord.getLogonTime() != null ? mileageRecord.getLogonTime().toLocalDate() : from)
                                .cabNumber(mileageRecord.getCabNumber())
                                .description(insuranceDescription)
                                .miles(miles)
                                .amount(insuranceExpense)
                                .isRecurring(false)
                                .build());

                        log.debug("Added insurance expense for person {}: {} miles × ${}/mile = ${}",
                                personId, miles, insuranceRate, insuranceExpense);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error calculating insurance expense for person {}: {}", personId, e.getMessage());
            // Don't fail the whole report if insurance calculation fails
        }

        // 4. Add shift-based revenues (trip fares, tips, etc.)
        // Get all shift logs for this owner/driver in the date range
        List<ShiftLog> shiftLogs;
        if (Boolean.TRUE.equals(person.getIsOwner())) {
            shiftLogs = shiftLogRepository.findByOwnerIdAndDateRange(personId, from, to);
            log.info("Found {} shift logs owned by person {} between {} and {}", shiftLogs.size(), personId, from, to);
        } else {
            shiftLogs = shiftLogRepository.findByDriverIdAndDateRange(personId, from, to);
            log.info("Found {} shift logs for driver {} between {} and {}", shiftLogs.size(), personId, from, to);
        }

        for (ShiftLog shiftLog : shiftLogs) {
            if (shiftLog.getRevenues() != null && !shiftLog.getRevenues().isEmpty()) {
                for (com.taxi.domain.revenue.model.Revenue revenue : shiftLog.getRevenues()) {
                    String revenueType = revenue.getRevenueType() != null ? revenue.getRevenueType().toString() : "OTHER";
                    String description = revenue.getDescription() != null ? revenue.getDescription() :
                        (revenue.getCustomerName() != null ? revenue.getCustomerName() : revenueType);

                    report.getRevenues().add(OwnerReportDTO.RevenueLineItem.builder()
                        .categoryName("Trip Revenue")
                        .revenueDate(shiftLog.getLogDate() != null ? shiftLog.getLogDate() : from)
                        .description(description)
                        .revenueType(revenueType)
                        .revenueSubType("SHIFT_REVENUE")
                        .amount(revenue.getAmount())
                        .build());
                    log.debug("Added trip revenue: {} {}", revenueType, revenue.getAmount());
                }
            }
        }

        // 5. Add credit card transaction revenues
        if (person.getDriverNumber() != null) {
            List<com.taxi.domain.payment.model.CreditCardTransaction> creditCardTransactions =
                creditCardTransactionRepository.findByDriverNumberAndDateRange(person.getDriverNumber(), from, to);
            log.info("Found {} credit card transactions for person {}", creditCardTransactions.size(), personId);

            for (com.taxi.domain.payment.model.CreditCardTransaction transaction : creditCardTransactions) {
                String cardDesc = (transaction.getCardLastFour() != null ? "Card ending in " + transaction.getCardLastFour() : "Credit Card") +
                    (transaction.getCardType() != null ? " (" + transaction.getCardType() + ")" : "");

                report.getRevenues().add(OwnerReportDTO.RevenueLineItem.builder()
                    .categoryName("Credit Card Revenue")
                    .revenueDate(transaction.getTransactionDate())
                    .description(cardDesc)
                    .revenueType("CREDIT_CARD")
                    .revenueSubType("CARD_REVENUE")
                    .amount(transaction.getTotalAmount())
                    .build());
                log.debug("Added credit card revenue: {}", transaction.getTotalAmount());
            }
        }

        // 6. Add account charge revenues
        try {
            List<com.taxi.domain.account.model.AccountCharge> accountCharges =
                accountChargeRepository.findByDriverIdAndDateRange(personId, from, to);
            log.info("Found {} account charges for person {} between {} and {}",
                accountCharges.size(), personId, from, to);

            for (com.taxi.domain.account.model.AccountCharge charge : accountCharges) {
                if (charge == null) continue;

                String accountName = "Account Charge";
                if (charge.getAccountCustomer() != null) {
                    if (charge.getAccountCustomer().getCompanyName() != null) {
                        accountName = charge.getAccountCustomer().getCompanyName();
                    }
                }

                BigDecimal fareAmount = charge.getFareAmount() != null ? charge.getFareAmount() : BigDecimal.ZERO;
                BigDecimal tipAmount = charge.getTipAmount() != null ? charge.getTipAmount() : BigDecimal.ZERO;
                BigDecimal totalAmount = fareAmount.add(tipAmount);

                OwnerReportDTO.RevenueLineItem item = OwnerReportDTO.RevenueLineItem.builder()
                    .categoryName("Account Charges")
                    .accountName(accountName)
                    .revenueDate(charge.getTripDate())
                    .description(accountName)
                    .revenueType("CHARGE_ACCOUNT")
                    .revenueSubType("ACCOUNT_REVENUE")
                    .amount(totalAmount)
                    .pickupAddress(charge.getPickupAddress() != null ? charge.getPickupAddress() : "")
                    .dropoffAddress(charge.getDropoffAddress() != null ? charge.getDropoffAddress() : "")
                    .tipAmount(tipAmount)
                    .fareAmount(fareAmount)
                    .build();

                report.getRevenues().add(item);
                log.debug("Added account charge: {} - {} to {} (Fare: ${}, Tip: ${}, Total: ${})",
                    accountName, charge.getPickupAddress(), charge.getDropoffAddress(),
                    fareAmount, tipAmount, totalAmount);
            }
        } catch (Exception e) {
            log.error("Error fetching account charges for person {}: {}", personId, e.getMessage(), e);
        }

        // 7. Add lease revenue (for owners only - drivers renting their shifts)
        if (Boolean.TRUE.equals(person.getIsOwner()) && person.getDriverNumber() != null) {
            try {
                com.taxi.web.dto.report.LeaseRevenueReportDTO leaseReport =
                    driverFinancialCalculationService.calculateLeaseRevenue(
                        person.getDriverNumber(), from, to);

                log.info("Found {} lease revenue items for owner {}",
                    leaseReport.getLeaseItems() != null ? leaseReport.getLeaseItems().size() : 0, personId);

                if (leaseReport.getLeaseItems() != null && !leaseReport.getLeaseItems().isEmpty()) {
                    for (LeaseRevenueDTO leaseItem : leaseReport.getLeaseItems()) {
                        String description = "Lease - Cab " + leaseItem.getCabNumber() +
                            " (" + leaseItem.getShiftType() + ") - Driver: " + leaseItem.getDriverName() +
                            " (" + leaseItem.getDriverNumber() + ")";

                        // Get fixed lease amount (base rate)
                        BigDecimal fixedLeaseAmount = leaseItem.getBaseRate();

                        // Get driver and calculate mileage lease for this specific shift from actual mileage records
                        // The owner is the shift owner (the person this report is for)
                        Driver driver = driverRepository.findByDriverNumber(leaseItem.getDriverNumber())
                                .orElse(null);
                        BigDecimal mileageLeaseAmount = BigDecimal.ZERO;
                        if (driver != null) {
                            // Owner is the person we're generating the report for (from the shift ownership)
                            String ownerDriverNumber = person.getDriverNumber();

                            MileageCalculationResult result = calculateMileageLeaseForDay(driver, leaseItem.getLogonTime(), leaseItem.getLogoffTime(),
                                    ownerDriverNumber, leaseItem.getCabNumber(), leaseItem.getShiftType());
                            mileageLeaseAmount = result.mileageLease;
                            // Note: insuranceExpense (result.insuranceExpense) is deducted as driver expense, not owner revenue
                        }

                        // Total lease = fixed + mileage
                        BigDecimal totalLease = fixedLeaseAmount.add(mileageLeaseAmount);

                        // Create lease breakdown with both fixed and mileage components
                        StatementLineItem.LeaseBreakdown breakdown = StatementLineItem.LeaseBreakdown.builder()
                            .fixedLeaseAmount(fixedLeaseAmount)
                            .mileageLeaseAmount(mileageLeaseAmount)
                            .build();

                        report.getRevenues().add(OwnerReportDTO.RevenueLineItem.builder()
                            .categoryName("Lease Income")
                            .revenueDate(leaseItem.getShiftDate())
                            .description(description)
                            .revenueSubType("LEASE_INCOME")
                            .amount(totalLease)
                            .leaseBreakdown(breakdown)
                            .build());

                        log.debug("Added lease revenue: Cab {} Driver {} on {} = ${} (Fixed: ${}, Mileage: ${})",
                            leaseItem.getCabNumber(), leaseItem.getDriverNumber(), leaseItem.getShiftDate(),
                            totalLease, fixedLeaseAmount, mileageLeaseAmount);
                    }
                }
            } catch (Exception e) {
                log.warn("Error calculating lease revenue for owner {}: {}", personId, e.getMessage());
                // Don't fail the whole report if lease calculation fails
            }
        }

        // 8. Add revenues from OtherRevenue table (bonuses, credits, adjustments, etc.)
        // ✅ Uses new ApplicationType system: finds revenues applicable to this person
        // This includes: SPECIFIC_PERSON (exact match), ALL_DRIVERS, ALL_OWNERS, and ALL_ACTIVE_SHIFTS
        List<OtherRevenue> otherRevenues = otherRevenueRepository.findApplicableRevenuesBetween(personId, from, to);
        log.info("Found {} other revenues (legacy + ApplicationType system) for person {}", otherRevenues.size(), personId);

        for (OtherRevenue revenue : otherRevenues) {
            String revenueTypeStr = revenue.getRevenueType() != null ? revenue.getRevenueType().toString() : "OTHER";
            String description = revenue.getDescription() != null ? revenue.getDescription() : revenueTypeStr;

            // Resolve application type display name for context
            String applicationTypeDisplay = resolveApplicationTypeDisplay(revenue.getApplicationType());

            report.getRevenues().add(OwnerReportDTO.RevenueLineItem.builder()
                .categoryName("Other Revenue")
                .revenueDate(revenue.getRevenueDate())
                .description(description)
                .revenueType(revenueTypeStr)
                .revenueSubType("OTHER_REVENUE")
                .applicationTypeDisplay(applicationTypeDisplay)  // ✅ NEW: show how revenue applies
                .amount(revenue.getAmount())
                .build());
            log.debug("Added other revenue: {} (applicationType: {}, amount: {})", revenueTypeStr, applicationTypeDisplay, revenue.getAmount());
        }

        // 6. Fetch previous balance from last PAID statement only
        // (carryforward only happens when a batch is processed and completed)
        Optional<Statement> lastStatement = statementRepository.findTopByPersonIdAndStatusOrderByPeriodToDesc(personId, StatementStatus.PAID);
        if (lastStatement.isPresent()) {
            // Calculate what's actually owed AFTER the payment was applied
            // previousBalance = netDue - paidAmount (what remains unpaid)
            BigDecimal netDueAfterPayment = lastStatement.get().getNetDue()
                    .subtract(lastStatement.get().getPaidAmount() != null ? lastStatement.get().getPaidAmount() : BigDecimal.ZERO);

            // If fully paid or overpaid, don't carry forward a balance
            if (netDueAfterPayment.compareTo(BigDecimal.ZERO) <= 0) {
                report.setPreviousBalance(BigDecimal.ZERO);
                log.info("Previous statement for {} was fully paid (netDue={}, paid={}). No carryforward.",
                    personId, lastStatement.get().getNetDue(), lastStatement.get().getPaidAmount());
            } else {
                report.setPreviousBalance(netDueAfterPayment);
                log.info("Found previous PAID statement for {} with outstanding balance: {}",
                    personId, report.getPreviousBalance());
            }
        } else {
            report.setPreviousBalance(BigDecimal.ZERO);
            log.info("No previous PAID statement found for {} (carryforward only occurs after batch completion)", personId);
        }

        report.setPersonType(Boolean.TRUE.equals(person.getIsOwner()) ? "OWNER" : "DRIVER");
        report.setStatus(StatementStatus.DRAFT);
        report.setStatementId(null);  // Not yet finalized

        // Calculate total payments made during this period for this person
        List<com.taxi.domain.account.model.StatementPayment> paymentsInPeriod =
            statementPaymentRepository.findByPersonIdAndPaymentDateRange(personId, from, to);

        BigDecimal totalPaidInPeriod = BigDecimal.ZERO;
        for (com.taxi.domain.account.model.StatementPayment payment : paymentsInPeriod) {
            totalPaidInPeriod = totalPaidInPeriod.add(payment.getAmount());
        }

        report.setPaidAmount(totalPaidInPeriod);
        log.info("Found {} payments for person {} in period {} to {} totaling ${}",
            paymentsInPeriod.size(), personId, from, to, totalPaidInPeriod);

        // Calculate per-unit expenses (mileage, airport trips, etc.)
        Map<ItemRateChargedTo, List<OwnerReportDTO.PerUnitExpenseLineItem>> perUnitMap =
            calculatePerUnitExpenses(shiftLogs, from, to, person);

        // Add per-unit expenses relevant to this person (owner or driver)
        ItemRateChargedTo relevantKey = Boolean.TRUE.equals(person.getIsOwner())
            ? ItemRateChargedTo.OWNER
            : ItemRateChargedTo.DRIVER;

        List<OwnerReportDTO.PerUnitExpenseLineItem> relevantPerUnitExpenses =
            perUnitMap.getOrDefault(relevantKey, new ArrayList<>());

        report.setPerUnitExpenses(relevantPerUnitExpenses);

        if (!relevantPerUnitExpenses.isEmpty()) {
            BigDecimal totalPerUnit = relevantPerUnitExpenses.stream()
                .map(OwnerReportDTO.PerUnitExpenseLineItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            report.setTotalPerUnitExpenses(totalPerUnit);
            log.info("Added {} per-unit expenses for {} totaling ${}",
                relevantPerUnitExpenses.size(), relevantKey, totalPerUnit);
        } else {
            report.setTotalPerUnitExpenses(BigDecimal.ZERO);
        }

        report.calculateTotals();
        return report;
    }

    /**
     * Finalize a statement and save it to the database
     * This freezes the numbers for historical reference
     */
    @Transactional
    public Statement finalizeStatement(OwnerReportDTO report) {
        try {
            // Create line items list for serialization
            Map<String, Object> lineItems = new HashMap<>();
            lineItems.put("revenues", report.getRevenues());
            lineItems.put("recurringExpenses", report.getRecurringExpenses());
            lineItems.put("oneTimeExpenses", report.getOneTimeExpenses());
            lineItems.put("perUnitExpenses", report.getPerUnitExpenses());

            String lineItemsJson = objectMapper.writeValueAsString(lineItems);

            Statement statement = Statement.builder()
                .personId(report.getOwnerId())
                .personType(report.getPersonType())
                .personName(report.getOwnerName())
                .periodFrom(report.getPeriodFrom())
                .periodTo(report.getPeriodTo())
                .generatedDate(LocalDateTime.now())
                .totalRevenues(report.getTotalRevenues())
                .totalRecurringExpenses(report.getTotalRecurringExpenses())
                .totalOneTimeExpenses(report.getTotalOneTimeExpenses())
                .totalExpenses(report.getTotalExpenses())
                .previousBalance(report.getPreviousBalance())
                .paidAmount(report.getPaidAmount() != null ? report.getPaidAmount() : BigDecimal.ZERO)
                .netDue(report.getNetDue())
                .status(StatementStatus.FINALIZED)
                .lineItemsJson(lineItemsJson)
                .build();

            statement = statementRepository.save(statement);
            log.info("Finalized statement ID {} for person {} with net due {}", statement.getId(), statement.getPersonId(), statement.getNetDue());
            return statement;

        } catch (Exception e) {
            log.error("Error finalizing statement", e);
            throw new RuntimeException("Failed to finalize statement: " + e.getMessage(), e);
        }
    }

    /**
     * Resolve human-readable display name for application type
     */
    private String resolveApplicationTypeDisplay(ApplicationType applicationType) {
        if (applicationType == null) {
            return null;
        }

        return switch (applicationType) {
            case SPECIFIC_PERSON -> "Specific Person";
            case ALL_DRIVERS -> "All Drivers";
            case ALL_OWNERS -> "All Owners";
            case SPECIFIC_SHIFT -> "Specific Shift";
            case SHIFT_PROFILE -> "Shift Profile";
            case ALL_ACTIVE_SHIFTS -> "All Active Shifts";
            case SHIFTS_WITH_ATTRIBUTE -> "Shifts with Attribute";
        };
    }

    /**
     * Convert a saved Statement entity back to OwnerReportDTO
     * Used when loading an existing finalized or paid statement
     */
    public OwnerReportDTO convertStatementToReport(Statement statement) {
        try {
            // Deserialize line items from JSON
            com.fasterxml.jackson.databind.JsonNode lineItemsNode = objectMapper.readTree(statement.getLineItemsJson() != null ? statement.getLineItemsJson() : "{}");

            List<OwnerReportDTO.RevenueLineItem> revenues = new ArrayList<>();
            List<StatementLineItem> recurringExpenses = new ArrayList<>();
            List<StatementLineItem> oneTimeExpenses = new ArrayList<>();
            List<OwnerReportDTO.PerUnitExpenseLineItem> perUnitExpenses = new ArrayList<>();

            // Extract revenues
            if (lineItemsNode.has("revenues") && lineItemsNode.get("revenues").isArray()) {
                revenues = objectMapper.readValue(
                    lineItemsNode.get("revenues").toString(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, OwnerReportDTO.RevenueLineItem.class)
                );
            }

            // Extract recurring expenses
            if (lineItemsNode.has("recurringExpenses") && lineItemsNode.get("recurringExpenses").isArray()) {
                recurringExpenses = objectMapper.readValue(
                    lineItemsNode.get("recurringExpenses").toString(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, StatementLineItem.class)
                );
            }

            // Extract one-time expenses
            if (lineItemsNode.has("oneTimeExpenses") && lineItemsNode.get("oneTimeExpenses").isArray()) {
                oneTimeExpenses = objectMapper.readValue(
                    lineItemsNode.get("oneTimeExpenses").toString(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, StatementLineItem.class)
                );
            }

            // Extract per-unit expenses
            if (lineItemsNode.has("perUnitExpenses") && lineItemsNode.get("perUnitExpenses").isArray()) {
                perUnitExpenses = objectMapper.readValue(
                    lineItemsNode.get("perUnitExpenses").toString(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, OwnerReportDTO.PerUnitExpenseLineItem.class)
                );
            }

            // Get driver info
            Driver person = driverRepository.findById(statement.getPersonId())
                .orElseThrow(() -> new RuntimeException("Driver/Owner not found: " + statement.getPersonId()));

            // Build report DTO
            OwnerReportDTO report = OwnerReportDTO.builder()
                .ownerId(statement.getPersonId())
                .ownerName(statement.getPersonName())
                .ownerNumber(person.getDriverNumber())
                .periodFrom(statement.getPeriodFrom())
                .periodTo(statement.getPeriodTo())
                .statementId(statement.getId())
                .personType(statement.getPersonType())
                .status(statement.getStatus())
                .previousBalance(statement.getPreviousBalance())
                .paidAmount(statement.getPaidAmount())
                .totalRevenues(statement.getTotalRevenues())
                .totalRecurringExpenses(statement.getTotalRecurringExpenses())
                .totalOneTimeExpenses(statement.getTotalOneTimeExpenses())
                .totalExpenses(statement.getTotalExpenses())
                .revenues(revenues)
                .recurringExpenses(recurringExpenses)
                .oneTimeExpenses(oneTimeExpenses)
                .perUnitExpenses(perUnitExpenses)
                .build();

            // Calculate totals to set netDue
            report.calculateTotals();

            log.info("Converted statement ID {} to report for {} (status: {})",
                statement.getId(), statement.getPersonName(), statement.getStatus());

            return report;

        } catch (Exception e) {
            log.error("Error converting statement to report", e);
            throw new RuntimeException("Failed to convert statement to report: " + e.getMessage(), e);
        }
    }

    /**
     * Calculate mileage lease amount for a driver for a specific shift
     * Gets mileage record and calculates mileage lease amount
     * - Mileage Lease: dayMileage × mileageRate (from MILAGE_RATE ItemRate)
     *
     * Note: Insurance is now calculated separately in section 3.6 based on total period miles
     *
     * @param driver Driver to calculate mileage for
     * @param logonTime Shift logon time
     * @param logoffTime Shift logoff time
     * @param ownerDriverNumber Shift owner for override checking
     * @param cabNumber Cab number for override matching
     * @param shiftType Shift type (DAY/NIGHT) for override matching
     * @return MileageCalculationResult with dayMileage and mileageLease only
     */
    private MileageCalculationResult calculateMileageLeaseForDay(
            Driver driver, LocalDateTime logonTime, LocalDateTime logoffTime,
            String ownerDriverNumber, String cabNumber, String shiftType) {
        try {
            // Get all mileage records within the shift's time window (including 15-minute tolerance)
            List<MileageRecord> mileageRecords = mileageRecordRepository.findByDriverNumberAndShiftTimes(
                    driver.getDriverNumber(), logonTime, logoffTime);

            if (mileageRecords.isEmpty()) {
                log.debug("No mileage record found for driver {} within shift window {} to {} (±15min tolerance)",
                    driver.getDriverNumber(), logonTime, logoffTime);
                return new MileageCalculationResult(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
            }

            // Sum all mileage A (Flag fall / Tariff 1) from all captured mileage records
            BigDecimal dayMileage = mileageRecords.stream()
                    .map(MileageRecord::getMileageA)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (dayMileage.compareTo(BigDecimal.ZERO) <= 0) {
                log.debug("No mileage A recorded for driver {} during shift {} to {}",
                    driver.getDriverNumber(), logonTime, logoffTime);
                return new MileageCalculationResult(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
            }

            BigDecimal mileageLease = BigDecimal.ZERO;
            LocalDate shiftDate = logonTime.toLocalDate();
            String dayOfWeek = shiftDate.getDayOfWeek().toString();

            // ═══════════════════════════════════════════════════════════════════════
            // GET MILEAGE LEASE RATE
            // ═══════════════════════════════════════════════════════════════════════
            java.util.Optional<ItemRate> mileageRateOpt = itemRateRepository.findByName("MILEAGE_RATE");

            if (mileageRateOpt.isPresent()) {
                ItemRate baseMileageRate = mileageRateOpt.get();
                BigDecimal mileageRate = baseMileageRate.getRate();

                // Check for overrides
                if (ownerDriverNumber != null) {
                    List<ItemRateOverride> applicableOverrides = itemRateOverrideRepository
                        .findActiveOverridesForRate(baseMileageRate.getId(), ownerDriverNumber, shiftDate);

                    if (!applicableOverrides.isEmpty()) {
                        for (ItemRateOverride override : applicableOverrides) {
                            if (override.matches(cabNumber, shiftType, dayOfWeek)) {
                                mileageRate = override.getOverrideRate();
                                log.debug("Using mileage override rate ${}/mile for owner {} cab {} {} shift",
                                        mileageRate, ownerDriverNumber, cabNumber, shiftType);
                                break;
                            }
                        }
                    }
                }

                mileageLease = dayMileage.multiply(mileageRate)
                        .setScale(2, java.math.RoundingMode.HALF_UP);

                log.debug("Calculated mileage lease for driver {}: {} miles × ${}/mile = ${}",
                        driver.getDriverNumber(), dayMileage, mileageRate, mileageLease);
            } else {
                log.debug("MILAGE_RATE not found in item_rate table");
            }

            // Insurance is now calculated separately in section 3.6 based on total period miles
            return new MileageCalculationResult(dayMileage, mileageLease, BigDecimal.ZERO);

        } catch (Exception e) {
            log.error("Error calculating mileage/insurance for driver {} (shift {} to {})",
                driver.getDriverNumber(), logonTime, logoffTime, e);
            return new MileageCalculationResult(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }

    /**
     * Simple result object holding both mileage lease and insurance expense amounts
     * Both calculated from the same mileage using different rates
     */
    private static class MileageCalculationResult {
        public final BigDecimal dayMileage;         // Actual miles driven
        public final BigDecimal mileageLease;       // Mileage lease amount
        public final BigDecimal insuranceExpense;   // Insurance expense amount

        public MileageCalculationResult(BigDecimal dayMileage, BigDecimal mileageLease, BigDecimal insuranceExpense) {
            this.dayMileage = dayMileage;
            this.mileageLease = mileageLease;
            this.insuranceExpense = insuranceExpense;
        }
    }

}
