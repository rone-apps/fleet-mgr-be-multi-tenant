package com.taxi.web.controller;

import com.taxi.domain.expense.model.ExpenseCategory;
import com.taxi.domain.expense.model.OneTimeExpense;
import com.taxi.domain.expense.repository.ExpenseCategoryRepository;
import com.taxi.domain.expense.service.OneTimeExpenseService;
import com.taxi.web.dto.expense.CreateOneTimeExpenseRequest;
import com.taxi.web.dto.expense.OneTimeExpenseRequest;
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
import java.util.List;

/**
 * REST controller for managing one-time (variable) expenses.
 */
@RestController
@RequestMapping("/one-time-expenses")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class OneTimeExpenseController {

    private final OneTimeExpenseService oneTimeExpenseService;
    private final ExpenseCategoryRepository expenseCategoryRepository;

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("OneTimeExpense controller is working!");
    }

    /**
     * Create a new one-time expense
     * Charges to entities via application type determined by expense category
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DISPATCHER', 'ACCOUNTANT')")
    public ResponseEntity<?> create(@Valid @RequestBody CreateOneTimeExpenseRequest request) {
        try {
            OneTimeExpense created = oneTimeExpenseService.createFromCategory(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);

        } catch (Exception e) {
            log.error("Error creating one-time expense", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DISPATCHER')")
    public ResponseEntity<OneTimeExpense> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(oneTimeExpenseService.getById(id));
        } catch (Exception e) {
            log.error("Error fetching one-time expense", e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Update a one-time expense
     * ✅ UPDATED - Accepts OneTimeExpenseRequest and properly sets FK fields
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody OneTimeExpenseRequest request) {
        try {
            OneTimeExpense existing = oneTimeExpenseService.getById(id);

            // Validate application type and required fields
            if (request.getApplicationType() == null) {
                throw new RuntimeException("Application type is required to charge the expense");
            }

            // Validate based on application type
            switch (request.getApplicationType()) {
                case SHIFT_PROFILE:
                    if (request.getShiftProfileId() == null) {
                        throw new RuntimeException("Shift profile ID is required for SHIFT_PROFILE application type");
                    }
                    break;
                case SPECIFIC_SHIFT:
                    if (request.getSpecificShiftId() == null) {
                        throw new RuntimeException("Specific shift ID is required for SPECIFIC_SHIFT application type");
                    }
                    break;
                case SPECIFIC_OWNER_DRIVER:
                    if ((request.getSpecificOwnerId() == null && request.getSpecificDriverId() == null) ||
                        (request.getSpecificOwnerId() != null && request.getSpecificDriverId() != null)) {
                        throw new RuntimeException("Exactly one of owner ID or driver ID must be set for SPECIFIC_OWNER_DRIVER application type");
                    }
                    break;
                case ALL_ACTIVE_SHIFTS:
                case ALL_NON_OWNER_DRIVERS:
                    // No additional validation needed
                    break;
            }

            // Update all fields from request
            existing.setName(request.getName());
            existing.setAmount(request.getAmount());
            existing.setExpenseDate(request.getExpenseDate());
            existing.setPaidBy(request.getPaidBy());
            existing.setResponsibleParty(request.getResponsibleParty());
            existing.setDescription(request.getDescription());
            existing.setVendor(request.getVendor());
            existing.setReceiptUrl(request.getReceiptUrl());
            existing.setInvoiceNumber(request.getInvoiceNumber());
            existing.setReimbursable(request.getIsReimbursable() != null && request.getIsReimbursable());
            existing.setNotes(request.getNotes());
            // Application type fields
            existing.setApplicationType(request.getApplicationType());
            existing.setShiftProfileId(request.getShiftProfileId());
            existing.setSpecificShiftId(request.getSpecificShiftId());
            existing.setSpecificOwnerId(request.getSpecificOwnerId());
            existing.setSpecificDriverId(request.getSpecificDriverId());

            OneTimeExpense updated = oneTimeExpenseService.update(id, existing);
            return ResponseEntity.ok(updated);

        } catch (Exception e) {
            log.error("Error updating one-time expense", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            oneTimeExpenseService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting one-time expense", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<OneTimeExpense>> getByEntity(
            @PathVariable OneTimeExpense.EntityType entityType,
            @PathVariable Long entityId) {
        return ResponseEntity.ok(oneTimeExpenseService.getExpensesForEntity(entityType, entityId));
    }

    @GetMapping("/entity/{entityType}/{entityId}/between")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<OneTimeExpense>> getByEntityBetween(
            @PathVariable OneTimeExpense.EntityType entityType,
            @PathVariable Long entityId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(oneTimeExpenseService.getExpensesForEntityBetween(
            entityType, entityId, startDate, endDate));
    }

    @GetMapping("/between")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<OneTimeExpense>> getBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(oneTimeExpenseService.getExpensesBetween(startDate, endDate));
    }

    @GetMapping("/driver/between")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<OneTimeExpense>> getBetweenForDriver(
            @RequestParam String driverNumber,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(oneTimeExpenseService.getExpensesForDriverBetween(driverNumber, startDate, endDate));
    }

    @GetMapping("/category/{categoryId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<OneTimeExpense>> getByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(oneTimeExpenseService.getExpensesByCategory(categoryId));
    }

    @GetMapping("/responsible-party/{responsibleParty}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<OneTimeExpense>> getByResponsibleParty(
            @PathVariable OneTimeExpense.ResponsibleParty responsibleParty,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(oneTimeExpenseService.getExpensesByResponsibleParty(
            responsibleParty, startDate, endDate));
    }

    @GetMapping("/entity/{entityType}/{entityId}/total")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<BigDecimal> calculateTotal(
            @PathVariable OneTimeExpense.EntityType entityType,
            @PathVariable Long entityId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(oneTimeExpenseService.calculateTotalForEntity(
            entityType, entityId, startDate, endDate));
    }

    @GetMapping("/category/{categoryId}/total")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<BigDecimal> calculateTotalByCategory(
            @PathVariable Long categoryId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(oneTimeExpenseService.calculateTotalByCategory(
            categoryId, startDate, endDate));
    }

    @GetMapping("/pending-reimbursements")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<OneTimeExpense>> getPendingReimbursements() {
        return ResponseEntity.ok(oneTimeExpenseService.getPendingReimbursements());
    }

    @GetMapping("/pending-reimbursements/paid-by/{paidBy}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<OneTimeExpense>> getPendingReimbursementsByPaidBy(
            @PathVariable OneTimeExpense.PaidBy paidBy) {
        return ResponseEntity.ok(oneTimeExpenseService.getPendingReimbursementsByPaidBy(paidBy));
    }

    @GetMapping("/without-receipts")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<OneTimeExpense>> getWithoutReceipts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sinceDate) {
        return ResponseEntity.ok(oneTimeExpenseService.getExpensesWithoutReceipts(sinceDate));
    }

    @PostMapping("/{id}/reimburse")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<Void> markAsReimbursed(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reimbursementDate) {
        try {
            oneTimeExpenseService.markAsReimbursed(id, reimbursementDate);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error marking expense as reimbursed", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/set-reimbursable")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> setReimbursable(
            @PathVariable Long id,
            @RequestParam boolean reimbursable) {
        try {
            oneTimeExpenseService.setReimbursable(id, reimbursable);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error setting expense reimbursable status", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/attach-receipt")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DISPATCHER')")
    public ResponseEntity<Void> attachReceipt(
            @PathVariable Long id,
            @RequestParam String receiptUrl) {
        try {
            oneTimeExpenseService.attachReceipt(id, receiptUrl);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error attaching receipt", e);
            return ResponseEntity.badRequest().build();
        }
    }
}