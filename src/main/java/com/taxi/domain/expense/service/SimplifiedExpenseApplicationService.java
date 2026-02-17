package com.taxi.domain.expense.service;

import com.taxi.domain.driver.model.Driver;
import com.taxi.domain.driver.repository.DriverRepository;
import com.taxi.domain.expense.model.ApplicationType;
import com.taxi.domain.expense.model.ExpenseCategory;
import com.taxi.domain.expense.model.RecurringExpense;
import com.taxi.domain.expense.model.RecurringExpense.EntityType;
import com.taxi.domain.expense.repository.RecurringExpenseRepository;
import com.taxi.domain.shift.model.CabShift;
import com.taxi.domain.shift.repository.CabShiftRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SimplifiedExpenseApplicationService - Handles creating expenses based on simplified application types
 *
 * Replaces complex attribute-based matching with straightforward application types:
 * - SHIFT_PROFILE: All shifts with a specific profile
 * - SPECIFIC_SHIFT: One specific shift
 * - SPECIFIC_PERSON: A specific owner or driver
 * - ALL_OWNERS: All currently active shifts
 * - ALL_DRIVERS: All drivers who don't own shifts
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SimplifiedExpenseApplicationService {

    private final CabShiftRepository cabShiftRepository;
    private final DriverRepository driverRepository;
    private final RecurringExpenseRepository recurringExpenseRepository;

    /**
     * Create expenses for a category based on its application type
     *
     * @param category the expense category with application type and target entity
     * @param amount the expense amount
     * @param billingMethod how to bill (MONTHLY, DAILY, PER_SHIFT)
     * @param effectiveFrom when the expense starts
     * @param createdBy user who created the expense
     * @return list of created recurring expenses
     */
    public List<RecurringExpense> createExpensesForCategory(
            ExpenseCategory category,
            BigDecimal amount,
            RecurringExpense.BillingMethod billingMethod,
            LocalDate effectiveFrom,
            String createdBy) {

        if (category.getApplicationType() == null) {
            throw new IllegalArgumentException("Category must have an application type");
        }

        List<RecurringExpense> createdExpenses = new ArrayList<>();

        try {
            switch (category.getApplicationType()) {
                case SHIFT_PROFILE:
                    createdExpenses = createExpensesForShiftProfile(
                            category, amount, billingMethod, effectiveFrom, createdBy);
                    break;

                case SPECIFIC_SHIFT:
                    RecurringExpense shiftExpense = createExpenseForShift(
                            category, category.getSpecificShiftId(), amount, billingMethod, effectiveFrom, createdBy);
                    if (shiftExpense != null) {
                        createdExpenses.add(shiftExpense);
                    }
                    break;

                case SPECIFIC_PERSON:
                    RecurringExpense ownerDriverExpense = createExpenseForOwnerOrDriver(
                            category, amount, billingMethod, effectiveFrom, createdBy);
                    if (ownerDriverExpense != null) {
                        createdExpenses.add(ownerDriverExpense);
                    }
                    break;

                case ALL_OWNERS:
                    createdExpenses = createExpensesForAllActiveShifts(
                            category, amount, billingMethod, effectiveFrom, createdBy);
                    break;

                case ALL_DRIVERS:
                    createdExpenses = createExpensesForNonOwnerDrivers(
                            category, amount, billingMethod, effectiveFrom, createdBy);
                    break;
            }

            log.info("Successfully created {} expenses for category: {}",
                    createdExpenses.size(), category.getCategoryCode());

        } catch (Exception e) {
            log.error("Error creating expenses for category: " + category.getCategoryCode(), e);
            throw new RuntimeException("Failed to create expenses: " + e.getMessage(), e);
        }

        return createdExpenses;
    }

    /**
     * Get preview of how many entities will receive the expense
     */
    public int previewApplicationCount(ExpenseCategory category) {
        switch (category.getApplicationType()) {
            case SHIFT_PROFILE:
                return (int) cabShiftRepository.findActiveByCurrentProfileId(category.getShiftProfileId())
                        .stream().count();

            case SPECIFIC_SHIFT:
                return 1;  // Always 1 for specific shift

            case SPECIFIC_PERSON:
                return 1;  // Always 1 for specific owner or driver

            case ALL_OWNERS:
                return (int) cabShiftRepository.findAllActiveShifts().stream().count();

            case ALL_DRIVERS:
                return (int) driverRepository.findNonOwnerDrivers().stream().count();

            default:
                return 0;
        }
    }

    /**
     * Create expenses for all shifts with a specific profile
     */
    private List<RecurringExpense> createExpensesForShiftProfile(
            ExpenseCategory category,
            BigDecimal amount,
            RecurringExpense.BillingMethod billingMethod,
            LocalDate effectiveFrom,
            String createdBy) {

        List<CabShift> shiftsWithProfile = cabShiftRepository.findActiveByCurrentProfileId(
                category.getShiftProfileId());

        log.info("Creating expenses for {} shifts with profile ID: {}",
                shiftsWithProfile.size(), category.getShiftProfileId());

        return shiftsWithProfile.stream()
                .map(shift -> createExpenseForShift(category, shift.getId(), amount, billingMethod, effectiveFrom, createdBy))
                .filter(exp -> exp != null)
                .collect(Collectors.toList());
    }

    /**
     * Create expense for a specific shift
     */
    private RecurringExpense createExpenseForShift(
            ExpenseCategory category,
            Long shiftId,
            BigDecimal amount,
            RecurringExpense.BillingMethod billingMethod,
            LocalDate effectiveFrom,
            String createdBy) {

        try {
            CabShift shift = cabShiftRepository.findById(shiftId)
                    .orElseThrow(() -> new RuntimeException("Shift not found: " + shiftId));

            // Check if expense already exists
            if (recurringExpenseRepository.existsActiveByCabAndCategory(shift.getCab().getId(), category.getId())) {
                log.warn("Expense already exists for shift {} and category {}", shiftId, category.getCategoryCode());
                return null;
            }

            RecurringExpense expense = RecurringExpense.builder()
                    .expenseCategory(category)
                    .entityType(EntityType.SHIFT)
                    .entityId(shift.getId())
                    .shiftType(shift.getShiftType())
                    .amount(amount)
                    .billingMethod(billingMethod)
                    .effectiveFrom(effectiveFrom)
                    .isActive(true)
                    .isAutoGenerated(false)  // Explicitly created by user
                    .build();

            RecurringExpense saved = recurringExpenseRepository.save(expense);
            log.debug("Created expense for shift: {}", shiftId);
            return saved;

        } catch (Exception e) {
            log.error("Error creating expense for shift: " + shiftId, e);
            throw new RuntimeException("Error creating expense for shift: " + e.getMessage(), e);
        }
    }

    /**
     * Create expenses for all currently active shifts
     */
    private List<RecurringExpense> createExpensesForAllActiveShifts(
            ExpenseCategory category,
            BigDecimal amount,
            RecurringExpense.BillingMethod billingMethod,
            LocalDate effectiveFrom,
            String createdBy) {

        List<CabShift> activeShifts = cabShiftRepository.findAllActiveShifts();

        log.info("Creating expenses for {} active shifts", activeShifts.size());

        return activeShifts.stream()
                .map(shift -> createExpenseForShift(category, shift.getId(), amount, billingMethod, effectiveFrom, createdBy))
                .filter(exp -> exp != null)
                .collect(Collectors.toList());
    }

    /**
     * Create expenses for all non-owner drivers
     */
    private List<RecurringExpense> createExpensesForNonOwnerDrivers(
            ExpenseCategory category,
            BigDecimal amount,
            RecurringExpense.BillingMethod billingMethod,
            LocalDate effectiveFrom,
            String createdBy) {

        List<Driver> nonOwnerDrivers = driverRepository.findNonOwnerDrivers();

        log.info("Creating expenses for {} non-owner drivers", nonOwnerDrivers.size());

        return nonOwnerDrivers.stream()
                .map(driver -> createExpenseForDriver(category, driver.getId(), amount, billingMethod, effectiveFrom, createdBy))
                .filter(exp -> exp != null)
                .collect(Collectors.toList());
    }

    /**
     * Create expense for a specific driver
     */
    private RecurringExpense createExpenseForDriver(
            ExpenseCategory category,
            Long driverId,
            BigDecimal amount,
            RecurringExpense.BillingMethod billingMethod,
            LocalDate effectiveFrom,
            String createdBy) {

        try {
            Driver driver = driverRepository.findById(driverId)
                    .orElseThrow(() -> new RuntimeException("Driver not found: " + driverId));

            RecurringExpense expense = RecurringExpense.builder()
                    .expenseCategory(category)
                    .entityType(EntityType.DRIVER)
                    .entityId(driver.getId())
                    .amount(amount)
                    .billingMethod(billingMethod)
                    .effectiveFrom(effectiveFrom)
                    .isActive(true)
                    .isAutoGenerated(false)
                    .build();

            RecurringExpense saved = recurringExpenseRepository.save(expense);
            log.debug("Created expense for driver: {}", driverId);
            return saved;

        } catch (Exception e) {
            log.error("Error creating expense for driver: " + driverId, e);
            throw new RuntimeException("Error creating expense for driver: " + e.getMessage(), e);
        }
    }

    /**
     * Create expense for owner or driver based on which is set
     */
    private RecurringExpense createExpenseForOwnerOrDriver(
            ExpenseCategory category,
            BigDecimal amount,
            RecurringExpense.BillingMethod billingMethod,
            LocalDate effectiveFrom,
            String createdBy) {

        if (category.getSpecificPersonId() != null) {
            return createExpenseForDriver(category, category.getSpecificPersonId(), amount, billingMethod, effectiveFrom, createdBy);
        } else {
            throw new IllegalArgumentException("Person ID (driver or owner) is not set for SPECIFIC_PERSON type");
        }
    }
}
