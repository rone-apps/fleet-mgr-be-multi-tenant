package com.taxi.web.controller;

import com.taxi.domain.account.service.EmailService;
import com.taxi.domain.expense.service.FinancialStatementService;
import com.taxi.domain.report.service.ReportPdfService;
import com.taxi.domain.statement.model.Statement;
import com.taxi.domain.statement.repository.StatementRepository;
import com.taxi.web.dto.email.EmailReportRequest;
import com.taxi.web.dto.expense.DriverStatementDTO;
import com.taxi.web.dto.expense.OwnerReportDTO;
import java.math.BigDecimal;
import jakarta.validation.Valid;
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
import java.util.Map;
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
    private final EmailService emailService;
    private final ReportPdfService reportPdfService;

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

            // Validate date range: start date cannot be later than end date
            if (from.isAfter(to)) {
                return ResponseEntity.badRequest().body("Start date cannot be later than end date. From: " + from + ", To: " + to);
            }

            log.info("Fetching owner report for owner {} from {} to {} (period may span multiple months)", ownerId, from, to);

            // First, check if a finalized or paid statement already exists for this period
            Optional<Statement> existingStatement = statementRepository.findByPersonIdAndPeriodFromAndPeriodTo(ownerId, from, to);

            if (existingStatement.isPresent()) {
                Statement stmt = existingStatement.get();
                // Return existing finalized/paid statement converted to OwnerReportDTO
                log.info("Found existing statement ID {} for owner {} with status {}", stmt.getId(), ownerId, stmt.getStatus());
                OwnerReportDTO report = financialStatementService.convertStatementToReport(stmt);
                return ResponseEntity.ok(report);
            }

            // If no existing statement, generate a new draft report
            log.info("No existing statement found, generating new draft report for owner {} from {} to {}", ownerId, from, to);
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
                // Recalculate netDue: previousBalance + revenues - expenses - paidAmount
                BigDecimal prevBalance = statement.getPreviousBalance() != null ? statement.getPreviousBalance() : BigDecimal.ZERO;
                statement.setNetDue(prevBalance.add(statement.getTotalRevenues()).subtract(statement.getTotalExpenses()).subtract(paidAmount));
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

    /**
     * ✅ NEW: Send financial report via email with PDF attachment
     * Uses configured Spring Mail settings (Gmail SMTP)
     */
    @PostMapping("/send-report")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<?> sendReportByEmail(@Valid @RequestBody EmailReportRequest request) {
        try {
            log.info("Sending report to email: {}", request.getToEmail());

            // Generate PDF from report data
            byte[] pdfContent = reportPdfService.generateDriverReportPdf(request.getReport());

            // Build email summary (concise overview for email body)
            String emailSummary = buildReportEmailSummary(request.getReport());

            // Send email with PDF attachment
            emailService.sendDriverReport(request.getToEmail(), request.getDriverName(), emailSummary, pdfContent);

            return ResponseEntity.ok(Map.of("message", "Email sent successfully to " + request.getToEmail()));
        } catch (Exception e) {
            log.error("Error sending report by email to {}: {}", request.getToEmail(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to send email: " + e.getMessage()));
        }
    }

    /**
     * Build a concise summary for email body - plain text style for best email compatibility
     */
    private String buildReportEmailSummary(OwnerReportDTO report) {
        StringBuilder summary = new StringBuilder();

        java.math.BigDecimal totalRevenues = report.getTotalRevenues() != null ? report.getTotalRevenues() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal totalExpenses = report.getTotalExpenses() != null ? report.getTotalExpenses() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal prevBalance = report.getPreviousBalance() != null ? report.getPreviousBalance() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal paidAmount = report.getPaidAmount() != null ? report.getPaidAmount() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal netDue = report.getNetDue() != null ? report.getNetDue() : java.math.BigDecimal.ZERO;

        // Simple text format that works perfectly across all email clients
        summary.append("<pre style=\"font-family: Arial, sans-serif; font-size: 13px; line-height: 1.8; color: #333; background: none; padding: 0; border: none;\">");
        summary.append("Period:                  ").append(report.getPeriodFrom()).append(" to ").append(report.getPeriodTo()).append("\n");
        summary.append("Total Revenues:          $").append(String.format("%,.2f", totalRevenues)).append("\n");
        summary.append("Total Expenses:          $").append(String.format("%,.2f", totalExpenses)).append("\n");
        summary.append("Previous Balance:        $").append(String.format("%,.2f", prevBalance)).append("\n");
        summary.append("Amount Paid:             $").append(String.format("%,.2f", paidAmount)).append("\n");
        summary.append("─────────────────────────────────────\n");

        String netDueColor = netDue.compareTo(java.math.BigDecimal.ZERO) > 0 ? "color: #d32f2f;" : "color: #388e3c;";
        summary.append("<strong style=\"").append(netDueColor).append(" font-size: 14px;\">Net Due:                 $").append(String.format("%,.2f", netDue.abs())).append("</strong>\n");
        summary.append("</pre>");

        return summary.toString();
    }

    /**
     * Get finalized statements for a date period (for driver payment batch processing)
     */
    @GetMapping("/statements/period")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<Statement>> getStatementsByPeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String status) {

        try {
            List<Statement> statements;
            if (status != null && !status.isEmpty()) {
                // Query with status filter
                com.taxi.domain.statement.model.StatementStatus statementStatus =
                    com.taxi.domain.statement.model.StatementStatus.valueOf(status);
                statements = statementRepository.findByPeriodAndStatus(from, to, statementStatus);
            } else {
                // Query all statements in period (default to FINALIZED)
                statements = statementRepository.findByPeriod(from, to);
                // Filter to only finalized statements
                statements = statements.stream()
                    .filter(s -> s.getStatus() == com.taxi.domain.statement.model.StatementStatus.FINALIZED)
                    .toList();
            }
            return ResponseEntity.ok(statements);
        } catch (Exception e) {
            log.error("Error fetching statements for period", e);
            return ResponseEntity.badRequest().body(List.of());
        }
    }
}
