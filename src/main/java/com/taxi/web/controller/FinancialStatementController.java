package com.taxi.web.controller;

import com.taxi.domain.expense.service.FinancialStatementService;
import com.taxi.domain.statement.model.Statement;
import com.taxi.domain.statement.repository.StatementRepository;
import com.taxi.web.dto.expense.DriverStatementDTO;
import com.taxi.web.dto.expense.OwnerReportDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/financial-statements")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class FinancialStatementController {

    private final FinancialStatementService financialStatementService;
    private final StatementRepository statementRepository;
    private final com.taxi.domain.driver.repository.DriverRepository driverRepository;

    /**
     * Generate a financial statement for a driver for a date period
     */
    @GetMapping("/driver/{driverId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<?> getDriverStatement(
            @PathVariable Long driverId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        try {
            // Default to current month if not provided
            if (from == null || to == null) {
                YearMonth now = YearMonth.now();
                from = now.atDay(1);
                to = now.atEndOfMonth();
            }

            log.info("Generating driver statement for driver {} from {} to {}", driverId, from, to);
            DriverStatementDTO statement = financialStatementService.generateDriverStatement(driverId, from, to);
            return ResponseEntity.ok(statement);

        } catch (Exception e) {
            log.error("Error generating driver statement", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Generate a financial statement for an owner for a date period
     */
    @GetMapping("/owner/{ownerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<?> getOwnerStatement(
            @PathVariable Long ownerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        try {
            // Default to current month if not provided
            if (from == null || to == null) {
                YearMonth now = YearMonth.now();
                from = now.atDay(1);
                to = now.atEndOfMonth();
            }

            log.info("Generating owner statement for owner {} from {} to {}", ownerId, from, to);
            DriverStatementDTO statement = financialStatementService.generateOwnerStatement(ownerId, from, to);
            return ResponseEntity.ok(statement);

        } catch (Exception e) {
            log.error("Error generating owner statement", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Generate a comprehensive financial report for an owner (draft mode)
     * Always recalculates the statement for the given period
     * Automatically populates previousBalance from last finalized statement
     */
    @GetMapping("/owner-report/{ownerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DRIVER')")
    public ResponseEntity<?> getOwnerReport(
            @PathVariable Long ownerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        try {
            // Default to current month if not provided
            if (from == null || to == null) {
                YearMonth now = YearMonth.now();
                from = now.atDay(1);
                to = now.atEndOfMonth();
            }

            log.info("Generating owner report (draft) for owner {} from {} to {}", ownerId, from, to);
            OwnerReportDTO report = financialStatementService.generateOwnerReport(ownerId, from, to);
            return ResponseEntity.ok(report);

        } catch (Exception e) {
            log.error("Error generating owner report", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Get owner report by driver number (for dashboard modal popup)
     * Looks up driver by number and returns their detailed financial report
     */
    @GetMapping("/owner-report/by-number/{driverNumber}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DRIVER')")
    public ResponseEntity<?> getOwnerReportByNumber(
            @PathVariable String driverNumber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        try {
            // Default to current month if not provided
            if (from == null || to == null) {
                YearMonth now = YearMonth.now();
                from = now.atDay(1);
                to = now.atEndOfMonth();
            }

            // Look up driver by number
            com.taxi.domain.driver.model.Driver driver = driverRepository.findByDriverNumber(driverNumber)
                    .orElseThrow(() -> new RuntimeException("Driver not found: " + driverNumber));

            log.info("Generating owner report for driver {} (ID: {}) from {} to {}", driverNumber, driver.getId(), from, to);
            OwnerReportDTO report = financialStatementService.generateOwnerReport(driver.getId(), from, to);
            return ResponseEntity.ok(report);

        } catch (Exception e) {
            log.error("Error generating owner report for driver {}", driverNumber, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Finalize an owner report and save it to the database
     * This freezes the numbers and makes future viewing fast
     */
    @PostMapping("/owner-report/{ownerId}/finalize")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DRIVER')")
    public ResponseEntity<?> finalizeOwnerReport(
            @PathVariable Long ownerId,
            @RequestBody OwnerReportDTO report) {
        try {
            log.info("Finalizing owner report for owner {}", ownerId);
            Statement statement = financialStatementService.finalizeStatement(report);
            return ResponseEntity.ok(statement);

        } catch (Exception e) {
            log.error("Error finalizing owner report", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * List all saved statements for a person (driver or owner)
     */
    @GetMapping("/statements/{personId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DRIVER')")
    public ResponseEntity<?> listStatements(@PathVariable Long personId) {
        try {
            log.info("Listing statements for person {}", personId);
            List<Statement> statements = statementRepository.findByPersonIdOrderByPeriodToDesc(personId);
            return ResponseEntity.ok(statements);

        } catch (Exception e) {
            log.error("Error listing statements", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Fetch a single saved statement by ID
     */
    @GetMapping("/statements/detail/{statementId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DRIVER')")
    public ResponseEntity<?> getStatement(@PathVariable Long statementId) {
        try {
            log.info("Fetching statement {}", statementId);
            Optional<Statement> statement = statementRepository.findById(statementId);
            if (statement.isPresent()) {
                return ResponseEntity.ok(statement.get());
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            log.error("Error fetching statement", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Update the paidAmount for a finalized statement
     */
    @PutMapping("/statements/{statementId}/paid")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<?> updatePaidAmount(
            @PathVariable Long statementId,
            @RequestParam BigDecimal paidAmount) {
        try {
            log.info("Updating paid amount for statement {} to {}", statementId, paidAmount);
            Optional<Statement> statementOpt = statementRepository.findById(statementId);
            if (statementOpt.isPresent()) {
                Statement statement = statementOpt.get();
                statement.setPaidAmount(paidAmount);
                // Recalculate netDue
                BigDecimal prevBalance = statement.getPreviousBalance() != null ? statement.getPreviousBalance() : BigDecimal.ZERO;
                statement.setNetDue(prevBalance.add(statement.getTotalExpenses()).subtract(statement.getTotalRevenues()).subtract(paidAmount));
                statementRepository.save(statement);
                return ResponseEntity.ok(statement);
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            log.error("Error updating paid amount", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
