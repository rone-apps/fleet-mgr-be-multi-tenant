package com.taxi.domain.taxicaller.scheduler;

import com.taxi.domain.shift.dto.DriverShiftImportResult;
import com.taxi.domain.shift.service.TaxiCallerDriverShiftImportService;
import com.taxi.domain.account.dto.TaxiCallerImportResult;
import com.taxi.domain.account.service.TaxiCallerAccountChargeImportService;
import com.taxi.domain.drivertrip.dto.DriverTripImportResult;
import com.taxi.domain.drivertrip.service.TaxiCallerDriverTripImportService;
import com.taxi.domain.taxicaller.service.TaxiCallerService;
import com.taxi.infrastructure.multitenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Scheduled service to automatically import data from TaxiCaller
 * 
 * Imports:
 * 1. Driver shift data (log on/off times)
 * 2. Account job data (account charges)
 * 
 * Runs daily at a configured time (default: 2:00 AM) to import previous day's data
 * 
 * Configuration:
 * - taxicaller.scheduler.enabled=true/false (enable/disable the scheduler)
 * - taxicaller.scheduler.import-previous-days=1 (how many days back to import, default: 1)
 * 
 * Schedule: Runs at 2:00 AM daily (can be configured via cron expression)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    name = "taxicaller.scheduler.enabled", 
    havingValue = "true", 
    matchIfMissing = false
)
public class TaxiCallerScheduledImportService {

    private final TaxiCallerService taxiCallerService;
    private final TaxiCallerDriverShiftImportService driverShiftImportService;
    private final TaxiCallerAccountChargeImportService accountChargeImportService;
    private final TaxiCallerDriverTripImportService driverTripImportService;
    
    @Value("${taxicaller.scheduler.import-previous-days:2}")
    private int importPreviousDays;

    /**
     * Scheduled task to import all data from TaxiCaller
     * 
     * Schedule: Daily at 2:00 AM
     * 
     * You can customize the schedule using cron expressions:
     * - "0 0 2 * * *"        = 2:00 AM every day (default)
     * - "0 0 3 * * *"        = 3:00 AM every day
     * - "0 0 1 * * MON-FRI"  = 1:00 AM Monday-Friday only
     * - "0 30 23 * * *"      = 11:30 PM every day
     * 
     * Cron format: second minute hour day month weekday
     */
    @Scheduled(cron = "${taxicaller.scheduler.cron:0 0 11 * * *}")
    public void scheduledTaxiCallerImport() {
        log.info("═══════════════════════════════════════════════════════════");
        log.info("SCHEDULED TASK: TaxiCaller Data Import Started");
        log.info("   Timestamp: {}", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        log.info("═══════════════════════════════════════════════════════════");

        try {
            // Set tenant context for scheduled (non-HTTP) execution
            TenantContext.setSystemTenant();

            // Calculate date range (previous day by default)
            LocalDate endDate = LocalDate.now().minusDays(1);
            LocalDate startDate = endDate.minusDays(importPreviousDays - 1);

            log.info("Import Parameters:");
            log.info("   Start Date: {}", startDate);
            log.info("   End Date: {}", endDate);
            log.info("   Days to Import: {}", importPreviousDays);

            // Import all types of data
            importDriverShifts(startDate, endDate);
            importAccountJobs(startDate, endDate);
            importDriverJobs(startDate, endDate);

            log.info("SCHEDULED IMPORT COMPLETED SUCCESSFULLY");

        } catch (Exception e) {
            log.error("SCHEDULED IMPORT FAILED: Error during scheduled TaxiCaller import", e);
            log.error("   Error Message: {}", e.getMessage());
            log.error("   Error Type: {}", e.getClass().getName());
        } finally {
            TenantContext.clear();
            log.info("═══════════════════════════════════════════════════════════");
            log.info("SCHEDULED TASK: TaxiCaller Data Import Ended");
            log.info("   Timestamp: {}", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            log.info("═══════════════════════════════════════════════════════════");
        }
    }
    
    /**
     * Import driver shifts from TaxiCaller
     */
    private void importDriverShifts(LocalDate startDate, LocalDate endDate) {
        log.info("");
        log.info("🚗 ═══════════════════════════════════════════════════════════");
        log.info("   PART 1: Driver Shifts Import");
        log.info("   ═══════════════════════════════════════════════════════════");
        
        try {
            // Step 1: Fetch data from TaxiCaller
            log.info("📡 Step 1/3: Fetching driver log on/off data from TaxiCaller API...");
            JSONArray rows = taxiCallerService.generateDriverLogOnOffReports(startDate, endDate);
            
            if (rows == null || rows.length() == 0) {
                log.warn("⚠️  No driver shift data found for {} to {}", startDate, endDate);
                log.info("✅ Driver shifts import completed - No data to import");
                return;
            }
            
            log.info("✅ Retrieved {} driver shift records from TaxiCaller", rows.length());
            
            // Step 2: Import into database
            log.info("💾 Step 2/3: Importing driver shifts into database...");
            DriverShiftImportResult result = driverShiftImportService.importDriverShifts(rows);
            
            // Step 3: Log results
            log.info("📊 Step 3/3: Driver Shifts Import Summary:");
            log.info("   Total Records: {}", result.getTotalRecords());
            log.info("   ✅ Successfully Imported: {}", result.getSuccessCount());
            log.info("   🔄 Duplicates Skipped: {}", result.getDuplicateCount());
            log.info("   ⊘ Skipped (validation): {}", result.getSkippedCount());
            log.info("   ❌ Failed: {}", result.getFailedCount());
            
            // Log skipped reasons if any
            if (result.getSkippedCount() > 0 && result.getSkippedReasons() != null) {
                log.info("   Skipped Reasons Breakdown:");
                result.getSkippedReasons().forEach((reason, count) -> 
                    log.info("      - {}: {}", reason, count)
                );
            }
            
            // Log errors if any
            if (result.getFailedCount() > 0 && result.getErrors() != null && !result.getErrors().isEmpty()) {
                log.warn("   ⚠️  Errors encountered during import:");
                int errorCount = Math.min(result.getErrors().size(), 10); // Show max 10 errors
                for (int i = 0; i < errorCount; i++) {
                    log.warn("      - {}", result.getErrors().get(i));
                }
                if (result.getErrors().size() > 10) {
                    log.warn("      ... and {} more errors", result.getErrors().size() - 10);
                }
            }
            
            // Determine success level
            if (result.getSuccessCount() == 0 && result.getFailedCount() > 0) {
                log.error("❌ Driver Shifts Import FAILED: All records failed to import");
            } else if (result.getFailedCount() > 0) {
                log.warn("⚠️  Driver Shifts Import COMPLETED WITH WARNINGS: Some records failed");
            } else {
                log.info("✅ Driver Shifts Import COMPLETED SUCCESSFULLY");
            }
            
        } catch (Exception e) {
            log.error("❌ Driver Shifts Import FAILED: Error during import", e);
            log.error("   Error Message: {}", e.getMessage());
        }
    }
    
    /**
     * Import account jobs from TaxiCaller
     */
    private void importAccountJobs(LocalDate startDate, LocalDate endDate) {
        log.info("");
        log.info("💼 ═══════════════════════════════════════════════════════════");
        log.info("   PART 2: Account Jobs Import");
        log.info("   ═══════════════════════════════════════════════════════════");
        
        try {
            // Step 1: Fetch data from TaxiCaller
            log.info("📡 Step 1/3: Fetching account job data from TaxiCaller API...");
            JSONArray rows = taxiCallerService.generateAccountJobReports(startDate, endDate);
            
            if (rows == null || rows.length() == 0) {
                log.warn("⚠️  No account job data found for {} to {}", startDate, endDate);
                log.info("✅ Account jobs import completed - No data to import");
                return;
            }
            
            log.info("✅ Retrieved {} account job records from TaxiCaller", rows.length());
            
            // Step 2: Import into database
            log.info("💾 Step 2/3: Importing account jobs into database...");
            TaxiCallerImportResult result = accountChargeImportService.importAccountJobReports(rows);
            
            if (result == null) {
                log.error("❌ Account Jobs Import FAILED: Import service returned null");
                return;
            }
            
            // Step 3: Log results
            log.info("📊 Step 3/3: Account Jobs Import Summary:");
            log.info("   Total Records: {}", result.getTotalRecords());
            log.info("   ✅ Successfully Imported: {}", result.getSuccessCount());
            log.info("   🔄 Duplicates Skipped: {}", result.getDuplicateCount());
            log.info("   ❌ Errors: {}", result.getErrorCount());
            
            // Log errors if any
            if (result.getErrorCount() > 0 && result.getErrors() != null && !result.getErrors().isEmpty()) {
                log.warn("   ⚠️  Errors encountered during import:");
                int errorCount = Math.min(result.getErrors().size(), 10); // Show max 10 errors
                for (int i = 0; i < errorCount; i++) {
                    log.warn("      - {}", result.getErrors().get(i));
                }
                if (result.getErrors().size() > 10) {
                    log.warn("      ... and {} more errors", result.getErrors().size() - 10);
                }
            }
            
            // Determine success level
            if (result.getSuccessCount() == 0 && result.getErrorCount() > 0) {
                log.error("❌ Account Jobs Import FAILED: All records failed to import");
            } else if (result.getErrorCount() > 0) {
                log.warn("⚠️  Account Jobs Import COMPLETED WITH WARNINGS: Some records failed");
            } else {
                log.info("✅ Account Jobs Import COMPLETED SUCCESSFULLY");
            }
            
        } catch (Exception e) {
            log.error("❌ Account Jobs Import FAILED: Error during import", e);
            log.error("   Error Message: {}", e.getMessage());
        }
    }
    
    /**
     * Import driver jobs from TaxiCaller
     */
    private void importDriverJobs(LocalDate startDate, LocalDate endDate) {
        log.info("");
        log.info("🚕 ═══════════════════════════════════════════════════════════");
        log.info("   PART 3: Driver Jobs Import");
        log.info("   ═══════════════════════════════════════════════════════════");

        try {
            log.info("📡 Step 1/3: Fetching driver job data from TaxiCaller API...");
            JSONArray rows = taxiCallerService.generateDriverJobReports(startDate, endDate);

            if (rows == null || rows.length() == 0) {
                log.warn("⚠️  No driver job data found for {} to {}", startDate, endDate);
                log.info("✅ Driver jobs import completed - No data to import");
                return;
            }

            log.info("✅ Retrieved {} driver job records from TaxiCaller", rows.length());

            log.info("💾 Step 2/3: Importing driver jobs into database...");
            DriverTripImportResult result = driverTripImportService.importDriverJobReports(rows);

            log.info("📊 Step 3/3: Driver Jobs Import Summary:");
            log.info("   Total Records: {}", result.getTotalRecords());
            log.info("   ✅ Successfully Imported: {}", result.getSuccessCount());
            log.info("   🔄 Duplicates Skipped: {}", result.getDuplicateCount());
            log.info("   ❌ Errors: {}", result.getErrorCount());

            if (result.getErrorCount() > 0 && result.getErrors() != null && !result.getErrors().isEmpty()) {
                log.warn("   ⚠️  Errors encountered during import:");
                int errorCount = Math.min(result.getErrors().size(), 10);
                for (int i = 0; i < errorCount; i++) {
                    log.warn("      - {}", result.getErrors().get(i));
                }
                if (result.getErrors().size() > 10) {
                    log.warn("      ... and {} more errors", result.getErrors().size() - 10);
                }
            }

            if (result.getSuccessCount() == 0 && result.getErrorCount() > 0) {
                log.error("❌ Driver Jobs Import FAILED: All records failed to import");
            } else if (result.getErrorCount() > 0) {
                log.warn("⚠️  Driver Jobs Import COMPLETED WITH WARNINGS: Some records failed");
            } else {
                log.info("✅ Driver Jobs Import COMPLETED SUCCESSFULLY");
            }

        } catch (Exception e) {
            log.error("❌ Driver Jobs Import FAILED: Error during import", e);
            log.error("   Error Message: {}", e.getMessage());
        }
    }

    /**
     * Manual trigger for testing the scheduled import (both types)
     * This method can be called via a test endpoint to verify the scheduler works
     */
    public ImportResults manualTrigger(LocalDate startDate, LocalDate endDate) {
        log.info("MANUAL TRIGGER: TaxiCaller Data Import");
        log.info("   Start Date: {}", startDate);
        log.info("   End Date: {}", endDate);

        ImportResults results = new ImportResults();

        // Manual trigger may be called from controller (tenant already set) or directly
        boolean tenantWasSet = false;
        try {
            TenantContext.getCurrentTenant();
            tenantWasSet = true;
        } catch (IllegalStateException e) {
            TenantContext.setSystemTenant();
        }

        try {
            // Import driver shifts
            JSONArray shiftRows = taxiCallerService.generateDriverLogOnOffReports(startDate, endDate);
            if (shiftRows != null && shiftRows.length() > 0) {
                results.driverShiftResult = driverShiftImportService.importDriverShifts(shiftRows);
                log.info("Driver shifts: {} success, {} failed",
                    results.driverShiftResult.getSuccessCount(),
                    results.driverShiftResult.getFailedCount());
            } else {
                log.warn("No driver shift data found");
            }

            // Import account jobs
            JSONArray accountRows = taxiCallerService.generateAccountJobReports(startDate, endDate);
            if (accountRows != null && accountRows.length() > 0) {
                results.accountJobResult = accountChargeImportService.importAccountJobReports(accountRows);
                log.info("Account jobs: {} success, {} errors",
                    results.accountJobResult.getSuccessCount(),
                    results.accountJobResult.getErrorCount());
            } else {
                log.warn("No account job data found");
            }

            // Import driver jobs
            JSONArray driverJobRows = taxiCallerService.generateDriverJobReports(startDate, endDate);
            if (driverJobRows != null && driverJobRows.length() > 0) {
                results.driverJobResult = driverTripImportService.importDriverJobReports(driverJobRows);
                log.info("Driver jobs: {} success, {} errors",
                    results.driverJobResult.getSuccessCount(),
                    results.driverJobResult.getErrorCount());
            } else {
                log.warn("No driver job data found");
            }

            return results;

        } catch (Exception e) {
            log.error("Manual import failed", e);
            throw e;
        } finally {
            if (!tenantWasSet) {
                TenantContext.clear();
            }
        }
    }
    
    /**
     * Container for both import results
     */
    public static class ImportResults {
        public DriverShiftImportResult driverShiftResult;
        public TaxiCallerImportResult accountJobResult;
        public DriverTripImportResult driverJobResult;
    }
}