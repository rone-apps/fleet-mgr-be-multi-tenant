package com.taxi.web.controller;

import com.taxi.domain.expense.model.ExpenseCategory;
import com.taxi.domain.expense.model.RecurringExpense;
import com.taxi.domain.expense.service.ExpenseCategoryService;
import com.taxi.domain.expense.service.SimplifiedExpenseApplicationService;
import com.taxi.web.dto.expense.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/expense-categories")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ExpenseCategoryController {

    private final ExpenseCategoryService expenseCategoryService;
    private final SimplifiedExpenseApplicationService simplifiedExpenseApplicationService;

    // Test endpoint - no security
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("ExpenseCategory controller is working!");
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ExpenseCategory> createCategory(@Valid @RequestBody CreateExpenseCategoryRequest request) {
        try {
            ExpenseCategory category = request.toEntity();
            ExpenseCategory created = expenseCategoryService.createCategory(category);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            log.error("Error creating expense category", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ExpenseCategory> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CreateExpenseCategoryRequest request) {
        try {
            ExpenseCategory updates = request.toEntity();
            ExpenseCategory updated = expenseCategoryService.updateCategory(id, updates);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Error updating expense category", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        try {
            expenseCategoryService.deleteCategory(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting expense category", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> activateCategory(@PathVariable Long id) {
        try {
            expenseCategoryService.activateCategory(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error activating expense category", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> deactivateCategory(@PathVariable Long id) {
        try {
            expenseCategoryService.deactivateCategory(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deactivating expense category", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DISPATCHER', 'ACCOUNTANT')")
    public ResponseEntity<ExpenseCategory> getCategory(@PathVariable Long id) {
        try {
            ExpenseCategory category = expenseCategoryService.getCategory(id);
            return ResponseEntity.ok(category);
        } catch (Exception e) {
            log.error("Error fetching expense category", e);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DISPATCHER', 'ACCOUNTANT')")
    public ResponseEntity<List<ExpenseCategory>> getAllCategories() {
        log.info("GET /api/expense-categories called");
        log.info("Current authentication: {}", 
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication());
        
        List<ExpenseCategory> categories = expenseCategoryService.getAllCategories();
        log.info("Returning {} categories", categories.size());
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DISPATCHER', 'ACCOUNTANT', 'DRIVER')")
    public ResponseEntity<List<ExpenseCategory>> getActiveCategories() {
        List<ExpenseCategory> categories = expenseCategoryService.getActiveCategories();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/type/{type}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<ExpenseCategory>> getCategoriesByType(@PathVariable ExpenseCategory.CategoryType type) {
        List<ExpenseCategory> categories = expenseCategoryService.getCategoriesByType(type);
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/applies-to/{appliesTo}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<ExpenseCategory>> getCategoriesByAppliesTo(@PathVariable ExpenseCategory.AppliesTo appliesTo) {
        List<ExpenseCategory> categories = expenseCategoryService.getCategoriesByAppliesTo(appliesTo);
        return ResponseEntity.ok(categories);
    }

    // ============================================================================
    // SIMPLIFIED EXPENSE APPLICATION ENDPOINTS (New System)
    // ============================================================================

    /**
     * Create expenses for a category using the simplified application type system
     */
    @PostMapping("/{categoryId}/create-expenses")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ExpenseCreationResultDTO> createExpenses(
            @PathVariable Long categoryId,
            @Valid @RequestBody SimpleExpenseCreationRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        try {
            log.info("Creating expenses for category: {}", categoryId);

            ExpenseCategory category = expenseCategoryService.getCategory(categoryId);

            List<RecurringExpense> createdExpenses = simplifiedExpenseApplicationService.createExpensesForCategory(
                    category,
                    request.getAmount(),
                    request.getBillingMethod(),
                    request.getEffectiveFrom(),
                    currentUser.getUsername());

            ExpenseCreationResultDTO result = ExpenseCreationResultDTO.builder()
                    .createdCount(createdExpenses.size())
                    .totalCount(createdExpenses.size())
                    .success(true)
                    .build();

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error creating expenses for category: {}", categoryId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Preview how many entities will receive an expense for a category
     */
    @GetMapping("/{categoryId}/preview-application")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<ApplicationPreviewDTO> previewApplication(@PathVariable Long categoryId) {
        try {
            ExpenseCategory category = expenseCategoryService.getCategory(categoryId);

            if (category.getApplicationType() == null) {
                return ResponseEntity.badRequest().build();
            }

            int count = simplifiedExpenseApplicationService.previewApplicationCount(category);

            ApplicationPreviewDTO preview = ApplicationPreviewDTO.builder()
                    .applicationType(category.getApplicationType())
                    .applicationTypeLabel(category.getApplicationType().getDisplayName())
                    .affectedEntityCount(count)
                    .description(buildApplicationDescription(category, count))
                    .build();

            return ResponseEntity.ok(preview);

        } catch (Exception e) {
            log.error("Error previewing application for category: {}", categoryId, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Build human-readable description of application
     */
    private String buildApplicationDescription(ExpenseCategory category, int count) {
        switch (category.getApplicationType()) {
            case SHIFT_PROFILE:
                return String.format("This expense will apply to %d shifts with the selected profile", count);
            case SPECIFIC_SHIFT:
                return "This expense will apply to one specific shift";
            case SPECIFIC_PERSON:
                return "This expense will apply to a specific owner or driver";
            case ALL_OWNERS:
                return String.format("This expense will apply to all %d currently active shifts", count);
            case ALL_DRIVERS:
                return String.format("This expense will apply to all %d drivers who are not owners", count);
            default:
                return "Unknown application type";
        }
    }
}
