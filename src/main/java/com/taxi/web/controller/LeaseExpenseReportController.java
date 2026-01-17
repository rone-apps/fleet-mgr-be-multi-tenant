package com.taxi.web.controller;

import com.taxi.domain.report.service.ReportService;
import com.taxi.web.dto.report.LeaseExpenseReportDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * REST controller for lease expense reports
 */
@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class LeaseExpenseReportController {

    private final ReportService reportService;

    /**
     * Generate lease expense report for a driver
     * Shows all shifts where this driver drove shifts owned by OTHER drivers
     * 
     * GET /api/reports/lease-expense?driverNumber=DRV-001&startDate=2024-01-01&endDate=2024-01-31
     * 
     * Business Logic:
     * - If driver drives their OWN shift → NO lease expense
     * - If driver drives someone else's shift → Lease expense owed to owner
     * 
     * @param driverNumber The working driver's number
     * @param startDate Start date for the report period
     * @param endDate End date for the report period
     * @return Lease expense report with all shifts and totals
     */
    @GetMapping("/lease-expense")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DRIVER')")
    public ResponseEntity<LeaseExpenseReportDTO> getLeaseExpenseReport(
            @RequestParam String driverNumber,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("Received request for lease expense report: driver={}, start={}, end={}", 
                driverNumber, startDate, endDate);
        
        try {
            // Validate date range
            if (endDate.isBefore(startDate)) {
                log.error("Invalid date range: end date before start date");
                return ResponseEntity.badRequest().build();
            }
            
            // Generate the report
            LeaseExpenseReportDTO report = reportService.generateLeaseExpenseReport(
                    driverNumber, startDate, endDate);
            
            log.info("Successfully generated lease expense report with {} shifts", 
                    report.getTotalShifts());
            
            return ResponseEntity.ok(report);
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error generating lease expense report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
