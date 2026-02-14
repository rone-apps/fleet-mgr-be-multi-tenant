package com.taxi.domain.expense.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxi.domain.cab.repository.CabRepository;
import com.taxi.domain.driver.model.Driver;
import com.taxi.domain.driver.repository.DriverRepository;
import com.taxi.domain.expense.model.ApplicationType;
import com.taxi.domain.expense.model.OneTimeExpense;
import com.taxi.domain.expense.model.RecurringExpense;
import com.taxi.domain.expense.repository.OneTimeExpenseRepository;
import com.taxi.domain.expense.repository.RecurringExpenseRepository;
import com.taxi.domain.revenue.entity.OtherRevenue;
import com.taxi.domain.revenue.repository.OtherRevenueRepository;
import com.taxi.domain.account.repository.AccountChargeRepository;
import com.taxi.domain.payment.repository.CreditCardTransactionRepository;
import com.taxi.domain.report.service.DriverFinancialCalculationService;
import com.taxi.web.dto.report.LeaseRevenueDTO;
import com.taxi.web.dto.report.LeaseExpenseDTO;
import com.taxi.domain.shift.model.CabShift;
import com.taxi.domain.shift.model.DriverSegment;
import com.taxi.domain.shift.model.ShiftLog;
import com.taxi.domain.shift.model.ShiftOwnership;
import com.taxi.domain.shift.repository.CabShiftRepository;
import com.taxi.domain.shift.repository.ShiftLogRepository;
import com.taxi.domain.shift.repository.ShiftOwnershipRepository;
import com.taxi.domain.statement.model.Statement;
import com.taxi.domain.statement.model.StatementStatus;
import com.taxi.domain.statement.repository.StatementRepository;
import com.taxi.web.dto.expense.DriverStatementDTO;
import com.taxi.web.dto.expense.OwnerReportDTO;
import com.taxi.web.dto.expense.StatementLineItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FinancialStatementService {

    private final RecurringExpenseRepository recurringExpenseRepository;
    private final OneTimeExpenseRepository oneTimeExpenseRepository;
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

    /**
     * Generate a financial statement for a driver for a date period
     * Shows all applicable recurring (prorated) and one-time charges
     */
    public DriverStatementDTO generateDriverStatement(Long driverId, LocalDate from, LocalDate to) {
        Driver driver = driverRepository.findById(driverId)
            .orElseThrow(() -> new RuntimeException("Driver not found: " + driverId));

        log.info("Generating driver statement for driver {} from {} to {}", driverId, from, to);

        DriverStatementDTO statement = DriverStatementDTO.builder()
            .driverId(driver.getId())
            .driverName(driver.getFullName())
            .driverNumber(driver.getDriverNumber())
            .periodFrom(from)
            .periodTo(to)
            .recurringCharges(new ArrayList<>())
            .oneTimeCharges(new ArrayList<>())
            .build();

        // Get all shifts where this driver is active during the period
        List<CabShift> activeShifts = cabShiftRepository.findByCurrentOwnerIdAndShiftActive(driverId, true);
        activeShifts = filterShiftsByDateRange(activeShifts, from, to);

        // 1. Get recurring expenses applicable to the driver's shifts
        List<RecurringExpense> shiftRecurringExpenses = getRecurringExpensesForShifts(activeShifts, from, to);
        for (RecurringExpense expense : shiftRecurringExpenses) {
            BigDecimal proratedAmount = expense.calculateAmountForDateRange(from, to);
            String entityDesc = buildEntityDescription(expense, activeShifts);

            statement.getRecurringCharges().add(StatementLineItem.builder()
                .categoryCode(expense.getExpenseCategory().getCategoryCode())
                .categoryName(expense.getExpenseCategory().getCategoryName())
                .applicationType(expense.getApplicationTypeEnum() != null ? expense.getApplicationTypeEnum().toString() : "")
                .entityDescription(entityDesc)
                .billingMethod(expense.getBillingMethod())
                .effectiveFrom(expense.getEffectiveFrom())
                .effectiveTo(expense.getEffectiveTo())
                .amount(proratedAmount)
                .isRecurring(true)
                .build());
        }

        // 2. Get driver-direct recurring expenses
        List<RecurringExpense> driverRecurringExpenses = getRecurringExpensesForDriver(driverId, from, to);
        for (RecurringExpense expense : driverRecurringExpenses) {
            BigDecimal proratedAmount = expense.calculateAmountForDateRange(from, to);

            statement.getRecurringCharges().add(StatementLineItem.builder()
                .categoryCode(expense.getExpenseCategory().getCategoryCode())
                .categoryName(expense.getExpenseCategory().getCategoryName())
                .applicationType(expense.getApplicationTypeEnum() != null ? expense.getApplicationTypeEnum().toString() : "")
                .entityDescription("Driver Specific")
                .billingMethod(expense.getBillingMethod())
                .effectiveFrom(expense.getEffectiveFrom())
                .effectiveTo(expense.getEffectiveTo())
                .amount(proratedAmount)
                .isRecurring(true)
                .build());
        }

        // 3. Get one-time expenses for the driver
        List<OneTimeExpense> oneTimeExpenses = oneTimeExpenseRepository.findForEntityBetween(
            OneTimeExpense.EntityType.DRIVER, driverId, from, to);

        for (OneTimeExpense expense : oneTimeExpenses) {
            statement.getOneTimeCharges().add(StatementLineItem.builder()
                .categoryCode(expense.getExpenseCategory().getCategoryCode())
                .categoryName(expense.getExpenseCategory().getCategoryName())
                .applicationType(expense.getApplicationType() != null ? expense.getApplicationType().toString() : "")
                .entityDescription("One-Time Charge")
                .date(expense.getExpenseDate())
                .description(expense.getDescription())
                .amount(expense.getAmount())
                .isRecurring(false)
                .build());
        }

        statement.calculateTotals();
        return statement;
    }

    /**
     * Generate a financial statement for an owner for a date period
     */
    public DriverStatementDTO generateOwnerStatement(Long ownerId, LocalDate from, LocalDate to) {
        Driver owner = driverRepository.findById(ownerId)
            .orElseThrow(() -> new RuntimeException("Owner not found: " + ownerId));

        if (!Boolean.TRUE.equals(owner.getIsOwner())) {
            throw new RuntimeException("Driver " + ownerId + " is not an owner");
        }

        log.info("Generating owner statement for owner {} from {} to {}", ownerId, from, to);

        DriverStatementDTO statement = DriverStatementDTO.builder()
            .driverId(owner.getId())
            .driverName(owner.getFullName())
            .driverNumber(owner.getDriverNumber())
            .periodFrom(from)
            .periodTo(to)
            .recurringCharges(new ArrayList<>())
            .oneTimeCharges(new ArrayList<>())
            .build();

        // Get all cabs owned by this owner
        List<Long> cabIds = cabRepository.findByOwnerDriver(owner).stream()
            .map(cab -> cab.getId())
            .toList();

        // Get all shifts for these cabs
        List<CabShift> ownerShifts = new ArrayList<>();
        for (Long cabId : cabIds) {
            ownerShifts.addAll(cabShiftRepository.findByCabId(cabId));
        }
        ownerShifts = filterShiftsByDateRange(ownerShifts, from, to);

        // 1. Get recurring expenses applicable to the owner's shifts
        List<RecurringExpense> shiftRecurringExpenses = getRecurringExpensesForShifts(ownerShifts, from, to);
        for (RecurringExpense expense : shiftRecurringExpenses) {
            BigDecimal proratedAmount = expense.calculateAmountForDateRange(from, to);
            String entityDesc = buildEntityDescription(expense, ownerShifts);

            statement.getRecurringCharges().add(StatementLineItem.builder()
                .categoryCode(expense.getExpenseCategory().getCategoryCode())
                .categoryName(expense.getExpenseCategory().getCategoryName())
                .applicationType(expense.getApplicationTypeEnum() != null ? expense.getApplicationTypeEnum().toString() : "")
                .entityDescription(entityDesc)
                .billingMethod(expense.getBillingMethod())
                .effectiveFrom(expense.getEffectiveFrom())
                .effectiveTo(expense.getEffectiveTo())
                .amount(proratedAmount)
                .isRecurring(true)
                .build());
        }

        // 2. Get owner-direct recurring expenses
        List<RecurringExpense> ownerRecurringExpenses = getRecurringExpensesForOwner(ownerId, from, to);
        for (RecurringExpense expense : ownerRecurringExpenses) {
            BigDecimal proratedAmount = expense.calculateAmountForDateRange(from, to);

            statement.getRecurringCharges().add(StatementLineItem.builder()
                .categoryCode(expense.getExpenseCategory().getCategoryCode())
                .categoryName(expense.getExpenseCategory().getCategoryName())
                .applicationType(expense.getApplicationTypeEnum() != null ? expense.getApplicationTypeEnum().toString() : "")
                .entityDescription("Owner Specific")
                .billingMethod(expense.getBillingMethod())
                .effectiveFrom(expense.getEffectiveFrom())
                .effectiveTo(expense.getEffectiveTo())
                .amount(proratedAmount)
                .isRecurring(true)
                .build());
        }

        // 3. Get one-time expenses for the owner
        List<OneTimeExpense> oneTimeExpenses = oneTimeExpenseRepository.findForEntityBetween(
            OneTimeExpense.EntityType.OWNER, ownerId, from, to);

        for (OneTimeExpense expense : oneTimeExpenses) {
            statement.getOneTimeCharges().add(StatementLineItem.builder()
                .categoryCode(expense.getExpenseCategory().getCategoryCode())
                .categoryName(expense.getExpenseCategory().getCategoryName())
                .applicationType(expense.getApplicationType() != null ? expense.getApplicationType().toString() : "")
                .entityDescription("One-Time Charge")
                .date(expense.getExpenseDate())
                .description(expense.getDescription())
                .amount(expense.getAmount())
                .isRecurring(false)
                .build());
        }

        statement.calculateTotals();
        return statement;
    }

    private List<RecurringExpense> getRecurringExpensesForShifts(List<CabShift> shifts, LocalDate from, LocalDate to) {
        List<RecurringExpense> expenses = new ArrayList<>();

        if (shifts.isEmpty()) {
            log.info("No shifts found for expense calculation");
            return expenses;
        }

        log.info("Calculating expenses for {} shifts between {} and {}", shifts.size(), from, to);

        // ALL_ACTIVE_SHIFTS - applies to all active shifts
        List<RecurringExpense> allActiveShiftsExpenses = recurringExpenseRepository.findEffectiveBetweenForApplicationType(
            ApplicationType.ALL_ACTIVE_SHIFTS, from, to);
        expenses.addAll(allActiveShiftsExpenses);
        log.info("Found {} ALL_ACTIVE_SHIFTS expenses", allActiveShiftsExpenses.size());
        allActiveShiftsExpenses.forEach(e -> log.info("  - ALL_ACTIVE: {} (amount: {})", e.getExpenseCategory().getCategoryName(), e.getAmount()));

        // Collect shift profile IDs from shifts
        Set<Long> shiftProfileIds = new HashSet<>();
        for (CabShift shift : shifts) {
            if (shift.getCurrentProfile() != null) {
                shiftProfileIds.add(shift.getCurrentProfile().getId());
                log.info("Shift {} has profile {}", shift.getId(), shift.getCurrentProfile().getId());
            }
        }
        log.info("Found {} unique shift profiles", shiftProfileIds.size());

        // SHIFT_PROFILE - expenses for specific profiles
        for (Long profileId : shiftProfileIds) {
            List<RecurringExpense> profileExpenses = recurringExpenseRepository.findByApplicationTypeAndShiftProfileIdBetween(
                ApplicationType.SHIFT_PROFILE, profileId, from, to);
            expenses.addAll(profileExpenses);
            log.info("Found {} SHIFT_PROFILE expenses for profile {}", profileExpenses.size(), profileId);
            profileExpenses.forEach(e -> log.info("  - PROFILE {}: {} (amount: {})", profileId, e.getExpenseCategory().getCategoryName(), e.getAmount()));
        }

        // SPECIFIC_SHIFT - expenses for specific shifts
        for (CabShift shift : shifts) {
            List<RecurringExpense> shiftExpenses = recurringExpenseRepository.findByApplicationTypeAndSpecificShiftIdBetween(
                ApplicationType.SPECIFIC_SHIFT, shift.getId(), from, to);
            expenses.addAll(shiftExpenses);
            log.info("Found {} SPECIFIC_SHIFT expenses for shift {}", shiftExpenses.size(), shift.getId());
        }

        log.info("Total expenses before deduplication: {}", expenses.size());
        List<RecurringExpense> result = expenses.stream().distinct().toList();
        log.info("Total expenses after deduplication: {}", result.size());

        return result;
    }

    private List<RecurringExpense> getRecurringExpensesForDriver(Long driverId, LocalDate from, LocalDate to) {
        List<RecurringExpense> expenses = new ArrayList<>();

        // SPECIFIC_OWNER_DRIVER where specificDriverId = driverId
        expenses.addAll(recurringExpenseRepository.findByApplicationTypeAndSpecificDriverIdBetween(
            ApplicationType.SPECIFIC_OWNER_DRIVER, driverId, from, to));

        // ALL_NON_OWNER_DRIVERS if driver is not an owner
        Driver driver = driverRepository.findById(driverId).orElse(null);
        if (driver != null && !Boolean.TRUE.equals(driver.getIsOwner())) {
            expenses.addAll(recurringExpenseRepository.findEffectiveBetweenForApplicationType(
                ApplicationType.ALL_NON_OWNER_DRIVERS, from, to));
        }

        return expenses;
    }

    private List<RecurringExpense> getRecurringExpensesForOwner(Long ownerId, LocalDate from, LocalDate to) {
        List<RecurringExpense> expenses = new ArrayList<>();

        // SPECIFIC_OWNER_DRIVER where specificOwnerId = ownerId
        expenses.addAll(recurringExpenseRepository.findByApplicationTypeAndSpecificOwnerIdBetween(
            ApplicationType.SPECIFIC_OWNER_DRIVER, ownerId, from, to));

        return expenses;
    }

    private List<CabShift> filterShiftsByDateRange(List<CabShift> shifts, LocalDate from, LocalDate to) {
        return shifts.stream()
            .filter(shift -> shift.isActiveOn(from) || shift.isActiveOn(to))
            .toList();
    }

    private String buildEntityDescription(RecurringExpense expense, List<CabShift> shifts) {
        ApplicationType appType = expense.getApplicationTypeEnum();

        if (appType == null) {
            return "Unknown";
        }

        return switch (appType) {
            case ALL_ACTIVE_SHIFTS -> "All Active Shifts";
            case SHIFT_PROFILE -> "Profile: " + (expense.getShiftProfileId() != null ? expense.getShiftProfileId() : "N/A");
            case SPECIFIC_SHIFT -> {
                CabShift shift = shifts.stream()
                    .filter(s -> s.getId().equals(expense.getSpecificShiftId()))
                    .findFirst()
                    .orElse(null);
                yield shift != null ? "Cab " + shift.getCab().getCabNumber() + " - " + shift.getShiftType() : "Specific Shift";
            }
            case SPECIFIC_OWNER_DRIVER -> expense.getSpecificOwnerId() != null ? "Owner Specific" : "Driver Specific";
            case ALL_NON_OWNER_DRIVERS -> "All Non-Owner Drivers";
            case SHIFTS_WITH_ATTRIBUTE -> "Shifts with Attribute";
        };
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
            // For drivers: get all ACTIVE shifts where they are the current owner
            relevantShifts = cabShiftRepository.findActiveByOwnerId(personId);
            relevantShifts = filterShiftsByDateRange(relevantShifts, from, to);
            log.info("Driver {} has {} active shifts in period", personId, relevantShifts.size());
        }

        // 1. Add ALL_ACTIVE_SHIFTS expenses (once, applies to all shifts)
        List<RecurringExpense> allActiveExpenses = recurringExpenseRepository.findEffectiveBetweenForApplicationType(
            ApplicationType.ALL_ACTIVE_SHIFTS, from, to);

        for (RecurringExpense expense : allActiveExpenses) {
            BigDecimal proratedAmount = expense.calculateAmountForDateRange(from, to);
            log.info("Adding ALL_ACTIVE_SHIFTS expense {} (prorated: {})",
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

        // 2. Add SHIFT_PROFILE and SPECIFIC_SHIFT expenses (per shift)
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
        }

        // 2. Add person-specific recurring expenses
        List<RecurringExpense> personRecurringExpenses;
        if (Boolean.TRUE.equals(person.getIsOwner())) {
            personRecurringExpenses = getRecurringExpensesForOwner(personId, from, to);
        } else {
            personRecurringExpenses = getRecurringExpensesForDriver(personId, from, to);
        }

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

        // 3. Add one-time expenses
        OneTimeExpense.EntityType entityType = Boolean.TRUE.equals(person.getIsOwner())
            ? OneTimeExpense.EntityType.OWNER
            : OneTimeExpense.EntityType.DRIVER;
        List<OneTimeExpense> oneTimeExpenses = oneTimeExpenseRepository.findForEntityBetween(
            entityType, personId, from, to);

        for (OneTimeExpense expense : oneTimeExpenses) {
            report.getOneTimeExpenses().add(StatementLineItem.builder()
                .categoryCode(expense.getExpenseCategory().getCategoryCode())
                .categoryName(expense.getExpenseCategory().getCategoryName())
                .applicationType(expense.getApplicationType() != null ? expense.getApplicationType().toString() : "")
                .entityDescription("One-Time Charge")
                .date(expense.getExpenseDate())
                .description(expense.getDescription())
                .amount(expense.getAmount())
                .isRecurring(false)
                .build());
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
                            " (" + leaseItem.getShiftType() + ") - Owner: " + leaseItem.getOwnerDriverName();

                        // Show breakdown: Base rate + Mileage
                        String entityDesc = "Base: $" + leaseItem.getBaseRate() +
                            " + Mileage: $" + leaseItem.getMileageLease() +
                            " (" + leaseItem.getMiles() + "mi @ $" + leaseItem.getMileageRate() + "/mi)";

                        report.getOneTimeExpenses().add(StatementLineItem.builder()
                            .categoryCode(categoryCode)
                            .categoryName(categoryName)
                            .applicationType("LEASE_RENT")
                            .entityDescription(entityDesc)
                            .date(leaseItem.getShiftDate())
                            .description(description)
                            .amount(leaseItem.getTotalLease())
                            .isRecurring(false)
                            .build());

                        log.debug("Added lease expense: Cab {} Owner {} = ${}",
                            leaseItem.getCabNumber(), leaseItem.getOwnerDriverNumber(), leaseItem.getTotalLease());
                    }
                }
            } catch (Exception e) {
                log.warn("Error calculating lease expense for driver {}: {}", personId, e.getMessage());
                // Don't fail the whole report if lease calculation fails
            }
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
                            " (" + leaseItem.getShiftType() + ") - Driver: " + leaseItem.getDriverName();

                        // Show breakdown: Base rate + Mileage
                        String details = "Base: $" + leaseItem.getBaseRate() +
                            " + Mileage: $" + leaseItem.getMileageLease() +
                            " (" + leaseItem.getMiles() + "mi @ $" + leaseItem.getMileageRate() + "/mi)";

                        report.getRevenues().add(OwnerReportDTO.RevenueLineItem.builder()
                            .categoryName("Lease Income")
                            .revenueDate(leaseItem.getShiftDate())
                            .description(description)
                            .revenueType(details)
                            .revenueSubType("LEASE_INCOME")
                            .amount(leaseItem.getTotalLease())
                            .build());

                        log.debug("Added lease revenue: Cab {} Driver {} = ${}",
                            leaseItem.getCabNumber(), leaseItem.getDriverNumber(), leaseItem.getTotalLease());
                    }
                }
            } catch (Exception e) {
                log.warn("Error calculating lease revenue for owner {}: {}", personId, e.getMessage());
                // Don't fail the whole report if lease calculation fails
            }
        }

        // 8. Add revenues from OtherRevenue table (bonuses, credits, adjustments, etc.)
        List<OtherRevenue> otherRevenues = otherRevenueRepository.findForDriverBetweenDates(personId, from, to);
        log.info("Found {} other revenues for person {}", otherRevenues.size(), personId);

        for (OtherRevenue revenue : otherRevenues) {
            String revenueTypeStr = revenue.getRevenueType() != null ? revenue.getRevenueType().toString() : "OTHER";
            String description = revenue.getDescription() != null ? revenue.getDescription() : revenueTypeStr;
            report.getRevenues().add(OwnerReportDTO.RevenueLineItem.builder()
                .categoryName("Other Revenue")
                .revenueDate(revenue.getRevenueDate())
                .description(description)
                .revenueType(revenueTypeStr)
                .revenueSubType("OTHER_REVENUE")
                .amount(revenue.getAmount())
                .build());
            log.debug("Added other revenue: {}", revenue.getAmount());
        }

        // 6. Fetch previous balance from last finalized statement
        Optional<Statement> lastStatement = statementRepository.findTopByPersonIdAndStatusOrderByPeriodToDesc(personId, StatementStatus.FINALIZED);
        if (lastStatement.isPresent()) {
            report.setPreviousBalance(lastStatement.get().getNetDue());
            log.info("Found previous statement for {}: previous balance = {}", personId, report.getPreviousBalance());
        } else {
            report.setPreviousBalance(BigDecimal.ZERO);
            log.info("No previous finalized statement found for {}", personId);
        }

        report.setPersonType(Boolean.TRUE.equals(person.getIsOwner()) ? "OWNER" : "DRIVER");
        report.setStatus(StatementStatus.DRAFT);
        report.setStatementId(null);  // Not yet finalized

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
}
