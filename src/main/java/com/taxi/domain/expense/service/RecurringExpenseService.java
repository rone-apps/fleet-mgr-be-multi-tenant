package com.taxi.domain.expense.service;

import com.taxi.domain.expense.model.RecurringExpense;
import com.taxi.domain.expense.repository.RecurringExpenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Service for managing recurring (fixed) expenses.
 * 
 * Recurring expenses are fixed costs that repeat on a schedule:
 * - Dispatch fees (per shift or daily)
 * - Internet charges (monthly)
 * - Insurance (monthly, can vary per cab)
 * - Airport fees (monthly, for cabs with airport plates)
 * 
 * Rate changes are handled by creating new versions to preserve history.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RecurringExpenseService {

    private final RecurringExpenseRepository recurringExpenseRepository;

    /**
     * Create a new recurring expense
     */
    public RecurringExpense create(RecurringExpense expense) {
        log.info("Creating recurring expense: category={}, entityType={}, entityId={}, amount={}, billingMethod={}",
            expense.getExpenseCategory() != null ? expense.getExpenseCategory().getCategoryName() : "null",
            expense.getEntityType(),
            expense.getEntityId(),
            expense.getAmount(),
            expense.getBillingMethod());
        return recurringExpenseRepository.save(expense);
    }

    /**
     * Get a recurring expense by ID
     */
    @Transactional(readOnly = true)
    public RecurringExpense getById(Long id) {
        return recurringExpenseRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Recurring expense not found: " + id));
    }

    /**
     * Get all recurring expenses (both active and inactive)
     */
    @Transactional(readOnly = true)
    public List<RecurringExpense> findAll() {
        return recurringExpenseRepository.findAll();
    }

    /**
     * Update a recurring expense
     */
    @Transactional
    public RecurringExpense update(RecurringExpense expense) {
        log.info("Updating recurring expense: {}", expense.getId());
        return recurringExpenseRepository.save(expense);
    }

    /**
     * Delete a recurring expense
     */
    @Transactional
    public void delete(Long id) {
        RecurringExpense expense = getById(id);
        recurringExpenseRepository.delete(expense);
        log.info("Deleted recurring expense: {}", id);
    }

    /**
     * Change rate for a recurring expense with end date control
     * This closes the current expense and creates a new one with the new rate
     * 
     * Scenario: Dispatch fee is $200 from Nov 1. On March 1, change it to $225.
     * Result: 
     * - Old expense: effective Nov 1 - Feb 28, inactive
     * - New expense: effective March 1 - ongoing, active
     */
    @Transactional
    public RecurringExpense changeRate(Long id, BigDecimal newAmount, LocalDate effectiveDate, String notes) {
        RecurringExpense existing = getById(id);
        
        // Close the existing expense one day before the new rate takes effect
        LocalDate closeDate = effectiveDate.minusDays(1);
        existing.setEffectiveTo(closeDate);
        existing.setActive(false);
        recurringExpenseRepository.save(existing);
        
        log.info("Closed recurring expense {} on {}", id, closeDate);
        
        // Create new expense with the new rate
        RecurringExpense newExpense = RecurringExpense.builder()
            .expenseCategory(existing.getExpenseCategory())
            .entityType(existing.getEntityType())
            .entityId(existing.getEntityId())
            .amount(newAmount)
            .billingMethod(existing.getBillingMethod())
            .effectiveFrom(effectiveDate)
            .effectiveTo(null) // Open-ended until next change
            .isActive(true)
            .notes(notes != null ? notes : "Rate changed from " + existing.getAmount() + " to " + newAmount)
            .build();
        
        RecurringExpense created = recurringExpenseRepository.save(newExpense);
        log.info("Created new recurring expense {} with rate {} effective from {}", 
            created.getId(), newAmount, effectiveDate);
        
        return created;
    }
    
    /**
     * Deactivate a recurring expense with specific end date (no longer charged)
     * 
     * Scenario: No longer charging internet fees from January 1st.
     * Result: Expense closed on Dec 31, marked inactive, history preserved for reports.
     */
    @Transactional
    public RecurringExpense deactivateWithEndDate(Long id, LocalDate endDate) {
        RecurringExpense expense = getById(id);
        
        // Set the end date (day before deactivation takes effect)
        expense.setEffectiveTo(endDate.minusDays(1));
        expense.setActive(false);
        
        recurringExpenseRepository.save(expense);
        log.info("Deactivated recurring expense {} with end date {}", id, endDate.minusDays(1));
        
        return expense;
    }
    
    /**
     * Reactivate with new start date
     */
    @Transactional
    public RecurringExpense reactivateWithDate(Long id, LocalDate effectiveDate) {
        RecurringExpense expense = getById(id);
        
        expense.setEffectiveTo(null);
        expense.setActive(true);
        expense.setEffectiveFrom(effectiveDate);
        
        recurringExpenseRepository.save(expense);
        log.info("Reactivated recurring expense {} effective from {}", id, effectiveDate);
        
        return expense;
    }

    /**
     * Get all active recurring expenses
     */
    @Transactional(readOnly = true)
    public List<RecurringExpense> getActiveExpenses() {
        return recurringExpenseRepository.findByIsActiveTrue();
    }

    /**
     * Get all recurring expenses for a specific entity (including history)
     */
    @Transactional(readOnly = true)
    public List<RecurringExpense> getExpensesForEntity(
            RecurringExpense.EntityType entityType, Long entityId) {
        return recurringExpenseRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }

    /**
     * Get active recurring expenses for a specific entity
     */
    @Transactional(readOnly = true)
    public List<RecurringExpense> getActiveExpensesForEntity(
            RecurringExpense.EntityType entityType, Long entityId) {
        return recurringExpenseRepository.findActiveByEntityTypeAndEntityId(entityType, entityId);
    }

    /**
     * Get all recurring expenses effective on a specific date
     */
    @Transactional(readOnly = true)
    public List<RecurringExpense> getExpensesEffectiveOn(LocalDate date) {
        return recurringExpenseRepository.findEffectiveOn(date);
    }

    /**
     * Get recurring expenses for a specific entity effective on a date
     */
    @Transactional(readOnly = true)
    public List<RecurringExpense> getExpensesForEntityOn(
            RecurringExpense.EntityType entityType, Long entityId, LocalDate date) {
        return recurringExpenseRepository.findEffectiveForEntityOn(entityType, entityId, date);
    }

    /**
     * Get recurring expenses for an entity within a date range (for reporting)
     * This includes expenses that were active at any point during the range
     */
    @Transactional(readOnly = true)
    public List<RecurringExpense> getExpensesForEntityBetween(
            RecurringExpense.EntityType entityType, Long entityId,
            LocalDate startDate, LocalDate endDate) {
        return recurringExpenseRepository.findForEntityBetween(entityType, entityId, startDate, endDate);
    }

    /**
     * Get expense history for an entity (all versions including inactive)
     * Useful for viewing rate change history
     */
    @Transactional(readOnly = true)
    public List<RecurringExpense> getExpenseHistory(
            RecurringExpense.EntityType entityType, Long entityId) {
        return recurringExpenseRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }

    /**
     * Get expenses by category
     */
    @Transactional(readOnly = true)
    public List<RecurringExpense> getExpensesByCategory(Long categoryId) {
        return recurringExpenseRepository.findActiveByExpenseCategoryId(categoryId);
    }

    /**
     * Update the rate for a recurring expense.
     * This creates a new version and ends the current one to preserve history.
     * 
     * Example: Dispatch fee changes from $300 to $250 in January 2026
     * - Current record (effective_from=2025-01-01, effective_to=null) gets effective_to=2025-12-31
     * - New record created (effective_from=2026-01-01, effective_to=null, amount=250)
     * 
     * @param id The ID of the current recurring expense
     * @param newAmount The new rate amount
     * @param newBillingMethod The billing method (can change, e.g., from DAILY to PER_SHIFT)
     * @param effectiveMonth The month when the new rate takes effect
     * @return The newly created recurring expense with the new rate
     */
    public RecurringExpense updateRate(Long id, BigDecimal newAmount,
            RecurringExpense.BillingMethod newBillingMethod, YearMonth effectiveMonth) {
        log.info("Updating rate for recurring expense {}: newAmount={}, billingMethod={}, effectiveMonth={}",
            id, newAmount, newBillingMethod, effectiveMonth);
        
        RecurringExpense current = getById(id);
        
        // Create new version (this also ends the current one at end of previous month)
        RecurringExpense newVersion = current.createNewVersion(newAmount, newBillingMethod, effectiveMonth);
        
        // Save both - current with updated effective_to, new version
        recurringExpenseRepository.save(current);
        RecurringExpense saved = recurringExpenseRepository.save(newVersion);
        
        log.info("Created new version {} for recurring expense, old version {} ended at {}",
            saved.getId(), current.getId(), current.getEffectiveTo());
        
        return saved;
    }

    /**
     * End a recurring expense (e.g., cab sold, no longer needs insurance)
     */
    public void endExpense(Long id, LocalDate endDate) {
        log.info("Ending recurring expense {} as of {}", id, endDate);
        RecurringExpense expense = getById(id);
        expense.endExpense(endDate);
        recurringExpenseRepository.save(expense);
    }

    /**
     * Calculate total recurring expenses for an entity in a date range.
     * Handles prorating for partial months and rate changes within the period.
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalForEntity(
            RecurringExpense.EntityType entityType, Long entityId,
            LocalDate startDate, LocalDate endDate) {
        List<RecurringExpense> expenses = getExpensesForEntityBetween(
            entityType, entityId, startDate, endDate);
        
        return expenses.stream()
            .map(e -> e.calculateAmountForDateRange(startDate, endDate))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculate total recurring expenses for all entities of a type in a date range
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalBetween(LocalDate startDate, LocalDate endDate) {
        List<RecurringExpense> expenses = recurringExpenseRepository.findEffectiveBetween(startDate, endDate);
        
        return expenses.stream()
            .map(e -> e.calculateAmountForDateRange(startDate, endDate))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Deactivate a recurring expense without setting an end date
     * Use this for temporary suspension
     */
    public void deactivate(Long id) {
        log.info("Deactivating recurring expense {}", id);
        RecurringExpense expense = getById(id);
        expense.setActive(false);
        recurringExpenseRepository.save(expense);
    }

    /**
     * Reactivate a previously deactivated recurring expense
     */
    public void reactivate(Long id) {
        log.info("Reactivating recurring expense {}", id);
        RecurringExpense expense = getById(id);
        expense.setActive(true);
        recurringExpenseRepository.save(expense);
    }
}
