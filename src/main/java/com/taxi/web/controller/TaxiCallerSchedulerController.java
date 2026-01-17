package com.taxi.web.controller;

import com.taxi.domain.shift.dto.DriverShiftImportResult;
import com.taxi.domain.account.dto.TaxiCallerImportResult;
import com.taxi.domain.taxicaller.scheduler.TaxiCallerScheduledImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for managing TaxiCaller scheduled imports
 * Provides endpoints to manually trigger imports and check scheduler status
 */
@RestController
@RequestMapping("/api/taxicaller/scheduler")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TaxiCallerSchedulerController {

    private final TaxiCallerScheduledImportService scheduledImportService;

    /**
     * Manually trigger the scheduled import for testing (both driver shifts and account jobs)
     * POST /api/taxicaller/scheduler/trigger-import?startDate=2025-12-01&endDate=2025-12-01
     * 
     * This allows you to test the import without waiting for the scheduled time
     */
    @PostMapping("/trigger-import")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> triggerManualImport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            TaxiCallerScheduledImportService.ImportResults results = 
                scheduledImportService.manualTrigger(startDate, endDate);
            
            boolean hasData = false;
            
            // Driver shifts results
            Map<String, Object> driverShiftData = new HashMap<>();
            if (results.driverShiftResult != null) {
                hasData = true;
                driverShiftData.put("totalRecords", results.driverShiftResult.getTotalRecords());
                driverShiftData.put("successCount", results.driverShiftResult.getSuccessCount());
                driverShiftData.put("duplicateCount", results.driverShiftResult.getDuplicateCount());
                driverShiftData.put("skippedCount", results.driverShiftResult.getSkippedCount());
                driverShiftData.put("failedCount", results.driverShiftResult.getFailedCount());
                driverShiftData.put("errors", results.driverShiftResult.getErrors());
                driverShiftData.put("skippedReasons", results.driverShiftResult.getSkippedReasons());
            } else {
                driverShiftData.put("message", "No driver shift data found");
            }
            
            // Account jobs results
            Map<String, Object> accountJobData = new HashMap<>();
            if (results.accountJobResult != null) {
                hasData = true;
                accountJobData.put("totalRecords", results.accountJobResult.getTotalRecords());
                accountJobData.put("successCount", results.accountJobResult.getSuccessCount());
                accountJobData.put("duplicateCount", results.accountJobResult.getDuplicateCount());
                accountJobData.put("errorCount", results.accountJobResult.getErrorCount());
                accountJobData.put("errors", results.accountJobResult.getErrors());
                accountJobData.put("duplicateJobIds", results.accountJobResult.getDuplicateJobIds());
            } else {
                accountJobData.put("message", "No account job data found");
            }
            
            if (!hasData) {
                response.put("success", false);
                response.put("message", "No data found for the specified date range");
                return ResponseEntity.ok(response);
            }
            
            response.put("success", true);
            response.put("message", "Manual import completed successfully");
            response.put("startDate", startDate.toString());
            response.put("endDate", endDate.toString());
            response.put("driverShifts", driverShiftData);
            response.put("accountJobs", accountJobData);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error during manual import");
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Trigger import for yesterday's data (both driver shifts and account jobs)
     * POST /api/taxicaller/scheduler/trigger-yesterday
     * 
     * Convenience endpoint to import yesterday's data
     */
    @PostMapping("/trigger-yesterday")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> triggerYesterdayImport() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        return triggerManualImport(yesterday, yesterday);
    }

    /**
     * Get scheduler status and configuration
     * GET /api/taxicaller/scheduler/status
     */
    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getSchedulerStatus() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Check if scheduler bean exists (means it's enabled)
            boolean isEnabled = scheduledImportService != null;
            
            response.put("success", true);
            response.put("schedulerEnabled", isEnabled);
            response.put("message", isEnabled 
                ? "Scheduler is enabled and running" 
                : "Scheduler is disabled");
            
            if (isEnabled) {
                response.put("info", "Scheduler runs daily at configured time (default: 2:00 AM)");
                response.put("imports", "Driver Shifts & Account Jobs");
                response.put("note", "Check application.properties for taxicaller.scheduler.cron");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("schedulerEnabled", false);
            response.put("message", "Scheduler is disabled or not configured");
            return ResponseEntity.ok(response);
        }
    }
}