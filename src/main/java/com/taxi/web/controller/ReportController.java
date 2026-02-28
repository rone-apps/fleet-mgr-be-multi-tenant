package com.taxi.web.controller;

import com.taxi.domain.report.dto.LeaseReconciliationReportDTO;
import com.taxi.domain.report.service.FixedExpenseReportService;
import com.taxi.domain.report.service.LeaseReconciliationService;
import com.taxi.domain.report.service.ReportService;
import com.taxi.web.dto.report.ChargesRevenueReportDTO;
import com.taxi.web.dto.report.CreditCardRevenueReportDTO;
import com.taxi.web.dto.report.FixedExpenseReportDTO;
import com.taxi.web.dto.report.LeaseRevenueReportDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * REST Controller for financial reports
 * SIMPLIFIED VERSION - NO PERMISSION CHECKS (for testing)
 */
@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ReportController {

    private final ReportService reportService;
    private final FixedExpenseReportService fixedExpenseReportService;
    private final LeaseReconciliationService leaseReconciliationService;

    /**
     * Generate lease revenue report for a cab owner
     * GET /api/reports/lease-revenue?ownerDriverNumber=DRV-001&startDate=2024-01-01&endDate=2024-01-31
     */
    @GetMapping("/lease-revenue")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DRIVER')")
    public ResponseEntity<LeaseRevenueReportDTO> getLeaseRevenueReport(
            @RequestParam String ownerDriverNumber,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("📊 Lease revenue report request: owner={}, start={}, end={}", 
                ownerDriverNumber, startDate, endDate);
        
        try {
            // Validate date range
            if (endDate.isBefore(startDate)) {
                log.error("❌ Invalid date range: end date before start date");
                return ResponseEntity.badRequest().build();
            }
            
            LeaseRevenueReportDTO report = reportService.generateLeaseRevenueReport(
                    ownerDriverNumber, startDate, endDate);
            
            log.info("✅ Lease revenue report generated: {} shifts", 
                    report.getTotalShifts());
            
            return ResponseEntity.ok(report);
            
        } catch (IllegalArgumentException e) {
            log.error("❌ Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("❌ Error generating lease revenue report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Generate charges revenue report for a driver
     * GET /api/reports/charges-revenue?driverNumber=DRV-001&startDate=2024-01-01&endDate=2024-01-31
     */
    @GetMapping("/charges-revenue")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DRIVER')")
    public ResponseEntity<ChargesRevenueReportDTO> getChargesRevenueReport(
            @RequestParam String driverNumber,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("📊 Charges revenue report request: driver={}, start={}, end={}", 
                driverNumber, startDate, endDate);
        
        try {
            // Validate date range
            if (endDate.isBefore(startDate)) {
                log.error("❌ Invalid date range: end date before start date");
                return ResponseEntity.badRequest().build();
            }
            
            ChargesRevenueReportDTO report = reportService.generateChargesRevenueReport(
                    driverNumber, startDate, endDate);
            
            log.info("✅ Charges revenue report generated: {} charges", 
                    report.getTotalCharges());
            
            return ResponseEntity.ok(report);
            
        } catch (IllegalArgumentException e) {
            log.error("❌ Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("❌ Error generating charges revenue report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Generate credit card revenue report for a driver
     * GET /api/reports/credit-card-revenue?driverNumber=DRV-001&startDate=2024-01-01&endDate=2024-01-31
     */
    @GetMapping("/credit-card-revenue")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DRIVER')")
    public ResponseEntity<CreditCardRevenueReportDTO> getCreditCardRevenueReport(
            @RequestParam String driverNumber,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("📊 Credit card revenue report request: driver={}, start={}, end={}", 
                driverNumber, startDate, endDate);
        
        try {
            // Validate date range
            if (endDate.isBefore(startDate)) {
                log.error("❌ Invalid date range: end date before start date");
                return ResponseEntity.badRequest().build();
            }
            
            CreditCardRevenueReportDTO report = reportService.generateCreditCardRevenueReport(
                    driverNumber, startDate, endDate);
            
            log.info("✅ Credit card revenue report generated: {} transactions", 
                    report.getTotalTransactions());
            
            return ResponseEntity.ok(report);
            
        } catch (IllegalArgumentException e) {
            log.error("❌ Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("❌ Error generating credit card revenue report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Generate fixed expense report for a driver
     * GET /api/reports/fixed-expenses?driverNumber=DRV-001&startDate=2024-01-01&endDate=2024-03-31
     */
    @GetMapping("/fixed-expenses")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DRIVER')")
    public ResponseEntity<FixedExpenseReportDTO> getFixedExpenseReport(
            @RequestParam String driverNumber,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("📊 Fixed expense report request: driver={}, start={}, end={}", 
                driverNumber, startDate, endDate);
        
        try {
            // Validate date range
            if (endDate.isBefore(startDate)) {
                log.error("❌ Invalid date range: end date before start date");
                return ResponseEntity.badRequest().build();
            }
            
            // Generate the report
            FixedExpenseReportDTO report = fixedExpenseReportService
                    .generateFixedExpenseReport(driverNumber, startDate, endDate);
            
            log.info("✅ Fixed expense report generated: {} items", 
                    report.getTotalExpenses());
            
            return ResponseEntity.ok(report);
            
        } catch (IllegalArgumentException e) {
            log.error("❌ Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("❌ Error generating fixed expense report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Generate lease reconciliation report
     * Shows shift-by-shift breakdown of driver lease expenses vs. owner lease revenues
     * GET /api/reports/lease-reconciliation?startDate=2026-02-01&endDate=2026-02-27
     */
    @GetMapping("/lease-reconciliation")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'MANAGER')")
    public ResponseEntity<LeaseReconciliationReportDTO> getLeaseReconciliation(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("📋 Lease reconciliation report request: start={}, end={}", startDate, endDate);

        try {
            if (endDate.isBefore(startDate)) {
                log.error("❌ Invalid date range: end date before start date");
                return ResponseEntity.badRequest().build();
            }

            LeaseReconciliationReportDTO report = leaseReconciliationService.generateReport(startDate, endDate);

            log.info("✅ Lease reconciliation report generated: {} shifts, {} matched, {} no_owner",
                    report.getTotalShifts(), report.getMatchedCount(), report.getNoOwnerCount());

            return ResponseEntity.ok(report);

        } catch (Exception e) {
            log.error("❌ Error generating lease reconciliation report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}