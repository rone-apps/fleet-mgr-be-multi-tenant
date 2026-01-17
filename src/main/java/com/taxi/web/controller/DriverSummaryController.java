package com.taxi.web.controller;

import com.taxi.domain.report.service.ReportService;
import com.taxi.web.dto.report.DriverSummaryReportDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

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

    /**
     * Get comprehensive driver summary report for active drivers with pagination
     * 
     * GET /api/reports/driver-summary?startDate=2024-01-01&endDate=2024-01-31&page=0&size=25
     * 
     * @param startDate Start date for the report period (required)
     * @param endDate End date for the report period (required)
     * @param page Page number (default: 0)
     * @param size Page size (default: 25)
     * @param sort Sort field (default: driverName)
     * @param direction Sort direction (default: asc)
     * @return Paginated driver summary report with all financial metrics
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_ACCOUNTANT', 'ROLE_DRIVER')")
    public ResponseEntity<DriverSummaryReportDTO> getDriverSummaryReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
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
            
            log.info("Calling report service...");
            DriverSummaryReportDTO report = reportService.generateDriverSummaryReportPaginated(
                    startDate, endDate, pageable);
            
            log.info("Successfully generated driver summary report with {} drivers (page {} of {})", 
                    report.getDriverSummaries().size(),
                    report.getCurrentPage() + 1,
                    report.getTotalPages());
            
            return ResponseEntity.ok(report);
            
        } catch (Exception e) {
            log.error("Error generating driver summary report: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}