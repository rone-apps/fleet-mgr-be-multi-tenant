package com.taxi.web.controller;

import com.taxi.domain.expense.model.ExpenseCategory;
import com.taxi.domain.expense.service.ExpenseCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/expense-categories")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ExpenseCategoryController {

    private final ExpenseCategoryService expenseCategoryService;

    // Test endpoint - no security
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("ExpenseCategory controller is working!");
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ExpenseCategory> createCategory(@RequestBody ExpenseCategory category) {
        try {
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
            @RequestBody ExpenseCategory updates) {
        try {
            ExpenseCategory updated = expenseCategoryService.updateCategory(id, updates);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Error updating expense category", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
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
}
