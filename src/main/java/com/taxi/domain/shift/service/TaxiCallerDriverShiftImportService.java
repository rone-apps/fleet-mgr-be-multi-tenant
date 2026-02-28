package com.taxi.domain.shift.service;

import com.taxi.domain.shift.model.DriverShift;
import com.taxi.domain.shift.repository.DriverShiftRepository;
import com.taxi.domain.shift.dto.DriverShiftImportResult;
import com.taxi.domain.driver.model.Driver;
import com.taxi.domain.driver.repository.DriverRepository;
import com.taxi.domain.cab.repository.CabRepository;  // ADD THIS
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service to import driver shift data from TaxiCaller API
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TaxiCallerDriverShiftImportService {

    private final DriverShiftRepository driverShiftRepository;
    private final DriverRepository driverRepository;
    private final CabRepository cabRepository;
    private final TransactionTemplate transactionTemplate;
    
    // TaxiCaller date format: "04/12/2025 06:32"
    private static final DateTimeFormatter TAXICALLER_DATE_FORMAT = 
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Import driver shifts from TaxiCaller report data
     * 
     * @param rows JSONArray from TaxiCaller driver log on/off report
     * @return DriverShiftImportResult with import statistics
     */
    public DriverShiftImportResult importDriverShifts(JSONArray rows) {
        log.info("Starting TaxiCaller driver shift import. Total records: {}", rows.length());
        
        DriverShiftImportResult result = new DriverShiftImportResult();
        result.setTotalRecords(rows.length());
        
        List<String> errors = new ArrayList<>();
        Map<String, Integer> skippedReasons = new HashMap<>();
        
        for (int i = 0; i < rows.length(); i++) {
            try {
                JSONObject row = rows.getJSONObject(i);
                
                // Extract fields from TaxiCaller data
                String driverUsername = row.optString("driver_username", "");
                String vehicleCallsign = row.optString("vehicle.callsign", "");
                String trackStart = row.optString("track.start", "");
                String trackEnd = row.optString("track.end", "");
                String hosDuration = row.optString("hos.duration", "");
                String driverFirstName = row.optString("driver.first_name", "");
                String driverLastName = row.optString("driver.last_name", "");
                
                // Debug: Log raw row data for first few rows to see actual field names
                if (i < 5) {
                    log.info("Row {}: RAW DATA = {}", i, row.toString());
                }
                
                // Validate required fields
                if (driverUsername.isEmpty() || vehicleCallsign.isEmpty() || 
                    trackStart.isEmpty() || trackEnd.isEmpty()) {
                    String reason = "Missing required fields";
                    skippedReasons.put(reason, skippedReasons.getOrDefault(reason, 0) + 1);
                    result.incrementSkipped();
                    log.warn("Row {}: Skipping - {}", i, reason);
                    continue;
                }
                
                // Parse dates
                LocalDateTime logonTime;
                LocalDateTime logoffTime;
                try {
                    log.debug("Row {}: Parsing dates - trackStart raw: '{}', trackEnd raw: '{}'", 
                        i, trackStart, trackEnd);
                    logonTime = LocalDateTime.parse(trackStart, TAXICALLER_DATE_FORMAT);
                    logoffTime = LocalDateTime.parse(trackEnd, TAXICALLER_DATE_FORMAT);
                    log.info("Row {}: Date conversion - TaxiCaller trackStart: '{}' → parsed logonTime: '{}', TaxiCaller trackEnd: '{}' → parsed logoffTime: '{}'", 
                        i, trackStart, logonTime, trackEnd, logoffTime);
                    
                    // Validate that logoff is after logon
                    if (logoffTime.isBefore(logonTime)) {
                        log.warn("Row {}: logoffTime ({}) is before logonTime ({}), this may indicate a date parsing issue", 
                            i, logoffTime, logonTime);
                    }
                } catch (Exception e) {
                    String reason = "Invalid date format";
                    skippedReasons.put(reason, skippedReasons.getOrDefault(reason, 0) + 1);
                    result.incrementSkipped();
                    errors.add(String.format("Row %d: Invalid date format - trackStart: '%s', trackEnd: '%s', error: %s", 
                        i, trackStart, trackEnd, e.getMessage()));
                    log.warn("Row {}: Skipping - Invalid date format - trackStart: '{}', trackEnd: '{}'", i, trackStart, trackEnd);
                    continue;
                }

                // **Extract cab number from vehicle callsign**
                // TaxiCaller sends formats like "M89", "M89,CHT FEB 28TH(9:15-13:45)" → We want just the numeric part
                String cabNumber = extractCabNumber(vehicleCallsign);
                if (cabNumber.isEmpty()) {
                    String reason = "Invalid cab callsign (callsign: " + vehicleCallsign + ")";
                    skippedReasons.put("Invalid cab callsign",
                        skippedReasons.getOrDefault("Invalid cab callsign", 0) + 1);
                    result.incrementSkipped();
                    log.warn("Row {}: Skipping - {}", i, reason);
                    continue;
                }

                if (!cabNumber.equals(vehicleCallsign)) {
                    log.debug("Row {}: Extracted numeric cab number from callsign: '{}' → '{}'",
                        i, vehicleCallsign, cabNumber);
                }
                // **Validate cab exists**
                boolean cabExists = cabRepository.existsByCabNumber(cabNumber);
                if (!cabExists) {
                    String reason = "Cab not found (cab: " + cabNumber + ")";
                    skippedReasons.put("Cab not found", 
                        skippedReasons.getOrDefault("Cab not found", 0) + 1);
                    result.incrementSkipped();
                    log.warn("Row {}: Skipping - {}", i, reason);
                    continue;
                }
                
                // **Map driver_username to driver_number**
                // Try multiple methods to find the driver
                String driverNumber = null;
                try {
                    // Method 1: Try driver_number field directly
                    Optional<Driver> driverNumberOpt = driverRepository.findByDriverNumber(driverUsername);
                    
                    if (driverNumberOpt.isPresent()) {
                        driverNumber = driverNumberOpt.get().getDriverNumber();
                        log.debug("Row {}: Mapped driver_username '{}' to driver_number '{}' via driver_number", 
                            i, driverUsername, driverNumber);
                    } else {
                        // Method 2: Try username field
                        Optional<Driver> driverOpt = driverRepository.findByUsername(driverUsername);
                        
                        if (driverOpt.isPresent()) {
                            driverNumber = driverOpt.get().getDriverNumber();
                            log.debug("Row {}: Mapped driver_username '{}' to driver_number '{}' via username", 
                                i, driverUsername, driverNumber);
                        } else {
                            // Method 3: Try matching by first name + last name
                            if (driverFirstName != null && !driverFirstName.isEmpty() && 
                                driverLastName != null && !driverLastName.isEmpty()) {
                                Optional<String> driverOptStr = driverRepository.findDriverNumberByName(
                                    driverFirstName, driverLastName);
                                
                                if (driverOptStr.isPresent()) {
                                    driverNumber = driverOptStr.get();
                                    log.debug("Row {}: Mapped driver '{} {}' to driver_number '{}' via name", 
                                        i, driverFirstName, driverLastName, driverNumber);
                                }
                            }
                        }
                    }
                    
                    if (driverNumber == null) {
                        // Driver not found by any method - skip this shift
                        String reason = "Driver not found (username: " + driverUsername + ", name: " + 
                            driverFirstName + " " + driverLastName + ")";
                        skippedReasons.put("Driver not found", 
                            skippedReasons.getOrDefault("Driver not found", 0) + 1);
                        result.incrementSkipped();
                        log.warn("Row {}: Skipping - {}", i, reason);
                        continue;
                    }
                } catch (Exception e) {
                    log.error("Row {}: Error looking up driver_number for username '{}'", 
                        i, driverUsername, e);
                    String reason = "Error looking up driver";
                    skippedReasons.put(reason, skippedReasons.getOrDefault(reason, 0) + 1);
                    result.incrementSkipped();
                    continue;
                }
                
                // Create new DriverShift entity
                DriverShift shift = new DriverShift();
                shift.setDriverNumber(driverNumber);      // FK enforces driver exists
                shift.setCabNumber(cabNumber);            // FK enforces cab exists (stripped M prefix)
                shift.setDriverUsername(driverUsername);
                shift.setDriverFirstName(driverFirstName);
                shift.setDriverLastName(driverLastName);
                shift.setLogonTime(logonTime);
                shift.setLogoffTime(logoffTime);
                
                // Parse total hours from duration string (e.g., "08:30" -> 8.5)
                BigDecimal totalHours = parseDurationToHours(hosDuration);
                shift.setTotalHours(totalHours);
                
                // Calculate shift types based on logon time
                // DAY = logon between 00:00-11:59, NIGHT = logon between 12:00-23:59
                int logonHour = logonTime.getHour();
                String primaryType = (logonHour < 12) ? "DAY" : "NIGHT";
                String secondaryType = (logonHour < 12) ? "NIGHT" : "DAY";
                
                shift.setPrimaryShiftType(primaryType);
                shift.setSecondaryShiftType(secondaryType);
                
                // Calculate shift counts based on total hours
                // Up to 12 hours = 1 primary, 12-15 = +0.25, 15-18 = +0.5, 18+ = +1 secondary
                shift.setPrimaryShiftCount(BigDecimal.ONE);
                
                BigDecimal secondaryCount = BigDecimal.ZERO;
                if (totalHours != null) {
                    double hours = totalHours.doubleValue();
                    if (hours > 18) {
                        secondaryCount = BigDecimal.ONE;
                    } else if (hours > 15) {
                        secondaryCount = new BigDecimal("0.50");
                    } else if (hours > 12) {
                        secondaryCount = new BigDecimal("0.25");
                    }
                }
                shift.setSecondaryShiftCount(secondaryCount);
                
                shift.setStatus("COMPLETED");
                
                // Check for duplicate before saving
                boolean isDuplicate = driverShiftRepository.existsByDriverNumberAndCabNumberAndLogonTimeAndLogoffTime(
                    driverNumber, cabNumber, logonTime, logoffTime);
                
                if (isDuplicate) {
                    result.incrementDuplicate();
                    log.debug("Row {}: Skipping duplicate shift for driver {} in cab {} ({} - {})", 
                        i, driverNumber, cabNumber, logonTime, logoffTime);
                    continue;
                }
                
                // Save to database in isolated transaction
                final DriverShift shiftToSave = shift;
                final int rowNum = i;
                final String username = driverUsername;
                final String cab = cabNumber;
                try {
                    transactionTemplate.executeWithoutResult(status -> {
                        driverShiftRepository.save(shiftToSave);
                    });
                    result.incrementSuccess();
                    log.debug("Row {}: Successfully imported shift for driver {} in cab {}", 
                        rowNum, username, cab);
                } catch (Exception saveEx) {
                    result.incrementFailed();
                    String errorMsg = String.format("Row %d: Error - %s", rowNum, saveEx.getMessage());
                    errors.add(errorMsg);
                    log.error("Row {}: Failed to save - {}", rowNum, saveEx.getMessage());
                }
                
            } catch (Exception e) {
                result.incrementFailed();
                String errorMsg = String.format("Row %d: Error - %s", i, e.getMessage());
                errors.add(errorMsg);
                log.error("Row {}: Failed to import - {}", i, e.getMessage(), e);
            }
        }
        
        result.setErrors(errors);
        result.setSkippedReasons(skippedReasons);
        
        log.info("TaxiCaller driver shift import completed. Success: {}, Duplicates: {}, Skipped: {}, Failed: {}", 
            result.getSuccessCount(), result.getDuplicateCount(), 
            result.getSkippedCount(), result.getFailedCount());
        
        return result;
    }
    
    /**
     * Get import statistics for display
     */
    public Map<String, Object> getImportStats(DriverShiftImportResult result) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRecords", result.getTotalRecords());
        stats.put("successCount", result.getSuccessCount());
        stats.put("duplicateCount", result.getDuplicateCount());
        stats.put("skippedCount", result.getSkippedCount());
        stats.put("failedCount", result.getFailedCount());
        stats.put("errors", result.getErrors());
        stats.put("skippedReasons", result.getSkippedReasons());
        return stats;
    }
    
    /**
     * Parse duration string (e.g., "08:30" or "8:30") to decimal hours (e.g., 8.5)
     */
    private BigDecimal parseDurationToHours(String duration) {
        if (duration == null || duration.isEmpty()) {
            return null;
        }
        try {
            String[] parts = duration.split(":");
            if (parts.length >= 2) {
                int hours = Integer.parseInt(parts[0].trim());
                int minutes = Integer.parseInt(parts[1].trim());
                double totalHours = hours + (minutes / 60.0);
                return BigDecimal.valueOf(totalHours).setScale(2, java.math.RoundingMode.HALF_UP);
            }
            // If no colon, try parsing as decimal hours
            return new BigDecimal(duration.trim()).setScale(2, java.math.RoundingMode.HALF_UP);
        } catch (Exception e) {
            log.warn("Failed to parse duration '{}': {}", duration, e.getMessage());
            return null;
        }
    }

    /**
     * Extract cab number from vehicle callsign using regex pattern.
     * Handles patterns like:
     * - "M89" → "89"
     * - "M89,CHT FEB 28TH(9:15-13:45)" → "89"
     * - "89" → "89"
     *
     * @param callsign the vehicle callsign string
     * @return the extracted numeric cab number, or empty string if no match
     */
    private String extractCabNumber(String callsign) {
        if (callsign == null || callsign.isEmpty()) {
            return "";
        }

        // Pattern: one or more letters followed by one or more digits
        // Captures only the digits part
        Pattern pattern = Pattern.compile("[A-Za-z]+(\\d+)");
        Matcher matcher = pattern.matcher(callsign);

        if (matcher.find()) {
            return matcher.group(1); // Return captured digits
        }

        // Fallback: if no letters, try to extract just the leading digits
        pattern = Pattern.compile("^(\\d+)");
        matcher = pattern.matcher(callsign);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return ""; // No cab number found
    }
}