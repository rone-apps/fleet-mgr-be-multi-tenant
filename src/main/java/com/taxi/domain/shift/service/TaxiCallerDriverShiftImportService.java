package com.taxi.domain.shift.service;

import com.taxi.domain.shift.model.DriverShift;
import com.taxi.domain.shift.repository.DriverShiftRepository;
import com.taxi.domain.shift.dto.DriverShiftImportResult;
import com.taxi.domain.driver.model.Driver;
import com.taxi.domain.driver.repository.DriverRepository;
import com.taxi.domain.cab.repository.CabRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
    
    // TaxiCaller date formats (support multiple precisions)
    // Primary: with seconds (dd/MM/yyyy HH:mm:ss) - preferred for accuracy
    // Fallback: with minutes only (dd/MM/yyyy HH:mm) - legacy format
    private static final DateTimeFormatter TAXICALLER_DATE_FORMAT_WITH_SECONDS =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter TAXICALLER_DATE_FORMAT_MINUTES_ONLY =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Optional: Millisecond precision if TaxiCaller provides it: "dd/MM/yyyy HH:mm:ss.SSS"
    private static final DateTimeFormatter TAXICALLER_DATE_FORMAT_WITH_MILLIS =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss.SSS");

    /**
     * Import driver shifts from TaxiCaller report data
     * ✅ CONSOLIDATION: Multiple logons/logoffs on the same day are merged into a single shift
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

        // ✅ STEP 1: Parse all raw shifts from TaxiCaller data
        Map<String, DriverShift> shiftsToProcess = new TreeMap<>();  // Key: driver|cab|date|shiftType

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
                
                // Parse dates with precision preservation
                // ✅ Try to capture seconds (or milliseconds) for accuracy
                LocalDateTime logonTime;
                LocalDateTime logoffTime;
                try {
                    log.debug("Row {}: Parsing dates - trackStart raw: '{}', trackEnd raw: '{}'",
                        i, trackStart, trackEnd);

                    // ✅ Try multiple date formats in order of precision (highest first)
                    logonTime = parseDateTimeWithPrecision(trackStart);
                    logoffTime = parseDateTimeWithPrecision(trackEnd);

                    log.info("Row {}: Date conversion - TaxiCaller trackStart: '{}' → parsed logonTime: '{}' (nanoseconds: {}), TaxiCaller trackEnd: '{}' → parsed logoffTime: '{}' (nanoseconds: {})",
                        i, trackStart, logonTime, logonTime.getNano(), trackEnd, logoffTime, logoffTime.getNano());

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
                        // ✅ AUTO-CREATE: Driver not found - create a new driver record with defaults
                        try {
                            driverNumber = createNewDriver(driverUsername, driverFirstName, driverLastName);
                            log.info("Row {}: Created new driver record - username: '{}', name: '{} {}', driver_number: '{}'",
                                i, driverUsername, driverFirstName, driverLastName, driverNumber);
                        } catch (Exception createEx) {
                            // Failed to create driver - skip this shift
                            String reason = "Failed to create driver (username: " + driverUsername + "): " + createEx.getMessage();
                            skippedReasons.put("Failed to create driver",
                                skippedReasons.getOrDefault("Failed to create driver", 0) + 1);
                            result.incrementSkipped();
                            log.error("Row {}: Could not create new driver - {}", i, reason);
                            continue;
                        }
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

                // ✅ Shift type will be determined in consolidation logic based on FIRST logon time
                shift.setStatus("COMPLETED");
                
                // ✅ CONSOLIDATION: Determine shift window based on FIRST logon time
                // DAY Shift Window: 1:00 AM - 12:59 PM (1 AM start)
                // NIGHT Shift Window: 1:00 PM - 12:59 AM (1 PM start, spans to next calendar day)
                int logonHour = logonTime.getHour();
                String shiftType = determineShiftType(logonHour);
                LocalDate shiftRecordDate = determineShiftRecordDate(logonTime, shiftType);

                String consolidationKey = driverNumber + "|" + cabNumber + "|" + shiftRecordDate + "|" + shiftType;

                if (shiftsToProcess.containsKey(consolidationKey)) {
                    // ✅ MERGE: Extend existing shift with this new logon/logoff pair
                    DriverShift existing = shiftsToProcess.get(consolidationKey);

                    // Take the earliest logon time
                    if (logonTime.isBefore(existing.getLogonTime())) {
                        existing.setLogonTime(logonTime);
                    }

                    // Take the latest logoff time
                    if (logoffTime.isAfter(existing.getLogoffTime())) {
                        existing.setLogoffTime(logoffTime);
                    }

                    // Check if total hours exceed 12 hour window - if so, this should be a new shift
                    BigDecimal currentTotal = (existing.getTotalHours() != null ? existing.getTotalHours() : BigDecimal.ZERO)
                            .add(totalHours != null ? totalHours : BigDecimal.ZERO);

                    if (currentTotal.doubleValue() > 12.0) {
                        log.warn("Row {}: Total hours ({}) exceed 12-hour shift window for {} {} on {}. This may need manual review.",
                            i, currentTotal, driverNumber, cabNumber, shiftRecordDate);
                    }

                    existing.setTotalHours(currentTotal);

                    // Recalculate shift counts based on new total hours
                    existing.setPrimaryShiftCount(BigDecimal.ONE);
                    double hours = currentTotal.doubleValue();
                    if (hours > 18) {
                        existing.setSecondaryShiftCount(BigDecimal.ONE);
                    } else if (hours > 15) {
                        existing.setSecondaryShiftCount(new BigDecimal("0.50"));
                    } else if (hours > 12) {
                        existing.setSecondaryShiftCount(new BigDecimal("0.25"));
                    } else {
                        existing.setSecondaryShiftCount(BigDecimal.ZERO);
                    }

                    // ✅ AUDIT TRAIL: Append session to notes field in JSON format
                    appendSessionToHistory(existing, logonTime, logoffTime, totalHours);

                    log.debug("Row {}: Consolidated {} shift for driver {} cab {} (record date: {}) - now spans {} to {}, total hours: {}",
                            i, shiftType, driverNumber, cabNumber, shiftRecordDate,
                            existing.getLogonTime(), existing.getLogoffTime(), existing.getTotalHours());
                } else {
                    // First occurrence of this shift - store it
                    shift.setPrimaryShiftType(shiftType);
                    shift.setSecondaryShiftType("DAY".equals(shiftType) ? "NIGHT" : "DAY");

                    // ✅ AUDIT TRAIL: Initialize session history in notes field
                    initializeSessionHistory(shift, logonTime, logoffTime, totalHours);

                    shiftsToProcess.put(consolidationKey, shift);
                    log.debug("Row {}: New {} shift for driver {} cab {} (record date: {}) - first logon at {}:00",
                            i, shiftType, driverNumber, cabNumber, shiftRecordDate, logonHour);
                }
                
            } catch (Exception e) {
                result.incrementFailed();
                String errorMsg = String.format("Row %d: Error - %s", i, e.getMessage());
                errors.add(errorMsg);
                log.error("Row {}: Failed to import - {}", i, e.getMessage(), e);
            }
        }

        // ✅ STEP 2: Save all consolidated shifts to database
        log.info("Processing {} consolidated shifts", shiftsToProcess.size());

        for (Map.Entry<String, DriverShift> entry : shiftsToProcess.entrySet()) {
            DriverShift shiftToSave = entry.getValue();
            String key = entry.getKey();

            try {
                // Check for existing duplicate before saving
                boolean isDuplicate = driverShiftRepository.existsByDriverNumberAndCabNumberAndLogonTimeAndLogoffTime(
                    shiftToSave.getDriverNumber(), shiftToSave.getCabNumber(),
                    shiftToSave.getLogonTime(), shiftToSave.getLogoffTime());

                if (isDuplicate) {
                    result.incrementDuplicate();
                    log.debug("Skipping duplicate shift: {}", key);
                    continue;
                }

                // Save in isolated transaction
                transactionTemplate.executeWithoutResult(status -> {
                    driverShiftRepository.save(shiftToSave);
                });
                result.incrementSuccess();
                log.debug("Saved consolidated shift: {} (logon: {}, logoff: {}, hours: {})",
                        key, shiftToSave.getLogonTime(), shiftToSave.getLogoffTime(), shiftToSave.getTotalHours());

            } catch (Exception saveEx) {
                result.incrementFailed();
                String errorMsg = String.format("Failed to save shift %s: %s", key, saveEx.getMessage());
                errors.add(errorMsg);
                log.error("Error saving consolidated shift {}: {}", key, saveEx.getMessage());
            }
        }

        result.setErrors(errors);
        result.setSkippedReasons(skippedReasons);

        log.info("TaxiCaller driver shift import completed. Input rows: {}, Consolidated shifts: {}, Success: {}, Duplicates: {}, Skipped: {}, Failed: {}",
            rows.length(), shiftsToProcess.size(), result.getSuccessCount(), result.getDuplicateCount(),
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
     * ✅ AUTO-CREATE: Create a new driver record with default values when importing from TaxiCaller
     * Used when a driver doesn't exist in the system
     */
    private String createNewDriver(String username, String firstName, String lastName) throws Exception {
        try {
            // Generate unique driver_number: TC-{timestamp} format for auto-created drivers
            long timestamp = System.currentTimeMillis();
            String generatedDriverNumber = "TC-" + timestamp;

            // Ensure first/last names have defaults
            String safeFirstName = firstName != null && !firstName.trim().isEmpty() ? firstName.trim() : "Unknown";
            String safeLastName = lastName != null && !lastName.trim().isEmpty() ? lastName.trim() : "Unknown";
            String safeUsername = username != null && !username.trim().isEmpty() ? username.trim() : "tc_" + timestamp;

            // Create new driver with defaults
            Driver newDriver = Driver.builder()
                    .driverNumber(username)          // TC-{timestamp}
                    .firstName(safeFirstName)                      // From TaxiCaller
                    .lastName(safeLastName)                        // From TaxiCaller
                    .username(safeUsername)                        // From TaxiCaller or generated
                    .status(Driver.DriverStatus.ACTIVE)           // Default: ACTIVE
                    .isOwner(false)                               // Default: driver only (not owner)
                    .isAdmin(false)                               // Default: not admin
                    .joinedDate(LocalDate.now())                 // Today's date
                    .notes("Auto-created from TaxiCaller import on " + LocalDateTime.now() +
                           ". Original username: " + username)    // Audit trail
                    .build();

            // Save to database in transaction
            Driver savedDriver = driverRepository.save(newDriver);
            log.info("✅ Created new driver: {} ({} {}) with driver_number: {}",
                    savedDriver.getUsername(), savedDriver.getFirstName(), savedDriver.getLastName(),
                    savedDriver.getDriverNumber());

            return savedDriver.getDriverNumber();

        } catch (Exception e) {
            log.error("❌ Failed to create new driver for username '{}': {}", username, e.getMessage(), e);
            throw new RuntimeException("Unable to create new driver record: " + e.getMessage(), e);
        }
    }

    /**
     * ✅ SHIFT WINDOWS: Determine shift type based on logon hour
     * DAY Shift: 1:00 AM (01:00) - 12:59 PM (12:59)
     * NIGHT Shift: 1:00 PM (13:00) - 12:59 AM (00:59)
     */
    private String determineShiftType(int logonHour) {
        // logonHour 1-12 = 1 AM to 12:59 PM = DAY shift
        // logonHour 13-23 or 0 = 1 PM to 12:59 AM = NIGHT shift
        if (logonHour >= 1 && logonHour <= 12) {
            return "DAY";
        } else {
            return "NIGHT";  // Hours 0, 13-23
        }
    }

    /**
     * ✅ SHIFT RECORD DATE: Determine which date to record the shift under
     * NIGHT shifts that span midnight are recorded under the date they START
     * If logon is 12:00 AM - 12:59 AM, it's part of previous day's NIGHT shift
     */
    private LocalDate determineShiftRecordDate(LocalDateTime logonTime, String shiftType) {
        int logonHour = logonTime.getHour();
        LocalDate logonDate = logonTime.toLocalDate();

        if ("NIGHT".equals(shiftType) && logonHour == 0) {
            // Midnight hour (12:00 AM - 12:59 AM) belongs to previous day's NIGHT shift
            return logonDate.minusDays(1);
        } else {
            // DAY shifts and normal NIGHT shifts (1 PM+) are recorded under their start date
            return logonDate;
        }
    }

    /**
     * ✅ PRECISION: Parse datetime with support for multiple precision levels
     * Tries formats in order: milliseconds → seconds → minutes (fallback)
     * Preserves the highest available precision
     */
    private LocalDateTime parseDateTimeWithPrecision(String dateString) throws Exception {
        Exception e1 = null, e2 = null, e3 = null;

        // Try millisecond precision first (highest precision)
        try {
            return LocalDateTime.parse(dateString, TAXICALLER_DATE_FORMAT_WITH_MILLIS);
        } catch (Exception ex) {
            e1 = ex;
            log.debug("Millisecond format failed for '{}', trying seconds format", dateString);
        }

        // Try second precision (common format)
        try {
            return LocalDateTime.parse(dateString, TAXICALLER_DATE_FORMAT_WITH_SECONDS);
        } catch (Exception ex) {
            e2 = ex;
            log.debug("Second format failed for '{}', falling back to minute format", dateString);
        }

        // Fallback to minute precision (legacy format)
        try {
            return LocalDateTime.parse(dateString, TAXICALLER_DATE_FORMAT_MINUTES_ONLY);
        } catch (Exception ex) {
            e3 = ex;
            log.error("All date formats failed for '{}': milliseconds={}, seconds={}, minutes={}",
                dateString,
                e1 != null ? e1.getMessage() : "N/A",
                e2 != null ? e2.getMessage() : "N/A",
                e3 != null ? e3.getMessage() : "N/A");
            throw new IllegalArgumentException("Unable to parse date: " + dateString);
        }
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
     * ✅ AUDIT TRAIL: Initialize session history in JSON format
     * Stores individual logon/logoff pairs for record keeping
     */
    private void initializeSessionHistory(DriverShift shift, LocalDateTime logonTime, LocalDateTime logoffTime, BigDecimal hours) {
        try {
            JSONArray sessions = new JSONArray();
            JSONObject session = new JSONObject();
            session.put("logon", logonTime.toString());
            session.put("logoff", logoffTime.toString());
            session.put("hours", hours != null ? hours.doubleValue() : 0);
            sessions.put(session);

            // Store as JSON in notes field
            shift.setNotes("SESSIONS: " + sessions.toString());
            log.debug("Initialized session history: {}", sessions.toString());
        } catch (Exception e) {
            log.warn("Failed to initialize session history: {}", e.getMessage());
            shift.setNotes("SESSIONS: [session tracking failed]");
        }
    }

    /**
     * ✅ AUDIT TRAIL: Append session to existing history in JSON format
     */
    private void appendSessionToHistory(DriverShift shift, LocalDateTime logonTime, LocalDateTime logoffTime, BigDecimal hours) {
        try {
            String existingNotes = shift.getNotes() != null ? shift.getNotes() : "";

            JSONArray sessions;
            if (existingNotes.startsWith("SESSIONS: ")) {
                // Parse existing sessions
                String jsonStr = existingNotes.substring(10); // Remove "SESSIONS: " prefix
                sessions = new JSONArray(jsonStr);
            } else {
                sessions = new JSONArray();
            }

            // Add new session
            JSONObject newSession = new JSONObject();
            newSession.put("logon", logonTime.toString());
            newSession.put("logoff", logoffTime.toString());
            newSession.put("hours", hours != null ? hours.doubleValue() : 0);
            sessions.put(newSession);

            // Update notes with appended session
            shift.setNotes("SESSIONS: " + sessions.toString());
            log.debug("Appended session to history. Total sessions: {}", sessions.length());
        } catch (Exception e) {
            log.warn("Failed to append session history: {}", e.getMessage());
            // Gracefully fall back - don't fail the entire import
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