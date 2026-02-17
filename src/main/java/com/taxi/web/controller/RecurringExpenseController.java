package com.taxi.web.controller;

import com.taxi.domain.expense.model.ExpenseCategory;
import com.taxi.domain.expense.model.RecurringExpense;
import com.taxi.domain.expense.repository.ExpenseCategoryRepository;
import com.taxi.domain.expense.service.RecurringExpenseService;
import com.taxi.web.dto.expense.ChangeRateRequest;
import com.taxi.web.dto.expense.CreateRecurringExpenseRequest;
import com.taxi.web.dto.expense.DeactivateRequest;
import com.taxi.web.dto.expense.ReactivateRequest;
import com.taxi.web.dto.expense.RecurringExpenseRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/recurring-expenses")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class RecurringExpenseController {

    private final RecurringExpenseService recurringExpenseService;
    private final ExpenseCategoryRepository expenseCategoryRepository;

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("RecurringExpense controller is working!");
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DRIVER')")
    public ResponseEntity<List<RecurringExpense>> getAllRecurringExpenses() {
        try {
            List<RecurringExpense> expenses = recurringExpenseService.findAll();
            log.info("Retrieved {} recurring expenses", expenses.size());
            return ResponseEntity.ok(expenses);
        } catch (Exception e) {
            log.error("Error getting all recurring expenses", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Create a new recurring expense
     * Application type and entity details are determined by the selected expense category
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<?> create(@Valid @RequestBody CreateRecurringExpenseRequest request) {
        try {
            RecurringExpense created = recurringExpenseService.createFromCategory(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);

        } catch (Exception e) {
            log.error("Error creating recurring expense", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Update a recurring expense
     * Application type and entity details are determined by the selected expense category
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody RecurringExpenseRequest request) {
        try {
            RecurringExpense existing = recurringExpenseService.getById(id);

            // Get the expense category - it contains all application type info
            ExpenseCategory category = expenseCategoryRepository.findById(request.getExpenseCategoryId())
                .orElseThrow(() -> new RuntimeException("Expense category not found: " + request.getExpenseCategoryId()));

            // Update all fields from request and category
            existing.setExpenseCategory(category);
            existing.setAmount(request.getAmount());
            existing.setBillingMethod(request.getBillingMethod());
            existing.setEffectiveFrom(request.getEffectiveFrom());
            existing.setEffectiveTo(request.getEffectiveTo());
            existing.setNotes(request.getNotes());

            if (request.getIsActive() != null) {
                existing.setActive(request.getIsActive());
            }

            // Copy application type settings from category
            existing.setApplicationTypeEnum(category.getApplicationType());
            existing.setShiftProfileId(category.getShiftProfileId());
            existing.setSpecificShiftId(category.getSpecificShiftId());
            existing.setSpecificPersonId(category.getSpecificPersonId());
            existing.setAttributeTypeId(category.getAttributeTypeId());

            RecurringExpense updated = recurringExpenseService.update(existing);
            return ResponseEntity.ok(updated);

        } catch (Exception e) {
            log.error("Error updating recurring expense", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            recurringExpenseService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting recurring expense", e);
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/change-rate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<?> changeRate(@PathVariable Long id, @RequestBody ChangeRateRequest request) {
        try {
            RecurringExpense newExpense = recurringExpenseService.changeRate(
                id, request.getNewAmount(), request.getEffectiveDate(), request.getNotes());
            return ResponseEntity.ok(newExpense);
        } catch (Exception e) {
            log.error("Error changing rate for recurring expense", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/deactivate-with-date")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<?> deactivateWithDate(@PathVariable Long id, @RequestBody DeactivateRequest request) {
        try {
            RecurringExpense expense = recurringExpenseService.deactivateWithEndDate(id, request.getEndDate());
            return ResponseEntity.ok(expense);
        } catch (Exception e) {
            log.error("Error deactivating recurring expense", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/reactivate-with-date")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<?> reactivateWithDate(@PathVariable Long id, @RequestBody ReactivateRequest request) {
        try {
            RecurringExpense expense = recurringExpenseService.reactivateWithDate(id, request.getEffectiveDate());
            return ResponseEntity.ok(expense);
        } catch (Exception e) {
            log.error("Error reactivating recurring expense", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<RecurringExpense> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(recurringExpenseService.getById(id));
        } catch (Exception e) {
            log.error("Error fetching recurring expense", e);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<RecurringExpense>> getActive() {
        return ResponseEntity.ok(recurringExpenseService.getActiveExpenses());
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<RecurringExpense>> getByEntity(
            @PathVariable RecurringExpense.EntityType entityType,
            @PathVariable Long entityId) {
        return ResponseEntity.ok(recurringExpenseService.getExpensesForEntity(entityType, entityId));
    }

    @GetMapping("/entity/{entityType}/{entityId}/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<RecurringExpense>> getActiveByEntity(
            @PathVariable RecurringExpense.EntityType entityType,
            @PathVariable Long entityId) {
        return ResponseEntity.ok(recurringExpenseService.getActiveExpensesForEntity(entityType, entityId));
    }

    @GetMapping("/effective")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<RecurringExpense>> getEffectiveOn(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(recurringExpenseService.getExpensesEffectiveOn(date));
    }

    @GetMapping("/entity/{entityType}/{entityId}/effective")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<RecurringExpense>> getEffectiveForEntityOn(
            @PathVariable RecurringExpense.EntityType entityType,
            @PathVariable Long entityId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(recurringExpenseService.getExpensesForEntityOn(entityType, entityId, date));
    }

    @GetMapping("/entity/{entityType}/{entityId}/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<RecurringExpense>> getHistory(
            @PathVariable RecurringExpense.EntityType entityType,
            @PathVariable Long entityId) {
        return ResponseEntity.ok(recurringExpenseService.getExpenseHistory(entityType, entityId));
    }

    @GetMapping("/category/{categoryId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<RecurringExpense>> getByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(recurringExpenseService.getExpensesByCategory(categoryId));
    }

    @PostMapping("/{id}/update-rate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<RecurringExpense> updateRate(
            @PathVariable Long id,
            @RequestParam BigDecimal newAmount,
            @RequestParam RecurringExpense.BillingMethod billingMethod,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth effectiveMonth) {
        try {
            RecurringExpense updated = recurringExpenseService.updateRate(id, newAmount, billingMethod, effectiveMonth);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Error updating recurring expense rate", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/end")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> endExpense(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            recurringExpenseService.endExpense(id, endDate);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error ending recurring expense", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        try {
            recurringExpenseService.deactivate(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deactivating recurring expense", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/reactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> reactivate(@PathVariable Long id) {
        try {
            recurringExpenseService.reactivate(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error reactivating recurring expense", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/entity/{entityType}/{entityId}/total")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<BigDecimal> calculateTotal(
            @PathVariable RecurringExpense.EntityType entityType,
            @PathVariable Long entityId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(recurringExpenseService.calculateTotalForEntity(
            entityType, entityId, startDate, endDate));
    }

    @GetMapping("/total")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<BigDecimal> calculateTotalAll(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(recurringExpenseService.calculateTotalBetween(startDate, endDate));
    }
}