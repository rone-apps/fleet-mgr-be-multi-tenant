package com.taxi.web.controller;

import com.taxi.domain.expense.model.LeaseExpense;
import com.taxi.domain.expense.service.LeaseExpenseService;
import com.taxi.web.dto.expense.CreateLeaseExpenseRequest;
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

@RestController
@RequestMapping("/api/lease-expenses")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class LeaseExpenseController {

    private final LeaseExpenseService leaseExpenseService;

    /**
     * Create a lease expense
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<LeaseExpense> createLeaseExpense(@RequestBody CreateLeaseExpenseRequest request) {
        try {
            LeaseExpense created = leaseExpenseService.createLeaseExpense(
                request.getDriverId(),
                request.getCabId(),
                request.getLeaseDate(),
                request.getShiftType(),
                request.getMilesDriven(),
                request.getShiftId(),
                request.getNotes()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            log.error("Error creating lease expense", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get lease expense by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DRIVER')")
    public ResponseEntity<LeaseExpense> getLeaseExpense(@PathVariable Long id) {
        try {
            LeaseExpense leaseExpense = leaseExpenseService.getById(id);
            return ResponseEntity.ok(leaseExpense);
        } catch (Exception e) {
            log.error("Error getting lease expense: {}", id, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get lease expenses between dates
     */
    @GetMapping("/between")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<LeaseExpense>> getLeaseExpensesBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        try {
            List<LeaseExpense> expenses = leaseExpenseService.getLeaseExpensesBetween(startDate, endDate);
            return ResponseEntity.ok(expenses);
        } catch (Exception e) {
            log.error("Error getting lease expenses between dates", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get lease expenses for a driver
     */
    @GetMapping("/driver/{driverId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DRIVER')")
    public ResponseEntity<List<LeaseExpense>> getLeaseExpensesByDriver(@PathVariable Long driverId) {
        try {
            List<LeaseExpense> expenses = leaseExpenseService.getLeaseExpensesByDriver(driverId);
            return ResponseEntity.ok(expenses);
        } catch (Exception e) {
            log.error("Error getting lease expenses for driver: {}", driverId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get lease expenses for a driver between dates
     */
    @GetMapping("/driver/{driverId}/between")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DRIVER')")
    public ResponseEntity<List<LeaseExpense>> getLeaseExpensesByDriverBetween(
            @PathVariable Long driverId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        try {
            List<LeaseExpense> expenses = leaseExpenseService.getLeaseExpensesByDriverBetween(
                driverId, startDate, endDate
            );
            return ResponseEntity.ok(expenses);
        } catch (Exception e) {
            log.error("Error getting lease expenses for driver between dates", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get unpaid lease expenses
     */
    @GetMapping("/unpaid")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<LeaseExpense>> getUnpaidLeaseExpenses() {
        try {
            List<LeaseExpense> expenses = leaseExpenseService.getUnpaidLeaseExpenses();
            return ResponseEntity.ok(expenses);
        } catch (Exception e) {
            log.error("Error getting unpaid lease expenses", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get unpaid lease expenses for a driver
     */
    @GetMapping("/driver/{driverId}/unpaid")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DRIVER')")
    public ResponseEntity<List<LeaseExpense>> getUnpaidLeaseExpensesByDriver(@PathVariable Long driverId) {
        try {
            List<LeaseExpense> expenses = leaseExpenseService.getUnpaidLeaseExpensesByDriver(driverId);
            return ResponseEntity.ok(expenses);
        } catch (Exception e) {
            log.error("Error getting unpaid lease expenses for driver: {}", driverId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get total unpaid amount for a driver
     */
    @GetMapping("/driver/{driverId}/unpaid-total")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DRIVER')")
    public ResponseEntity<BigDecimal> getTotalUnpaidByDriver(@PathVariable Long driverId) {
        try {
            BigDecimal total = leaseExpenseService.getTotalUnpaidByDriver(driverId);
            return ResponseEntity.ok(total);
        } catch (Exception e) {
            log.error("Error getting total unpaid for driver: {}", driverId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Mark lease expense as paid
     */
    @PostMapping("/{id}/mark-paid")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<LeaseExpense> markAsPaid(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate paidDate
    ) {
        try {
            LeaseExpense updated = leaseExpenseService.markAsPaid(id, paidDate);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Error marking lease expense as paid: {}", id, e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Mark lease expense as unpaid
     */
    @PostMapping("/{id}/mark-unpaid")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<LeaseExpense> markAsUnpaid(@PathVariable Long id) {
        try {
            LeaseExpense updated = leaseExpenseService.markAsUnpaid(id);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Error marking lease expense as unpaid: {}", id, e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update notes
     */
    @PutMapping("/{id}/notes")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<LeaseExpense> updateNotes(
            @PathVariable Long id,
            @RequestBody String notes
    ) {
        try {
            LeaseExpense updated = leaseExpenseService.updateNotes(id, notes);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Error updating notes for lease expense: {}", id, e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Delete lease expense
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteLeaseExpense(@PathVariable Long id) {
        try {
            leaseExpenseService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting lease expense: {}", id, e);
            return ResponseEntity.badRequest().build();
        }
    }
}
