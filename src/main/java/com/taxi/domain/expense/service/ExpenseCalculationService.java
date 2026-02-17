package com.taxi.domain.expense.service;

import com.taxi.domain.cab.repository.CabAttributeValueRepository;
import com.taxi.domain.driver.model.Driver;
import com.taxi.domain.driver.repository.DriverRepository;
import com.taxi.domain.expense.model.OneTimeExpense;
import com.taxi.domain.expense.model.RecurringExpense;
import com.taxi.domain.expense.repository.OneTimeExpenseRepository;
import com.taxi.domain.expense.repository.RecurringExpenseRepository;
import com.taxi.domain.shift.model.CabShift;
import com.taxi.domain.shift.repository.CabShiftRepository;
import com.taxi.web.dto.expense.DriverStatementDTO;
import com.taxi.web.dto.expense.StatementLineItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * SINGLE SOURCE OF TRUTH: ALL EXPENSE CALCULATIONS
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * Centralized service for calculating all types of expenses:
 * - Recurring expenses (daily/weekly/monthly charges)
 * - One-time expenses (ad-hoc charges)
 *
 * KEY BUSINESS RULE: ALL_ACTIVE_SHIFTS EXPANSION
 * When an expense has applicationType = ALL_ACTIVE_SHIFTS:
 * - For OWNERS: Expand to one charge per active shift
 * - For DRIVERS: Not applicable (drivers don't get shift-based charges)
 *
 * UNIFIED EXPENSE PROCESSING:
 * - All expense queries go through this service
 * - Single maintenance point for expense logic
 * - Consistent handling of ALL_ACTIVE_SHIFTS expansion
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ExpenseCalculationService {

    private final RecurringExpenseRepository recurringExpenseRepository;
    private final OneTimeExpenseRepository oneTimeExpenseRepository;
    private final DriverRepository driverRepository;
    private final CabShiftRepository cabShiftRepository;
    private final CabAttributeValueRepository cabAttributeValueRepository;

    /**
     * Calculate all recurring expenses for a person (driver or owner)
     * Includes shift-based recurring expenses if applicable
     */
    public List<RecurringExpense> getApplicableRecurringExpenses(
            Driver person,
            List<CabShift> personShifts,
            LocalDate from,
            LocalDate to) {

        log.info("Calculating recurring expenses for person {} (isOwner={}) from {} to {}",
                person.getId(), person.getIsOwner(), from, to);

        List<RecurringExpense> allExpenses = new ArrayList<>();
        boolean isOwner = Boolean.TRUE.equals(person.getIsOwner());

        // 1. Get shift-based recurring expenses (SHIFT_PROFILE, SPECIFIC_SHIFT, ALL_ACTIVE_SHIFTS)
        List<RecurringExpense> shiftExpenses = getRecurringExpensesForShifts(personShifts, from, to);
        allExpenses.addAll(shiftExpenses);

        // 2. Get person-direct recurring expenses (SPECIFIC_PERSON, ALL_OWNERS, ALL_DRIVERS)
        List<RecurringExpense> personExpenses = getRecurringExpensesForPerson(person.getId(), isOwner, from, to);
        allExpenses.addAll(personExpenses);

        log.info("Total recurring expenses for person {}: {}", person.getId(), allExpenses.size());
        return allExpenses;
    }

    /**
     * Calculate all one-time expenses for a person (driver or owner)
     * Includes application-type-based targeting
     */
    public List<OneTimeExpense> getApplicableOneTimeExpenses(
            Driver person,
            List<CabShift> personShifts,
            LocalDate from,
            LocalDate to) {

        log.info("Calculating one-time expenses for person {} (isOwner={}) from {} to {}",
                person.getId(), person.getIsOwner(), from, to);

        List<OneTimeExpense> allExpenses = new ArrayList<>();
        boolean isOwner = Boolean.TRUE.equals(person.getIsOwner());

        // Get applicable one-time expenses (includes ALL_ACTIVE_SHIFTS from query)
        List<OneTimeExpense> expenses = oneTimeExpenseRepository.findApplicableExpensesBetween(
                person.getId(), isOwner, from, to);

        log.info("Found {} applicable one-time expenses for person {}", expenses.size(), person.getId());
        allExpenses.addAll(expenses);

        // Add shift-profile, shift-specific, and attribute-based expenses (only for owners)
        if (isOwner && !personShifts.isEmpty()) {
            List<Long> profileIds = personShifts.stream()
                    .filter(s -> s.getCurrentProfile() != null)
                    .map(s -> s.getCurrentProfile().getId())
                    .distinct()
                    .toList();

            if (!profileIds.isEmpty()) {
                List<OneTimeExpense> profileExpenses = oneTimeExpenseRepository
                        .findByShiftProfileIdsBetween(profileIds, from, to);
                allExpenses.addAll(profileExpenses);
                log.info("Added {} SHIFT_PROFILE one-time expenses", profileExpenses.size());
            }

            List<Long> shiftIds = personShifts.stream()
                    .map(CabShift::getId)
                    .distinct()
                    .toList();

            if (!shiftIds.isEmpty()) {
                List<OneTimeExpense> shiftExpenses = oneTimeExpenseRepository
                        .findBySpecificShiftIdsBetween(shiftIds, from, to);
                allExpenses.addAll(shiftExpenses);
                log.info("Added {} SPECIFIC_SHIFT one-time expenses", shiftExpenses.size());

                // ✅ NEW: Add SHIFTS_WITH_ATTRIBUTE expenses (charges for shifts with specific attributes)
                // For each shift, get its currently active attributes and find expenses for those attributes
                Set<Long> attributeTypeIds = new java.util.HashSet<>();
                for (CabShift shift : personShifts) {
                    var shiftAttributes = cabAttributeValueRepository.findCurrentAttributesByShiftId(shift.getId());
                    shiftAttributes.forEach(attr -> attributeTypeIds.add(attr.getAttributeType().getId()));
                }

                if (!attributeTypeIds.isEmpty()) {
                    List<OneTimeExpense> attributeExpenses = oneTimeExpenseRepository
                            .findByAttributeTypeIdsBetween(new ArrayList<>(attributeTypeIds), from, to);
                    allExpenses.addAll(attributeExpenses);
                    log.info("Added {} SHIFTS_WITH_ATTRIBUTE one-time expenses for {} attribute types",
                            attributeExpenses.size(), attributeTypeIds.size());
                }
            }
        }

        log.info("Total one-time expenses for person {}: {}", person.getId(), allExpenses.size());
        return allExpenses;
    }

    /**
     * Add recurring expense line items to statement
     * Handles ALL_ACTIVE_SHIFTS expansion (one line per shift)
     */
    public void addRecurringExpensesToStatement(
            DriverStatementDTO statement,
            List<RecurringExpense> expenses,
            List<CabShift> personShifts) {

        for (RecurringExpense expense : expenses) {
            BigDecimal proratedAmount = expense.calculateAmountForDateRange(
                    statement.getPeriodFrom(), statement.getPeriodTo());

            // Special handling for ALL_ACTIVE_SHIFTS: expand per shift
            if (expense.getApplicationTypeEnum() != null &&
                    expense.getApplicationTypeEnum().name().equals("ALL_ACTIVE_SHIFTS") &&
                    !personShifts.isEmpty()) {

                log.info("EXPANDING ALL_ACTIVE_SHIFTS recurring expense ID {} to {} shifts",
                        expense.getId(), personShifts.size());

                for (CabShift shift : personShifts) {
                    statement.getRecurringCharges().add(StatementLineItem.builder()
                            .categoryCode(expense.getExpenseCategory().getCategoryCode())
                            .categoryName(expense.getExpenseCategory().getCategoryName())
                            .applicationType("ALL_ACTIVE_SHIFTS")
                            .entityDescription("Shift: Cab " + shift.getCab().getCabNumber() +
                                    " - " + shift.getShiftType())
                            .billingMethod(expense.getBillingMethod())
                            .effectiveFrom(expense.getEffectiveFrom())
                            .effectiveTo(expense.getEffectiveTo())
                            .amount(proratedAmount)
                            .isRecurring(true)
                            .build());
                }
            } else {
                // Standard handling for non-ALL_ACTIVE_SHIFTS
                statement.getRecurringCharges().add(StatementLineItem.builder()
                        .categoryCode(expense.getExpenseCategory().getCategoryCode())
                        .categoryName(expense.getExpenseCategory().getCategoryName())
                        .applicationType(expense.getApplicationTypeEnum() != null ?
                                expense.getApplicationTypeEnum().toString() : "")
                        .entityDescription("Expense Charge")
                        .billingMethod(expense.getBillingMethod())
                        .effectiveFrom(expense.getEffectiveFrom())
                        .effectiveTo(expense.getEffectiveTo())
                        .amount(proratedAmount)
                        .isRecurring(true)
                        .build());
            }
        }
    }

    /**
     * Add one-time expense line items to statement
     * Handles ALL_ACTIVE_SHIFTS and SHIFTS_WITH_ATTRIBUTE expansion (one line per shift)
     */
    public void addOneTimeExpensesToStatement(
            DriverStatementDTO statement,
            List<OneTimeExpense> expenses,
            List<CabShift> personShifts) {

        for (OneTimeExpense expense : expenses) {
            String categoryCode = expense.getExpenseCategory() != null
                    ? expense.getExpenseCategory().getCategoryCode()
                    : "AD_HOC";
            String categoryName = expense.getExpenseCategory() != null
                    ? expense.getExpenseCategory().getCategoryName()
                    : (expense.getName() != null ? expense.getName() : "Other Charge");

            // Special handling for ALL_ACTIVE_SHIFTS: expand per shift
            if (expense.getApplicationType() != null &&
                    expense.getApplicationType().name().equals("ALL_ACTIVE_SHIFTS") &&
                    !personShifts.isEmpty()) {

                log.info("EXPANDING ALL_ACTIVE_SHIFTS one-time expense ID {} to {} shifts",
                        expense.getId(), personShifts.size());

                for (CabShift shift : personShifts) {
                    statement.getOneTimeCharges().add(StatementLineItem.builder()
                            .categoryCode(categoryCode)
                            .categoryName(categoryName)
                            .applicationType("ALL_ACTIVE_SHIFTS")
                            .entityDescription("Shift: Cab " + shift.getCab().getCabNumber() +
                                    " - " + shift.getShiftType())
                            .cabNumber(shift.getCab().getCabNumber())
                            .shiftType(shift.getShiftType() != null ? shift.getShiftType().toString() : "-")
                            .chargeTarget("All Active Shifts")
                            .date(expense.getExpenseDate())
                            .description(expense.getDescription() != null ?
                                    expense.getDescription() : expense.getName())
                            .amount(expense.getAmount())
                            .isRecurring(false)
                            .build());
                }
            }
            // ✅ NEW: Special handling for SHIFTS_WITH_ATTRIBUTE: expand to shifts with that attribute
            else if (expense.getApplicationType() != null &&
                    expense.getApplicationType().name().equals("SHIFTS_WITH_ATTRIBUTE") &&
                    expense.getAttributeTypeId() != null &&
                    !personShifts.isEmpty()) {

                log.info("EXPANDING SHIFTS_WITH_ATTRIBUTE one-time expense ID {} (attribute type {}) to applicable shifts",
                        expense.getId(), expense.getAttributeTypeId());

                // Find which shifts have this attribute
                int expandedCount = 0;
                for (CabShift shift : personShifts) {
                    var shiftAttributes = cabAttributeValueRepository.findCurrentAttributesByShiftId(shift.getId());
                    boolean hasAttribute = shiftAttributes.stream()
                            .anyMatch(attr -> attr.getAttributeType().getId().equals(expense.getAttributeTypeId()));

                    if (hasAttribute) {
                        statement.getOneTimeCharges().add(StatementLineItem.builder()
                                .categoryCode(categoryCode)
                                .categoryName(categoryName)
                                .applicationType("SHIFTS_WITH_ATTRIBUTE")
                                .entityDescription("Shift: Cab " + shift.getCab().getCabNumber() +
                                        " - " + shift.getShiftType() + " (with attribute)")
                                .cabNumber(shift.getCab().getCabNumber())
                                .shiftType(shift.getShiftType() != null ? shift.getShiftType().toString() : "-")
                                .chargeTarget("With Attribute")
                                .date(expense.getExpenseDate())
                                .description(expense.getDescription() != null ?
                                        expense.getDescription() : expense.getName())
                                .amount(expense.getAmount())
                                .isRecurring(false)
                                .build());
                        expandedCount++;
                    }
                }
                log.debug("  - Expanded to {} shifts with the attribute", expandedCount);
            } else {
                // Standard handling for other application types
                statement.getOneTimeCharges().add(StatementLineItem.builder()
                        .categoryCode(categoryCode)
                        .categoryName(categoryName)
                        .applicationType(expense.getApplicationType() != null ?
                                expense.getApplicationType().toString() : "")
                        .entityDescription("One-Time Charge")
                        .date(expense.getExpenseDate())
                        .description(expense.getDescription() != null ?
                                expense.getDescription() : expense.getName())
                        .amount(expense.getAmount())
                        .isRecurring(false)
                        .build());
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private List<RecurringExpense> getRecurringExpensesForShifts(
            List<CabShift> shifts, LocalDate from, LocalDate to) {
        List<RecurringExpense> expenses = new ArrayList<>();

        if (shifts.isEmpty()) {
            return expenses;
        }

        // Get SHIFT_PROFILE expenses
        for (CabShift shift : shifts) {
            if (shift.getCurrentProfile() != null) {
                expenses.addAll(recurringExpenseRepository
                        .findByApplicationTypeAndShiftProfileIdBetween(
                                com.taxi.domain.expense.model.ApplicationType.SHIFT_PROFILE,
                                shift.getCurrentProfile().getId(), from, to));
            }
        }

        // Get SPECIFIC_SHIFT expenses
        for (CabShift shift : shifts) {
            expenses.addAll(recurringExpenseRepository
                    .findByApplicationTypeAndSpecificShiftIdBetween(
                            com.taxi.domain.expense.model.ApplicationType.SPECIFIC_SHIFT,
                            shift.getId(), from, to));
        }

        // Get ALL_ACTIVE_SHIFTS expenses
        expenses.addAll(recurringExpenseRepository.findEffectiveBetweenForApplicationType(
                com.taxi.domain.expense.model.ApplicationType.ALL_ACTIVE_SHIFTS, from, to));

        return expenses;
    }

    private List<RecurringExpense> getRecurringExpensesForPerson(
            Long personId, boolean isOwner, LocalDate from, LocalDate to) {
        List<RecurringExpense> expenses = new ArrayList<>();

        // Get SPECIFIC_PERSON expenses
        expenses.addAll(recurringExpenseRepository.findByApplicationTypeAndSpecificPersonIdBetween(
                com.taxi.domain.expense.model.ApplicationType.SPECIFIC_PERSON, personId, from, to));

        // Get ALL_OWNERS or ALL_DRIVERS
        if (isOwner) {
            expenses.addAll(recurringExpenseRepository.findEffectiveBetweenForApplicationType(
                    com.taxi.domain.expense.model.ApplicationType.ALL_OWNERS, from, to));
        } else {
            expenses.addAll(recurringExpenseRepository.findEffectiveBetweenForApplicationType(
                    com.taxi.domain.expense.model.ApplicationType.ALL_DRIVERS, from, to));
        }

        return expenses;
    }
}
