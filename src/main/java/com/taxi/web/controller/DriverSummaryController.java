package com.taxi.web.controller;

import com.taxi.domain.report.ReportJobStatus;
import com.taxi.domain.report.service.ReportCacheService;
import com.taxi.domain.report.service.ReportService;
import com.taxi.web.dto.report.DriverSummaryReportDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for driver summary reports
 */
@RestController
@RequestMapping("/reports/driver-summary")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class DriverSummaryController {

    private final ReportService reportService;
    private final ReportCacheService reportCacheService;

    /**
     * Get comprehensive driver summary report for active drivers with pagination
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_ACCOUNTANT', 'ROLE_DRIVER')")
    public ResponseEntity<DriverSummaryReportDTO> getDriverSummaryReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "ALL") String personType,
            @RequestParam(defaultValue = "false") boolean quickMode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "lastName") String sort,
            @RequestParam(defaultValue = "asc") String direction) {

        log.info("=== Driver Summary Report Request ===");
        log.info("StartDate: {}, EndDate: {}, Page: {}, Size: {}, Sort: {}, Direction: {}",
                startDate, endDate, page, size, sort, direction);

        // Validate dates
        if (startDate.isAfter(endDate)) {
            log.warn("Invalid date range: start date {} is after end date {}", startDate, endDate);
            return ResponseEntity.badRequest().build();
        }

        // Validate page and size
        if (page < 0 || size < 1 || size > 100) {
            log.warn("Invalid pagination parameters: page={}, size={}", page, size);
            return ResponseEntity.badRequest().build();
        }

        try {
            // Validate person type
            if (!personType.matches("(?i)DRIVER|OWNER|ALL")) {
                log.warn("Invalid personType: {}", personType);
                return ResponseEntity.badRequest().build();
            }

            // Create pageable with sorting
            Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction)
                    ? Sort.Direction.DESC
                    : Sort.Direction.ASC;

            // Map driverName to lastName since Driver entity uses firstName/lastName
            String sortField = sort;
            if ("driverName".equalsIgnoreCase(sort)) {
                sortField = "lastName";
            }

            Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortField));

            log.info("Calling report service... personType={}, quickMode={}", personType, quickMode);
            DriverSummaryReportDTO report = reportService.generateDriverSummaryReportPaginated(
                    startDate, endDate, pageable, personType.toUpperCase(), quickMode);

            log.info("Successfully generated driver summary report with {} drivers (page {} of {}) [{}]",
                    report.getDriverSummaries().size(),
                    report.getCurrentPage() + 1,
                    report.getTotalPages(),
                    quickMode ? "QUICK MODE" : "FULL MODE");

            return ResponseEntity.ok(report);

        } catch (Exception e) {
            log.error("Error generating driver summary report: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ASYNC REPORT GENERATION ENDPOINTS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Start async report generation. Returns jobId immediately.
     * POST /reports/driver-summary/generate
     */
    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_ACCOUNTANT', 'ROLE_DRIVER')")
    public ResponseEntity<Map<String, Object>> generateReportAsync(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "ALL") String personType,
            @RequestParam(defaultValue = "false") boolean quickMode,
            @RequestParam(defaultValue = "lastName") String sort,
            @RequestParam(defaultValue = "asc") String direction) {

        if (startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest().build();
        }
        if (!personType.matches("(?i)DRIVER|OWNER|ALL")) {
            return ResponseEntity.badRequest().build();
        }

        String jobId = UUID.randomUUID().toString();
        ReportJobStatus.create(jobId);

        log.info("Starting async report generation - jobId: {}, dates: {} to {}", jobId, startDate, endDate);

        reportService.generateReportAsync(jobId, startDate, endDate,
                personType.toUpperCase(), quickMode, sort, direction);

        Map<String, Object> response = new HashMap<>();
        response.put("jobId", jobId);
        response.put("status", "PENDING");

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Poll job status.
     * GET /reports/driver-summary/jobs/{jobId}/status
     */
    @GetMapping("/jobs/{jobId}/status")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_ACCOUNTANT', 'ROLE_DRIVER')")
    public ResponseEntity<Map<String, Object>> getJobStatus(@PathVariable String jobId) {
        ReportJobStatus jobStatus = ReportJobStatus.get(jobId);
        if (jobStatus == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("jobId", jobId);
        response.put("status", jobStatus.getStatus().name());
        response.put("totalDrivers", jobStatus.getTotalDrivers());
        response.put("processedDrivers", jobStatus.getProcessedDrivers());
        response.put("progressPercent", jobStatus.getProgressPercent());
        response.put("totalPages", jobStatus.getTotalPages());
        response.put("pagesReady", jobStatus.getPagesReady());
        response.put("message", jobStatus.getMessage());
        if (!jobStatus.getErrors().isEmpty()) {
            response.put("errors", jobStatus.getErrors());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Fetch a cached report page.
     * GET /reports/driver-summary/jobs/{jobId}/page/{pageNum}
     */
    @GetMapping("/jobs/{jobId}/page/{pageNum}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_ACCOUNTANT', 'ROLE_DRIVER')")
    public ResponseEntity<DriverSummaryReportDTO> getCachedPage(
            @PathVariable String jobId,
            @PathVariable int pageNum) {

        ReportJobStatus jobStatus = ReportJobStatus.get(jobId);
        if (jobStatus == null) {
            return ResponseEntity.notFound().build();
        }

        DriverSummaryReportDTO page = reportCacheService.getPage(jobId, pageNum);
        if (page == null) {
            return ResponseEntity.notFound().build();
        }

        // If this is the last page and job is completed, include grand totals
        if (jobStatus.getStatus() == ReportJobStatus.Status.COMPLETED) {
            DriverSummaryReportDTO grandTotals = reportCacheService.getGrandTotals(jobId);
            if (grandTotals != null) {
                page.setGrandTotalLeaseRevenue(grandTotals.getGrandTotalLeaseRevenue());
                page.setGrandTotalCreditCardRevenue(grandTotals.getGrandTotalCreditCardRevenue());
                page.setGrandTotalChargesRevenue(grandTotals.getGrandTotalChargesRevenue());
                page.setGrandTotalOtherRevenue(grandTotals.getGrandTotalOtherRevenue());
                page.setGrandTotalFixedExpense(grandTotals.getGrandTotalFixedExpense());
                page.setGrandTotalLeaseExpense(grandTotals.getGrandTotalLeaseExpense());
                page.setGrandTotalVariableExpense(grandTotals.getGrandTotalVariableExpense());
                page.setGrandTotalOtherExpense(grandTotals.getGrandTotalOtherExpense());
                page.setGrandTotalRevenue(grandTotals.getGrandTotalRevenue());
                page.setGrandTotalExpense(grandTotals.getGrandTotalExpense());
                page.setGrandNetOwed(grandTotals.getGrandNetOwed());
                page.setGrandTotalPaid(grandTotals.getGrandTotalPaid());
                page.setGrandTotalOutstanding(grandTotals.getGrandTotalOutstanding());
            }
        }

        return ResponseEntity.ok(page);
    }

    /**
     * Invalidate a cached report job.
     * DELETE /reports/driver-summary/jobs/{jobId}
     */
    @DeleteMapping("/jobs/{jobId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_ACCOUNTANT', 'ROLE_DRIVER')")
    public ResponseEntity<Void> invalidateJob(@PathVariable String jobId) {
        log.info("Invalidating report cache for job: {}", jobId);
        reportCacheService.remove(jobId);
        return ResponseEntity.noContent().build();
    }
}
