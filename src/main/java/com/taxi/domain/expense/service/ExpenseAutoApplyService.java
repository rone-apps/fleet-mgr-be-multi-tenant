package com.taxi.domain.expense.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxi.domain.cab.model.Cab;
import com.taxi.domain.cab.model.CabShiftType;
import com.taxi.domain.cab.repository.CabAttributeValueRepository;
import com.taxi.domain.cab.repository.CabRepository;
import com.taxi.domain.expense.model.*;
import com.taxi.domain.expense.model.ExpenseCategory.AppliesTo;
import com.taxi.domain.expense.model.RecurringExpense.EntityType;
import com.taxi.domain.expense.model.RecurringExpenseAutoCreation.CreationType;
import com.taxi.domain.expense.repository.RecurringExpenseAutoCreationRepository;
import com.taxi.domain.expense.repository.RecurringExpenseRepository;
import com.taxi.domain.shift.model.CabShift;
import com.taxi.domain.shift.model.ShiftType;
import com.taxi.domain.shift.repository.CabShiftRepository;
import com.taxi.domain.shift.service.ShiftStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ExpenseAutoApplyService - Handles auto-applying expenses to matching cabs
 * and bulk creating expenses with individual amounts
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ExpenseAutoApplyService {

    private final CabMatchingService cabMatchingService;
    private final CabRepository cabRepository;
    private final CabShiftRepository cabShiftRepository;
    private final CabAttributeValueRepository cabAttributeValueRepository;
    private final RecurringExpenseRepository recurringExpenseRepository;
    private final RecurringExpenseAutoCreationRepository autoCreationRepository;
    private final ShiftStatusService shiftStatusService;
    private final ObjectMapper objectMapper;

    /**
     * Auto-apply expenses to all cabs matching the category rules
     */
    public AutoApplyResult autoApplyExpenses(
            ExpenseCategoryRule categoryRule,
            BigDecimal amount,
            RecurringExpense.BillingMethod billingMethod,
            LocalDate effectiveFrom,
            String createdBy) {

        log.info("Auto-applying expenses for category rule ID: {}", categoryRule.getId());

        // Parse matching criteria from JSON
        MatchingCriteria criteria = parseMatchingCriteria(categoryRule.getMatchingCriteria());

        // Find matching cabs
        List<Cab> matchingCabs = cabMatchingService.findMatchingCabs(criteria);
        log.info("Found {} cabs matching criteria", matchingCabs.size());

        List<RecurringExpense> createdExpenses = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (Cab cab : matchingCabs) {
            try {
                // Check if expense already exists for this cab + category
                boolean exists = recurringExpenseRepository.existsActiveByCabAndCategory(
                    cab.getId(), categoryRule.getExpenseCategory().getId());

                if (exists) {
                    errors.add("Cab " + cab.getCabNumber() + " already has this expense");
                    log.warn("Cab {} already has expense for category {}",
                        cab.getCabNumber(), categoryRule.getExpenseCategory().getCategoryCode());
                    continue;
                }

                // Create expenses for this cab based on category appliesTo and shift type
                List<RecurringExpense> cabExpenses = createExpensesForCab(
                    cab, categoryRule, amount, billingMethod, effectiveFrom);

                createdExpenses.addAll(cabExpenses);
                log.debug("Created {} expenses for cab {}", cabExpenses.size(), cab.getCabNumber());

            } catch (Exception e) {
                String errorMsg = "Error for cab " + cab.getCabNumber() + ": " + e.getMessage();
                errors.add(errorMsg);
                log.error(errorMsg, e);
            }
        }

        return new AutoApplyResult(createdExpenses, errors, matchingCabs.size());
    }

    /**
     * Bulk create expenses with individual amounts per cab
     */
    public BulkCreateResult bulkCreateIndividualExpenses(
            ExpenseCategoryRule categoryRule,
            Map<Long, BigDecimal> cabIdToAmount,
            RecurringExpense.BillingMethod billingMethod,
            LocalDate effectiveFrom,
            String createdBy) {

        log.info("Bulk creating {} individual expenses", cabIdToAmount.size());

        List<RecurringExpense> createdExpenses = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (Map.Entry<Long, BigDecimal> entry : cabIdToAmount.entrySet()) {
            Long cabId = entry.getKey();
            BigDecimal amount = entry.getValue();

            try {
                Cab cab = cabRepository.findById(cabId)
                    .orElseThrow(() -> new RuntimeException("Cab not found with ID: " + cabId));

                // Check for existing expense
                boolean exists = recurringExpenseRepository.existsActiveByCabAndCategory(
                    cab.getId(), categoryRule.getExpenseCategory().getId());

                if (exists) {
                    errors.add("Cab " + cab.getCabNumber() + " already has this expense");
                    continue;
                }

                // Create expenses with individual amount
                List<RecurringExpense> cabExpenses = createExpensesForCab(
                    cab, categoryRule, amount, billingMethod, effectiveFrom);

                createdExpenses.addAll(cabExpenses);

            } catch (Exception e) {
                String errorMsg = "Error for cab ID " + cabId + ": " + e.getMessage();
                errors.add(errorMsg);
                log.error(errorMsg, e);
            }
        }

        return new BulkCreateResult(createdExpenses, errors);
    }

    /**
     * Create appropriate expenses for a cab based on category configuration
     *
     * REFACTORED (Phase 1): Now respects shift-level status
     * - Only creates expenses for currently active shifts
     * - Attributes are checked at shift level, not cab level
     *
     * If category applies to CAB: create 1 expense per cab
     * If category applies to SHIFT: create expense for each active shift
     *   (Every cab now has exactly 2 shifts: DAY and NIGHT)
     *   (Only creates expense if shift is currently active)
     */
    private List<RecurringExpense> createExpensesForCab(
            Cab cab,
            ExpenseCategoryRule categoryRule,
            BigDecimal amount,
            RecurringExpense.BillingMethod billingMethod,
            LocalDate effectiveFrom) {

        List<RecurringExpense> expenses = new ArrayList<>();
        ExpenseCategory category = categoryRule.getExpenseCategory();

        if (category.getAppliesTo() == AppliesTo.CAB) {
            // Create single CAB-level expense
            RecurringExpense expense = RecurringExpense.builder()
                .expenseCategory(category)
                .entityType(EntityType.CAB)
                .entityId(cab.getId())
                .shiftType(null)
                .amount(amount)
                .billingMethod(billingMethod)
                .effectiveFrom(effectiveFrom)
                .effectiveTo(null)
                .isActive(true)
                .isAutoGenerated(true)
                .sourceRuleId(categoryRule.getId())
                .generationDate(LocalDateTime.now())
                .build();

            RecurringExpense saved = recurringExpenseRepository.save(expense);
            expenses.add(saved);

            // Create audit trail
            createAuditTrail(categoryRule, saved, cab, null);

        } else if (category.getAppliesTo() == AppliesTo.SHIFT) {
            // ====================================================================
            // REFACTORED: Every cab now has exactly 2 shifts (DAY and NIGHT)
            // Check shift status before creating expenses
            // ====================================================================
            List<CabShift> allShifts = cabShiftRepository.findByCab(cab);

            for (CabShift shift : allShifts) {
                // IMPORTANT: Only create expense if shift is currently active
                if (!shift.isCurrentlyActive()) {
                    log.info("Skipping inactive shift {} for cab {} - no expense created",
                        shift.getShiftType(), cab.getCabNumber());
                    continue;
                }

                log.debug("Creating expense for active shift {} of cab {}",
                    shift.getShiftType(), cab.getCabNumber());

                RecurringExpense expense = RecurringExpense.builder()
                    .expenseCategory(category)
                    .entityType(EntityType.SHIFT)
                    .entityId(shift.getId())
                    .shiftType(shift.getShiftType())
                    .amount(amount)
                    .billingMethod(billingMethod)
                    .effectiveFrom(effectiveFrom)
                    .effectiveTo(null)
                    .isActive(true)
                    .isAutoGenerated(true)
                    .sourceRuleId(categoryRule.getId())
                    .generationDate(LocalDateTime.now())
                    .build();

                RecurringExpense saved = recurringExpenseRepository.save(expense);
                expenses.add(saved);
                createAuditTrail(categoryRule, saved, cab, shift.getShiftType());
            }
        }

        return expenses;
    }

    /**
     * Create audit trail entry for auto-created expense
     */
    private void createAuditTrail(
            ExpenseCategoryRule categoryRule,
            RecurringExpense expense,
            Cab cab,
            ShiftType shiftType) {

        try {
            String snapshot = createMatchingSnapshot(cab, categoryRule, shiftType);

            RecurringExpenseAutoCreation audit = RecurringExpenseAutoCreation.builder()
                .categoryRule(categoryRule)
                .recurringExpense(expense)
                .creationType(CreationType.AUTO_MATCHED)
                .matchingSnapshot(snapshot)
                .createdBy("SYSTEM")
                .build();

            autoCreationRepository.save(audit);
        } catch (Exception e) {
            log.warn("Failed to create audit trail for expense {}: {}", expense.getId(), e.getMessage());
        }
    }

    /**
     * Create JSON snapshot of cab state and matching criteria
     *
     * REFACTORED: Now captures shift-level attributes instead of cab-level
     * Attributes are now per-shift, not per-cab
     */
    private String createMatchingSnapshot(
            Cab cab,
            ExpenseCategoryRule categoryRule,
            ShiftType shiftType) {

        try {
            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put("cabNumber", cab.getCabNumber());
            snapshot.put("cabId", cab.getId());

            // REFACTORED: Get attributes from shift, not cab
            if (shiftType != null) {
                CabShift shift = cabShiftRepository.findByCabAndShiftType(cab, shiftType)
                    .orElse(null);

                if (shift != null) {
                    snapshot.put("shareType", shift.getShareType());
                    snapshot.put("hasAirportLicense", shift.getHasAirportLicense());
                    snapshot.put("cabType", shift.getCabType());
                    snapshot.put("isShiftActive", shift.isCurrentlyActive());
                }
            }

            snapshot.put("shiftType", shiftType);
            snapshot.put("matchingCriteria", categoryRule.getMatchingCriteria());
            snapshot.put("timestamp", LocalDateTime.now());

            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            log.warn("Failed to serialize matching snapshot: {}", e.getMessage());
            return "{}";
        }
    }

    /**
     * Create expenses for all shifts with a specific attribute
     * Used by SHIFTS_WITH_ATTRIBUTE application type
     */
    public List<RecurringExpense> createExpensesForShiftsWithAttribute(
            ExpenseCategory category,
            BigDecimal amount,
            RecurringExpense.BillingMethod billingMethod,
            LocalDate effectiveFrom,
            String createdBy) {

        log.info("Creating expenses for shifts with attribute type ID: {}", category.getAttributeTypeId());

        if (category.getAttributeTypeId() == null) {
            throw new IllegalStateException("Attribute type ID is required for SHIFTS_WITH_ATTRIBUTE application type");
        }

        // Find all active shifts with this attribute
        List<CabShift> matchingShifts = cabAttributeValueRepository.findActiveShiftsWithAttribute(category.getAttributeTypeId());
        log.info("Found {} shifts with attribute type {}", matchingShifts.size(), category.getAttributeTypeId());

        List<RecurringExpense> createdExpenses = new ArrayList<>();

        for (CabShift shift : matchingShifts) {
            try {
                // Check if expense already exists
                boolean exists = recurringExpenseRepository.existsActiveByCabAndCategory(
                        shift.getId(), category.getId());

                if (exists) {
                    log.warn("Shift {} already has expense for category {}", shift.getId(), category.getCategoryCode());
                    continue;
                }

                // Create expense for this shift
                RecurringExpense expense = RecurringExpense.builder()
                        .expenseCategory(category)
                        .applicationTypeEnum(ApplicationType.SPECIFIC_SHIFT)
                        .specificShiftId(shift.getId())
                        .shift(shift)
                        .amount(amount)
                        .billingMethod(billingMethod)
                        .effectiveFrom(effectiveFrom)
                        .isActive(true)
                        .isAutoGenerated(true)
                        .attributeTypeId(category.getAttributeTypeId())
                        .notes(String.format("Auto-created for shifts with attribute type %d", category.getAttributeTypeId()))
                        .build();

                RecurringExpense saved = recurringExpenseRepository.save(expense);
                createdExpenses.add(saved);
                log.debug("Created expense for shift {} with attribute type {}", shift.getId(), category.getAttributeTypeId());

            } catch (Exception e) {
                log.error("Error creating expense for shift {}: {}", shift.getId(), e.getMessage(), e);
            }
        }

        log.info("Created {} expenses for shifts with attribute type {}", createdExpenses.size(), category.getAttributeTypeId());
        return createdExpenses;
    }

    /**
     * Parse matching criteria from JSON string
     */
    private MatchingCriteria parseMatchingCriteria(String json) {
        try {
            if (json == null || json.isEmpty()) {
                return new MatchingCriteria();
            }
            return objectMapper.readValue(json, MatchingCriteria.class);
        } catch (Exception e) {
            log.error("Failed to parse matching criteria: {}", e.getMessage());
            return new MatchingCriteria();
        }
    }

    /**
     * Result of auto-apply operation
     */
    public static class AutoApplyResult {
        public final List<RecurringExpense> createdExpenses;
        public final List<String> errors;
        public final int totalMatched;

        public AutoApplyResult(
                List<RecurringExpense> createdExpenses,
                List<String> errors,
                int totalMatched) {
            this.createdExpenses = createdExpenses;
            this.errors = errors;
            this.totalMatched = totalMatched;
        }

        public int getSuccessCount() {
            return createdExpenses.size();
        }
    }

    /**
     * Result of bulk create operation
     */
    public static class BulkCreateResult {
        public final List<RecurringExpense> createdExpenses;
        public final List<String> errors;

        public BulkCreateResult(
                List<RecurringExpense> createdExpenses,
                List<String> errors) {
            this.createdExpenses = createdExpenses;
            this.errors = errors;
        }

        public int getSuccessCount() {
            return createdExpenses.size();
        }
    }
}
