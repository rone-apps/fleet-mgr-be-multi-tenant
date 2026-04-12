package com.taxi.web.controller;

import com.taxi.domain.expense.service.AnalyticsService;
import com.taxi.web.dto.expense.AnalyticsDTO;
import com.taxi.web.dto.expense.DriverPerformanceDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * Generate analytics dashboard metrics
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<?> getDashboardAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        try {
            // Default to current month if not provided
            if (from == null || to == null) {
                YearMonth now = YearMonth.now();
                from = now.atDay(1);
                to = now.atEndOfMonth();
            }

            log.info("Generating analytics dashboard from {} to {}", from, to);
            AnalyticsDTO analytics = analyticsService.generateAnalytics(from, to);
            return ResponseEntity.ok(analytics);

        } catch (Exception e) {
            log.error("Error generating analytics", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Generate driver performance metrics
     */
    @GetMapping("/driver-performance")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<?> getDriverPerformance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        try {
            // Default to current month if not provided
            if (from == null || to == null) {
                YearMonth now = YearMonth.now();
                from = now.atDay(1);
                to = now.atEndOfMonth();
            }

            log.info("Generating driver performance metrics from {} to {}", from, to);
            List<DriverPerformanceDTO> performance = analyticsService.generateDriverPerformance(from, to);
            return ResponseEntity.ok(performance);

        } catch (Exception e) {
            log.error("Error generating driver performance", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Get trip analytics data for heatmap (top pickup addresses, time charts)
     */
    @GetMapping("/trips")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<?> getTripAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String accountNumber,
            @RequestParam(required = false) Long driverId,
            @RequestParam(required = false) Integer startHour,
            @RequestParam(required = false) Integer endHour) {
        try {
            // Default to current month if not provided
            LocalDate start = startDate;
            LocalDate end = endDate;
            if (start == null || end == null) {
                YearMonth now = YearMonth.now();
                start = now.atDay(1);
                end = now.atEndOfMonth();
            }

            log.info("Getting trip analytics from {} to {}, account={}, driver={}, hours={}-{}",
                start, end, accountNumber, driverId, startHour, endHour);
            Map<String, Object> analytics = analyticsService.getTripAnalytics(
                start, end, accountNumber, driverId, startHour, endHour);
            return ResponseEntity.ok(analytics);

        } catch (Exception e) {
            log.error("Error generating trip analytics", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Get distinct account numbers for filter dropdown
     */
    @GetMapping("/trips/account-numbers")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<?> getDistinctAccountNumbers() {
        try {
            log.info("Fetching distinct account numbers from driver trips");
            List<String> accountNumbers = analyticsService.getDistinctAccountNumbers();
            return ResponseEntity.ok(accountNumbers);

        } catch (Exception e) {
            log.error("Error fetching distinct account numbers", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
