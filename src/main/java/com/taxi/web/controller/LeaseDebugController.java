package com.taxi.web.controller;

import com.taxi.domain.report.service.LeaseDebugReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Debug endpoint for lease expense/revenue matching
 */
@RestController
@RequestMapping("/api/reports/lease-debug")
@RequiredArgsConstructor
@Slf4j
public class LeaseDebugController {

    private final LeaseDebugReportService leaseDebugReportService;

    /**
     * Generate debug report for lease matching issues
     * Shows detailed shift-by-shift comparison of expense vs revenue calculations
     */
    @GetMapping("/matching")
    public ResponseEntity<LeaseDebugReportService.LeaseDebugReport> getLeaseMatchingDebug(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("🔍 Lease debug report requested for {} to {}", startDate, endDate);

        LeaseDebugReportService.LeaseDebugReport report = leaseDebugReportService.generateDebugReport(startDate, endDate);

        return ResponseEntity.ok(report);
    }
}
