package com.taxi.web.controller;

import com.taxi.domain.taxicaller.service.TaxiCallerService;
import com.taxi.domain.tenant.exception.TenantConfigurationException;
import com.taxi.domain.account.service.TaxiCallerAccountChargeImportService;
import com.taxi.domain.account.dto.TaxiCallerImportResult;
import com.taxi.domain.shift.dto.DriverShiftImportResult;
import com.taxi.domain.shift.service.TaxiCallerDriverShiftImportService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/taxicaller")
@CrossOrigin(origins = "*")
public class TaxiCallerController {

    @Autowired
    private TaxiCallerService taxiCallerService;

    @Autowired
    private TaxiCallerAccountChargeImportService taxiCallerAccountChargeImportService;

    @Autowired
    private TaxiCallerDriverShiftImportService taxiCallerDriverShiftImportService;

    /**
     * Test endpoint to verify TaxiCaller integration
     */
    @GetMapping("/test")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> testConnection() {
        Map<String, Object> response = new HashMap<>();
        try {
            String result = taxiCallerService.fetchReportTemplates();
            response.put("success", true);
            response.put("message", "TaxiCaller API connection successful");
            response.put("data", new JSONObject(result).toMap());
            return ResponseEntity.ok(response);
        } catch (TenantConfigurationException e) {
            throw e; // Let GlobalExceptionHandler handle it
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to connect to TaxiCaller API");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

     /**
     * Fetch account job reports for a date range
     * GET /api/taxicaller/reports/account-jobs?startDate=2025-01-01&endDate=2025-01-31
     */
    @GetMapping("/reports/account-jobs")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getAccountJobReports(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        Map<String, Object> response = new HashMap<>();
        try {
            JSONArray rows = taxiCallerService.generateAccountJobReports(startDate, endDate);

            //add importAccountJobReports
          // TaxiCallerImportResult result = taxiCallerAccountChargeImportService.importAccountJobReports(rows);
            
            // if (result == null) {
            //     response.put("success", false);
            //     response.put("message", "Failed to fetch account job reports");
            //     return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            // }

            List<Map<String, Object>> reportData = new ArrayList<>();
            for (int i = 0; i < rows.length(); i++) {
                reportData.add(rows.getJSONObject(i).toMap());
            }

            response.put("success", true);
            response.put("message", "Account job reports retrieved successfully");
            response.put("count", rows.length());
            response.put("startDate", startDate.toString());
            response.put("endDate", endDate.toString());
            response.put("data", reportData);
            
            return ResponseEntity.ok(response);
        } catch (TenantConfigurationException e) {
            throw e;
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error fetching account job reports");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }



    /**
     * Load account job reports for a date range into the database
     * GET /api/taxicaller/reports/load-account-jobs?startDate=2025-01-01&endDate=2025-01-31
     */
    @GetMapping("/reports/load-account-jobs")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> loadAccountJobReports(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        Map<String, Object> response = new HashMap<>();
        try {
            JSONArray rows = taxiCallerService.generateAccountJobReports(startDate, endDate);

            //add importAccountJobReports
           TaxiCallerImportResult result = taxiCallerAccountChargeImportService.importAccountJobReports(rows);
            
            if (result == null) {
                response.put("success", false);
                response.put("message", "Failed to fetch account job reports");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

            response.put("success", true);
            response.put("message", "Account job reports loaded successfully");
            response.put("count", result.getSuccessCount());
            response.put("duplicate", result.getDuplicateCount());
            response.put("total records read", result.getTotalRecords());
            response.put("data", result);
            return ResponseEntity.ok(response);
        } catch (TenantConfigurationException e) {
            throw e;
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error fetching account job reports");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    /**
     * Fetch account job reports for a date range
     * GET /api/taxicaller/reports/account-jobs?startDate=2025-01-01&endDate=2025-01-31
     */
    @GetMapping("/reports/importaccount-jobs")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> iimportAccountJobReports(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        Map<String, Object> response = new HashMap<>();
        try {
            JSONArray rows = taxiCallerService.generateAccountJobReports(startDate, endDate);
            
            if (rows == null) {
                response.put("success", false);
                response.put("message", "Failed to fetch account job reports");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

            List<Map<String, Object>> reportData = new ArrayList<>();
            for (int i = 0; i < rows.length(); i++) {
                reportData.add(rows.getJSONObject(i).toMap());
            }

            response.put("success", true);
            response.put("message", "Account job reports retrieved successfully");
            response.put("count", rows.length());
            response.put("startDate", startDate.toString());
            response.put("endDate", endDate.toString());
            response.put("data", reportData);
            
            return ResponseEntity.ok(response);
        } catch (TenantConfigurationException e) {
            throw e;
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error fetching account job reports");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Fetch driver log on/off reports for a date range
     * GET /api/taxicaller/reports/driver-logons?startDate=2025-02-01&endDate=2025-02-28
     */
    @GetMapping("/reports/driver-logons")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getDriverLogOnOffReports(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        Map<String, Object> response = new HashMap<>();
        try {
            JSONArray rows = taxiCallerService.generateDriverLogOnOffReports(startDate, endDate);
            
            if (rows == null) {
                response.put("success", false);
                response.put("message", "Failed to fetch driver log on/off reports");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

            List<Map<String, Object>> reportData = new ArrayList<>();
            for (int i = 0; i < rows.length(); i++) {
                reportData.add(rows.getJSONObject(i).toMap());
            }

            response.put("success", true);
            response.put("message", "Driver log on/off reports retrieved successfully");
            response.put("count", rows.length());
            response.put("startDate", startDate.toString());
            response.put("endDate", endDate.toString());
            response.put("data", reportData);
            System.out.println(reportData);
            
            return ResponseEntity.ok(response);
        } catch (TenantConfigurationException e) {
            throw e;
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error fetching driver log on/off reports");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Fetch driver job reports for a date range
     * GET /api/taxicaller/reports/driver-jobs?startDate=2025-02-01&endDate=2025-02-28
     */
    @GetMapping("/reports/driver-jobs")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getDriverJobReports(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        Map<String, Object> response = new HashMap<>();
        try {
            JSONArray rows = taxiCallerService.generateDriverJobReports(startDate, endDate);
            
            if (rows == null) {
                response.put("success", false);
                response.put("message", "Failed to fetch driver job reports");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

            List<Map<String, Object>> reportData = new ArrayList<>();
            for (int i = 0; i < rows.length(); i++) {
                reportData.add(rows.getJSONObject(i).toMap());
            }

            response.put("success", true);
            response.put("message", "Driver job reports retrieved successfully");
            response.put("count", rows.length());
            response.put("startDate", startDate.toString());
            response.put("endDate", endDate.toString());
            response.put("data", reportData);
            
            return ResponseEntity.ok(response);
        } catch (TenantConfigurationException e) {
            throw e;
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error fetching driver job reports");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Fetch all users from TaxiCaller
     * GET /api/taxicaller/users
     */
    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getUsers() {
        Map<String, Object> response = new HashMap<>();
        try {
            String result = taxiCallerService.fetchUsers();
            
            if (result == null) {
                response.put("success", false);
                response.put("message", "Failed to fetch users");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

            JSONObject jsonResult = new JSONObject(result);
            response.put("success", true);
            response.put("message", "Users retrieved successfully");
            response.put("data", jsonResult.toMap());
            
            return ResponseEntity.ok(response);
        } catch (TenantConfigurationException e) {
            throw e;
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error fetching users");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Fetch all vehicles from TaxiCaller
     * GET /api/taxicaller/vehicles
     */
    @GetMapping("/vehicles")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getVehicles() {
        Map<String, Object> response = new HashMap<>();
        try {
            String result = taxiCallerService.fetchVehicles();
            
            if (result == null) {
                response.put("success", false);
                response.put("message", "Failed to fetch vehicles");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

            JSONObject jsonResult = new JSONObject(result);
            response.put("success", true);
            response.put("message", "Vehicles retrieved successfully");
            response.put("data", jsonResult.toMap());
            
            return ResponseEntity.ok(response);
        } catch (TenantConfigurationException e) {
            throw e;
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error fetching vehicles");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Fetch report templates from TaxiCaller
     * GET /api/taxicaller/reports/templates
     */
    @GetMapping("/reports/templates")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getReportTemplates() {
        Map<String, Object> response = new HashMap<>();
        try {
            String result = taxiCallerService.fetchReportTemplates();
            
            if (result == null) {
                response.put("success", false);
                response.put("message", "Failed to fetch report templates");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

            JSONObject jsonResult = new JSONObject(result);
            response.put("success", true);
            response.put("message", "Report templates retrieved successfully");
            response.put("data", jsonResult.toMap());
            
            return ResponseEntity.ok(response);
        } catch (TenantConfigurationException e) {
            throw e;
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error fetching report templates");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get summary statistics from TaxiCaller
     * GET /api/taxicaller/summary?startDate=2025-01-01&endDate=2025-01-31
     */
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        Map<String, Object> response = new HashMap<>();
        try {
            // Fetch multiple reports
            JSONArray accountJobs = taxiCallerService.generateAccountJobReports(startDate, endDate);
            JSONArray driverLogons = taxiCallerService.generateDriverLogOnOffReports(startDate, endDate);
            
            Map<String, Object> summary = new HashMap<>();
            summary.put("totalAccountJobs", accountJobs != null ? accountJobs.length() : 0);
            summary.put("totalDriverLogons", driverLogons != null ? driverLogons.length() : 0);
            summary.put("dateRange", startDate + " to " + endDate);
            
            response.put("success", true);
            response.put("message", "Summary retrieved successfully");
            response.put("data", summary);
            
            return ResponseEntity.ok(response);
        } catch (TenantConfigurationException e) {
            throw e;
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error fetching summary");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Import driver shifts from TaxiCaller log on/off report
     * GET /api/taxicaller/reports/import-driver-shifts?startDate=2025-12-01&endDate=2025-12-31
     * 
     * This endpoint:
     * 1. Fetches driver log on/off data from TaxiCaller API
     * 2. Imports the data into driver_shifts table
     * 3. Automatically calculates shift hours and day/night split
     * 4. Avoids duplicates
     */
    @GetMapping("/reports/import-driver-shifts")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> importDriverShifts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        Map<String, Object> response = new HashMap<>();
        try {
            // Step 1: Fetch driver log on/off data from TaxiCaller
           // log.info("Fetching driver log on/off data from TaxiCaller: {} to {}", startDate, endDate);
            JSONArray rows = taxiCallerService.generateDriverLogOnOffReports(startDate, endDate);
            
            if (rows == null || rows.length() == 0) {
                response.put("success", false);
                response.put("message", "No driver shift data found for the specified date range");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
           // log.info("Retrieved {} driver shift records from TaxiCaller", rows.length());
            
            // Step 2: Import into driver_shifts table
            DriverShiftImportResult result = taxiCallerDriverShiftImportService.importDriverShifts(rows);
            
            // Step 3: Build response
            response.put("success", true);
            response.put("message", "Driver shifts import completed");
            response.put("startDate", startDate.toString());
            response.put("endDate", endDate.toString());
            response.put("totalRecords", result.getTotalRecords());
            response.put("successCount", result.getSuccessCount());
            response.put("duplicateCount", result.getDuplicateCount());
            response.put("skippedCount", result.getSkippedCount());
            response.put("failedCount", result.getFailedCount());
            
            // Add detailed statistics
            Map<String, Object> details = new HashMap<>();
            details.put("errors", result.getErrors());
            details.put("skippedReasons", result.getSkippedReasons());
            response.put("details", details);
            
            // Determine HTTP status based on results
            if (result.getSuccessCount() == 0 && result.getFailedCount() > 0) {
                response.put("success", false);
                response.put("message", "All records failed to import");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            } else if (result.getFailedCount() > 0) {
                response.put("message", "Driver shifts import completed with some errors");
                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).body(response);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (TenantConfigurationException e) {
            throw e;
        } catch (Exception e) {
           // log.error("Error importing driver shifts from TaxiCaller", e);
            response.put("success", false);
            response.put("message", "Error importing driver shifts");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Preview driver shifts data from TaxiCaller without importing
     * GET /api/taxicaller/reports/preview-driver-shifts?startDate=2025-12-01&endDate=2025-12-31
     * 
     * This endpoint fetches and displays the data without saving to database
     */
    @GetMapping("/reports/preview-driver-shifts")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> previewDriverShifts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        Map<String, Object> response = new HashMap<>();
        try {
            JSONArray rows = taxiCallerService.generateDriverLogOnOffReports(startDate, endDate);
            
            if (rows == null || rows.length() == 0) {
                response.put("success", false);
                response.put("message", "No driver shift data found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            List<Map<String, Object>> reportData = new ArrayList<>();
            for (int i = 0; i < rows.length(); i++) {
                reportData.add(rows.getJSONObject(i).toMap());
            }

            response.put("success", true);
            response.put("message", "Driver shift data preview (not imported)");
            response.put("count", rows.length());
            response.put("startDate", startDate.toString());
            response.put("endDate", endDate.toString());
            response.put("data", reportData);
            
            return ResponseEntity.ok(response);
        } catch (TenantConfigurationException e) {
            throw e;
        } catch (Exception e) {
          //  log.error("Error previewing driver shifts", e);
            response.put("success", false);
            response.put("message", "Error previewing driver shifts");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}