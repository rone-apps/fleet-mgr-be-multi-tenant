package com.taxi.domain.expense.service;

import com.taxi.domain.driver.model.Driver;
import com.taxi.domain.driver.repository.DriverRepository;
import com.taxi.domain.expense.model.OneTimeExpense;
import com.taxi.domain.expense.model.ExpenseCategory;
import com.taxi.domain.expense.repository.OneTimeExpenseRepository;
import com.taxi.domain.expense.repository.ExpenseCategoryRepository;
import com.taxi.web.dto.expense.CreateOneTimeExpenseRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Service for managing one-time (variable) expenses.
 * 
 * One-time expenses are irregular costs that occur as needed:
 * - Tickets (parking, traffic violations)
 * - Repairs (mechanical, body work)
 * - Parts replacement
 * - Cleaning services
 * - Other ad-hoc expenses
 * 
 * These expenses track who paid and who is responsible for reimbursement.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OneTimeExpenseService {

    private final OneTimeExpenseRepository oneTimeExpenseRepository;
    private final DriverRepository driverRepository;
    private final ExpenseCategoryRepository expenseCategoryRepository;

    /**
     * Create a new one-time expense from a request.
     * Supports multiple application types:
     * - SHIFT_PROFILE: Charges all shifts with a specific profile
     * - SPECIFIC_SHIFT: Charges one specific shift
     * - SPECIFIC_PERSON: Charges a specific owner or driver
     * - SHIFTS_WITH_ATTRIBUTE: Charges all shifts with a specific attribute
     * - ALL_OWNERS: Charges all currently active shifts
     * - ALL_DRIVERS: Charges all non-owner drivers
     */
    @Transactional
    public OneTimeExpense createFromCategory(CreateOneTimeExpenseRequest request) {
        // Category is now optional
        ExpenseCategory category = null;
        if (request.getExpenseCategoryId() != null) {
            category = expenseCategoryRepository.findById(request.getExpenseCategoryId())
                .orElseThrow(() -> new RuntimeException("Expense category not found: " + request.getExpenseCategoryId()));
        }

        // Use application type from request if provided, otherwise from category (if category exists)
        com.taxi.domain.expense.model.ApplicationType applicationType = request.getApplicationType() != null
            ? request.getApplicationType()
            : (category != null ? category.getApplicationType() : null);

        log.info("Creating one-time expense: categoryId={}, applicationType={}, amount={}, date={}",
            category != null ? category.getId() : "null (standalone)", applicationType, request.getAmount(), request.getExpenseDate());

        // Validate required fields based on application type
        validateApplicationTypeFields(applicationType, request);

        OneTimeExpense expense = OneTimeExpense.builder()
            .expenseCategory(category)
            .name(request.getName())
            .amount(request.getAmount())
            .expenseDate(request.getExpenseDate())
            .paidBy(request.getPaidBy())
            .responsibleParty(request.getResponsibleParty())
            .description(request.getDescription())
            .vendor(request.getVendor())
            .receiptUrl(request.getReceiptUrl())
            .invoiceNumber(request.getInvoiceNumber())
            .isReimbursable(request.isReimbursable())
            .notes(request.getNotes())
            .applicationType(applicationType)
            // Use request values if provided, otherwise fall back to category values (if category exists)
            .shiftProfileId(request.getShiftProfileId() != null ? request.getShiftProfileId() : (category != null ? category.getShiftProfileId() : null))
            .specificShiftId(request.getSpecificShiftId() != null ? request.getSpecificShiftId() : (category != null ? category.getSpecificShiftId() : null))
            .specificPersonId(request.getSpecificPersonId() != null ? request.getSpecificPersonId() : null)
            .attributeTypeId(request.getAttributeTypeId() != null ? request.getAttributeTypeId() : (category != null ? category.getAttributeTypeId() : null))
            .build();

        OneTimeExpense saved = oneTimeExpenseRepository.save(expense);

        // ✅ Eagerly fetch relationships for response
        initializeRelationships(saved);

        return saved;
    }

    /**
     * Validate that required fields are provided based on application type.
     * For standalone one-time expenses (no category), application type is optional.
     */
    private void validateApplicationTypeFields(com.taxi.domain.expense.model.ApplicationType applicationType, CreateOneTimeExpenseRequest request) {
        // Application type can be null for simple standalone expenses (no targeting required)
        if (applicationType == null) {
            return;  // Standalone expense with no specific targeting
        }

        switch (applicationType) {
            case SHIFT_PROFILE:
                if (request.getShiftProfileId() == null) {
                    throw new IllegalArgumentException("Shift profile ID is required for SHIFT_PROFILE application type");
                }
                break;
            case SPECIFIC_SHIFT:
                if (request.getSpecificShiftId() == null) {
                    throw new IllegalArgumentException("Specific shift ID is required for SPECIFIC_SHIFT application type");
                }
                break;
            case SPECIFIC_PERSON:
                if (request.getSpecificPersonId() == null) {
                    throw new IllegalArgumentException("Person ID (driver or owner) is required for SPECIFIC_PERSON application type");
                }
                break;
            case SHIFTS_WITH_ATTRIBUTE:
                if (request.getAttributeTypeId() == null) {
                    throw new IllegalArgumentException("Attribute type ID is required for SHIFTS_WITH_ATTRIBUTE application type");
                }
                break;
            case ALL_OWNERS:
            case ALL_DRIVERS:
            case ALL_ACTIVE_SHIFTS:
                // No additional fields required
                break;
            default:
                throw new IllegalArgumentException("Unknown application type: " + applicationType);
        }
    }

    /**
     * Create a new one-time expense
     */
    public OneTimeExpense create(OneTimeExpense expense) {
        log.info("Creating one-time expense: category={}, entityType={}, entityId={}, amount={}, date={}",
            expense.getExpenseCategory() != null ? expense.getExpenseCategory().getCategoryName() : "null",
            expense.getEntityType(),
            expense.getEntityId(),
            expense.getAmount(),
            expense.getExpenseDate());

        OneTimeExpense saved = oneTimeExpenseRepository.save(expense);

        // ✅ Eagerly fetch relationships for response
        initializeRelationships(saved);

        return saved;
    }

    /**
     * Get a one-time expense by ID
     */
    @Transactional(readOnly = true)
    public OneTimeExpense getById(Long id) {
        OneTimeExpense expense = oneTimeExpenseRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("One-time expense not found: " + id));
        
        // ✅ Eagerly fetch relationships
        initializeRelationships(expense);
        
        return expense;
    }

    /**
     * Update a one-time expense
     */
    public OneTimeExpense update(Long id, OneTimeExpense updates) {
        log.info("Updating one-time expense {}", id);
        OneTimeExpense expense = getById(id);
        
        if (updates.getAmount() != null) {
            expense.setAmount(updates.getAmount());
        }
        if (updates.getExpenseDate() != null) {
            expense.setExpenseDate(updates.getExpenseDate());
        }
        if (updates.getDescription() != null) {
            expense.setDescription(updates.getDescription());
        }
        if (updates.getVendor() != null) {
            expense.setVendor(updates.getVendor());
        }
        if (updates.getReceiptUrl() != null) {
            expense.setReceiptUrl(updates.getReceiptUrl());
        }
        if (updates.getInvoiceNumber() != null) {
            expense.setInvoiceNumber(updates.getInvoiceNumber());
        }
        if (updates.getNotes() != null) {
            expense.setNotes(updates.getNotes());
        }
        if (updates.getPaidBy() != null) {
            expense.setPaidBy(updates.getPaidBy());
        }
        if (updates.getResponsibleParty() != null) {
            expense.setResponsibleParty(updates.getResponsibleParty());
        }
        
        OneTimeExpense saved = oneTimeExpenseRepository.save(expense);
        
        // ✅ Eagerly fetch relationships for response
        initializeRelationships(saved);
        
        return saved;
    }

    /**
     * Delete a one-time expense
     */
    public void delete(Long id) {
        log.info("Deleting one-time expense {}", id);
        oneTimeExpenseRepository.deleteById(id);
    }

    /**
     * Get all one-time expenses for a specific entity
     */
    @Transactional(readOnly = true)
    public List<OneTimeExpense> getExpensesForEntity(
            OneTimeExpense.EntityType entityType, Long entityId) {
        List<OneTimeExpense> expenses = oneTimeExpenseRepository.findByEntityTypeAndEntityId(entityType, entityId);
        
        // ✅ Eagerly fetch relationships for all expenses
        expenses.forEach(this::initializeRelationships);
        
        return expenses;
    }

    @Transactional(readOnly = true)
    public List<OneTimeExpense> getExpensesForDriverBetween(String driverNumber, LocalDate startDate, LocalDate endDate) {
        Driver driver = driverRepository.findByDriverNumber(driverNumber)
            .orElseThrow(() -> new IllegalArgumentException("Driver not found: " + driverNumber));

        List<OneTimeExpense> expenses = oneTimeExpenseRepository.findForDriverBetween(driver.getId(), startDate, endDate);
        expenses.forEach(this::initializeRelationships);
        return expenses;
    }

    /**
     * Get one-time expenses for an entity within a date range
     */
    @Transactional(readOnly = true)
    public List<OneTimeExpense> getExpensesForEntityBetween(
            OneTimeExpense.EntityType entityType, Long entityId,
            LocalDate startDate, LocalDate endDate) {
        List<OneTimeExpense> expenses = oneTimeExpenseRepository.findForEntityBetween(
            entityType, entityId, startDate, endDate);
        
        // ✅ Eagerly fetch relationships for all expenses
        expenses.forEach(this::initializeRelationships);
        
        return expenses;
    }

    /**
     * Get all one-time expenses within a date range
     */
    @Transactional(readOnly = true)
    public List<OneTimeExpense> getExpensesBetween(LocalDate startDate, LocalDate endDate) {
        List<OneTimeExpense> expenses = oneTimeExpenseRepository.findBetween(startDate, endDate);
        
        // ✅ Eagerly fetch relationships for all expenses
        expenses.forEach(this::initializeRelationships);
        
        log.info("Loaded {} one-time expenses between {} and {}", expenses.size(), startDate, endDate);
        
        return expenses;
    }

    /**
     * Get expenses by category
     */
    @Transactional(readOnly = true)
    public List<OneTimeExpense> getExpensesByCategory(Long categoryId) {
        List<OneTimeExpense> expenses = oneTimeExpenseRepository.findByExpenseCategoryId(categoryId);
        
        // ✅ Eagerly fetch relationships for all expenses
        expenses.forEach(this::initializeRelationships);
        
        return expenses;
    }

    /**
     * Calculate total one-time expenses for an entity in a date range
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalForEntity(
            OneTimeExpense.EntityType entityType, Long entityId,
            LocalDate startDate, LocalDate endDate) {
        return oneTimeExpenseRepository.calculateTotalForEntityBetween(
            entityType, entityId, startDate, endDate);
    }

    /**
     * Calculate total expenses by category in a date range
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalByCategory(Long categoryId, LocalDate startDate, LocalDate endDate) {
        return oneTimeExpenseRepository.calculateTotalByCategoryBetween(categoryId, startDate, endDate);
    }

    /**
     * Get all expenses pending reimbursement
     */
    @Transactional(readOnly = true)
    public List<OneTimeExpense> getPendingReimbursements() {
        List<OneTimeExpense> expenses = oneTimeExpenseRepository.findPendingReimbursements();
        
        // ✅ Eagerly fetch relationships for all expenses
        expenses.forEach(this::initializeRelationships);
        
        return expenses;
    }

    /**
     * Get pending reimbursements for a specific payer
     */
    @Transactional(readOnly = true)
    public List<OneTimeExpense> getPendingReimbursementsByPaidBy(OneTimeExpense.PaidBy paidBy) {
        List<OneTimeExpense> expenses = oneTimeExpenseRepository.findPendingReimbursementsByPaidBy(paidBy);
        
        // ✅ Eagerly fetch relationships for all expenses
        expenses.forEach(this::initializeRelationships);
        
        return expenses;
    }

    /**
     * Get expenses without receipts (for compliance tracking)
     */
    @Transactional(readOnly = true)
    public List<OneTimeExpense> getExpensesWithoutReceipts(LocalDate sinceDate) {
        List<OneTimeExpense> expenses = oneTimeExpenseRepository.findWithoutReceiptSince(sinceDate);
        
        // ✅ Eagerly fetch relationships for all expenses
        expenses.forEach(this::initializeRelationships);
        
        return expenses;
    }

    /**
     * Get expenses by responsible party in a date range
     */
    @Transactional(readOnly = true)
    public List<OneTimeExpense> getExpensesByResponsibleParty(
            OneTimeExpense.ResponsibleParty responsibleParty,
            LocalDate startDate, LocalDate endDate) {
        List<OneTimeExpense> expenses = oneTimeExpenseRepository.findByResponsiblePartyBetween(
            responsibleParty, startDate, endDate);
        
        // ✅ Eagerly fetch relationships for all expenses
        expenses.forEach(this::initializeRelationships);
        
        return expenses;
    }

    /**
     * Mark an expense as reimbursed
     */
    public void markAsReimbursed(Long id, LocalDate reimbursementDate) {
        log.info("Marking expense {} as reimbursed on {}", id, reimbursementDate);
        OneTimeExpense expense = getById(id);
        expense.markAsReimbursed(reimbursementDate);
        OneTimeExpense saved = oneTimeExpenseRepository.save(expense);
        
        // ✅ Eagerly fetch relationships for response
        initializeRelationships(saved);
    }

    /**
     * Set an expense as reimbursable
     */
    public void setReimbursable(Long id, boolean reimbursable) {
        log.info("Setting expense {} reimbursable={}", id, reimbursable);
        OneTimeExpense expense = getById(id);
        expense.setReimbursable(reimbursable);
        OneTimeExpense saved = oneTimeExpenseRepository.save(expense);
        
        // ✅ Eagerly fetch relationships for response
        initializeRelationships(saved);
    }

    /**
     * Attach a receipt URL to an expense
     */
    public void attachReceipt(Long id, String receiptUrl) {
        log.info("Attaching receipt to expense {}", id);
        OneTimeExpense expense = getById(id);
        expense.setReceiptUrl(receiptUrl);
        OneTimeExpense saved = oneTimeExpenseRepository.save(expense);
        
        // ✅ Eagerly fetch relationships for response
        initializeRelationships(saved);
    }
    
    /**
     * ✅ ADDED - Initialize lazy-loaded relationships to prevent LazyInitializationException
     * This ensures all relationships are loaded within the transaction so they're
     * available when serializing to JSON
     */
    private void initializeRelationships(OneTimeExpense expense) {
        // Force initialization of lazy-loaded relationships
        if (expense.getCab() != null) {
            expense.getCab().getCabNumber(); // Touch the entity to initialize
        }
        if (expense.getDriver() != null) {
            expense.getDriver().getDriverNumber(); // Touch the entity to initialize
            expense.setDriverNumber(expense.getDriver().getDriverNumber());
        } else if (expense.getOwner() != null) {
            expense.setDriverNumber(expense.getOwner().getDriverNumber());
        }
        if (expense.getOwner() != null) {
            expense.getOwner().getDriverNumber(); // Touch the entity to initialize
        }
        if (expense.getDriverNumber() == null && expense.getEntityType() != null && expense.getEntityId() != null) {
            if (expense.getEntityType() == OneTimeExpense.EntityType.DRIVER
                    || expense.getEntityType() == OneTimeExpense.EntityType.OWNER) {
                driverRepository.findById(expense.getEntityId())
                    .map(Driver::getDriverNumber)
                    .ifPresent(expense::setDriverNumber);
            }
        }
        // ExpenseCategory is already EAGER, no need to initialize
    }
}