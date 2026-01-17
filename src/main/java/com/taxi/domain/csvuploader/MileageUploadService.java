package com.taxi.domain.csvuploader;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.taxi.domain.cab.model.Cab;
import com.taxi.domain.cab.repository.CabRepository;
import com.taxi.domain.driver.model.Driver;
import com.taxi.domain.driver.repository.DriverRepository;
import com.taxi.domain.mileage.model.MileageRecord;
import com.taxi.domain.mileage.repository.MileageRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MileageUploadService {

    private final MileageRecordRepository mileageRecordRepository;
    private final CabRepository cabRepository;
    private final DriverRepository driverRepository;

    // Date/time formats from CSV: "11/06/2025   04:09:54PM"
    private static final List<DateTimeFormatter> DATETIME_FORMATTERS = Arrays.asList(
        DateTimeFormatter.ofPattern("MM/dd/yyyy   hh:mm:ssa"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy   hh:mm:ss a"),
        DateTimeFormatter.ofPattern("M/d/yyyy   h:mm:ssa"),
        DateTimeFormatter.ofPattern("M/d/yyyy   h:mm:ss a"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ssa"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss a"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    );

    @Transactional(readOnly = true)
    public CsvUploadPreviewDTO parseAndPreview(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();

        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            List<String[]> allRows = reader.readAll();

            if (allRows.isEmpty()) {
                throw new IllegalArgumentException("CSV file is empty");
            }

            String[] headers = allRows.get(0);
            if (headers.length > 0 && headers[0].startsWith("\uFEFF")) {
                headers[0] = headers[0].substring(1);
            }

            List<String[]> dataRows = allRows.subList(1, allRows.size());
            log.info("Parsing mileage CSV: {} rows", dataRows.size());

            Map<String, Integer> columnMappings = detectColumnMappings(headers);

            // PRE-LOAD all cabs and drivers for fast lookup (instead of querying per row)
            Map<String, Cab> cabCache = new HashMap<>();
            cabRepository.findAll().forEach(cab -> {
                cabCache.put(cab.getCabNumber().toUpperCase(), cab);
                // Also add normalized version (digits only)
                String normalized = cab.getCabNumber().replaceAll("[^0-9]", "");
                if (!normalized.isEmpty()) {
                    cabCache.put(normalized, cab);
                }
            });
            
            Map<String, Driver> driverCache = new HashMap<>();
            driverRepository.findAll().forEach(driver -> {
                if (driver.getDriverNumber() != null) {
                    driverCache.put(driver.getDriverNumber(), driver);
                }
            });
            
            log.info("Loaded {} cabs and {} drivers into cache", cabCache.size(), driverCache.size());

            // Parse ALL rows with cached lookups
            List<MileageUploadDTO> previewData = new ArrayList<>();
            int validCount = 0, invalidCount = 0, cabMatchCount = 0, driverMatchCount = 0;

            for (int i = 0; i < dataRows.size(); i++) {
                try {
                    MileageUploadDTO dto = parseRow(dataRows.get(i), columnMappings, i + 2);
                    enrichWithCabAndDriverInfoCached(dto, cabCache, driverCache);
                    validateRow(dto);
                    previewData.add(dto);

                    if (dto.isValid()) validCount++;
                    else invalidCount++;
                    if (dto.isCabLookupSuccess()) cabMatchCount++;
                    if (dto.isDriverLookupSuccess()) driverMatchCount++;
                    
                    // Log progress every 500 rows
                    if (i > 0 && i % 500 == 0) {
                        log.info("Processed {} of {} rows", i, dataRows.size());
                    }
                } catch (Exception e) {
                    log.error("Error parsing row {}: {}", i + 2, e.getMessage());
                    invalidCount++;
                }
            }

            CsvUploadPreviewDTO preview = new CsvUploadPreviewDTO();
            preview.setFilename(filename);
            preview.setTotalRows(dataRows.size());
            preview.setHeaders(Arrays.asList(headers));
            preview.setColumnMappings(columnMappings);
            preview.setMileagePreviewData(previewData);

            Map<String, Object> stats = new HashMap<>();
            stats.put("validRows", validCount);
            stats.put("invalidRows", invalidCount);
            stats.put("cabMatches", cabMatchCount);
            stats.put("driverMatches", driverMatchCount);
            stats.put("uploadType", "MILEAGE");
            preview.setStatistics(stats);

            return preview;

        } catch (CsvException e) {
            throw new IllegalArgumentException("Failed to parse CSV: " + e.getMessage());
        }
    }

    @Transactional
    public Map<String, Object> importRecords(
            List<MileageUploadDTO> records,
            String uploadBatchId,
            String filename,
            String username) {

        int successCount = 0, skipCount = 0, errorCount = 0;
        List<String> errors = new ArrayList<>();
        LocalDateTime uploadDate = LocalDateTime.now();

        for (int i = 0; i < records.size(); i++) {
            MileageUploadDTO dto = records.get(i);
            int rowNumber = dto.getRowNumber() > 0 ? dto.getRowNumber() : i + 2;

            try {
                validateRow(dto);
                if (!dto.isValid()) {
                    errors.add("Row " + rowNumber + ": " + dto.getValidationMessage());
                    errorCount++;
                    continue;
                }

                // Check for duplicates
                if (isDuplicate(dto)) {
                    skipCount++;
                    continue;
                }

                MileageRecord record = convertToEntity(dto);
                record.setUploadBatchId(uploadBatchId);
                record.setUploadFilename(filename);
                record.setUploadDate(uploadDate);
                record.setUploadedBy(username);
                mileageRecordRepository.save(record);
                successCount++;

            } catch (Exception e) {
                log.error("Error importing row {}: {}", rowNumber, e.getMessage());
                errors.add("Row " + rowNumber + ": " + e.getMessage());
                errorCount++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("uploadBatchId", uploadBatchId);
        result.put("successCount", successCount);
        result.put("skipCount", skipCount);
        result.put("errorCount", errorCount);
        result.put("totalProcessed", records.size());
        result.put("errors", errors);

        return result;
    }

    private Map<String, Integer> detectColumnMappings(String[] headers) {
        Map<String, Integer> mappings = new HashMap<>();

        for (int i = 0; i < headers.length; i++) {
            String header = headers[i].trim().toLowerCase();

            if (header.equals("cab") || header.equals("cab_number") || header.equals("vehicle")) {
                mappings.put("cabNumber", i);
            } else if (header.equals("a") || header.equals("mileage_a")) {
                mappings.put("mileageA", i);
            } else if (header.equals("b") || header.equals("mileage_b")) {
                mappings.put("mileageB", i);
            } else if (header.equals("c") || header.equals("mileage_c")) {
                mappings.put("mileageC", i);
            } else if (header.contains("driver") && (header.contains("number") || header.contains("num") || header.contains("id"))) {
                mappings.put("driverNumber", i);
            } else if (header.contains("logon") || header.contains("log_on") || header.contains("start")) {
                mappings.put("logonTime", i);
            } else if (header.contains("logoff") || header.contains("log_off") || header.contains("end")) {
                mappings.put("logoffTime", i);
            }
        }

        return mappings;
    }

    private MileageUploadDTO parseRow(String[] row, Map<String, Integer> mappings, int rowNumber) {
        MileageUploadDTO dto = new MileageUploadDTO();
        dto.setRowNumber(rowNumber);

        dto.setCabNumber(normalizeCabNumber(getValue(row, mappings.getOrDefault("cabNumber", 0))));
        dto.setMileageA(parseDecimal(getValue(row, mappings.getOrDefault("mileageA", 1))));
        dto.setMileageB(parseDecimal(getValue(row, mappings.getOrDefault("mileageB", 2))));
        dto.setMileageC(parseDecimal(getValue(row, mappings.getOrDefault("mileageC", 3))));
        dto.setDriverNumber(getValue(row, mappings.getOrDefault("driverNumber", 4)));

        String logonStr = getValue(row, mappings.getOrDefault("logonTime", 5));
        String logoffStr = getValue(row, mappings.getOrDefault("logoffTime", 6));
        dto.setRawLogonTime(logonStr);
        dto.setRawLogoffTime(logoffStr);
        dto.setLogonTime(parseDateTime(logonStr));
        dto.setLogoffTime(parseDateTime(logoffStr));

        // Calculate totals
        BigDecimal a = dto.getMileageA() != null ? dto.getMileageA() : BigDecimal.ZERO;
        BigDecimal b = dto.getMileageB() != null ? dto.getMileageB() : BigDecimal.ZERO;
        BigDecimal c = dto.getMileageC() != null ? dto.getMileageC() : BigDecimal.ZERO;
        dto.setTotalMileage(a.add(b).add(c));

        if (dto.getLogonTime() != null && dto.getLogoffTime() != null) {
            long minutes = Duration.between(dto.getLogonTime(), dto.getLogoffTime()).toMinutes();
            dto.setShiftHours(BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP));
        }

        return dto;
    }

    /**
     * Extract just the numeric part from cab number.
     * "1b" -> "1", "1B" -> "1", "103" -> "103", "12A" -> "12"
     */
    private String normalizeCabNumber(String cabNumber) {
        if (cabNumber == null) return null;
        // Extract only digits from the cab number
        String digitsOnly = cabNumber.trim().replaceAll("[^0-9]", "");
        return digitsOnly.isEmpty() ? cabNumber.trim().toUpperCase() : digitsOnly;
    }

    /**
     * Fast cached lookup for cab and driver info
     */
    private void enrichWithCabAndDriverInfoCached(MileageUploadDTO dto, Map<String, Cab> cabCache, Map<String, Driver> driverCache) {
        // Cab lookup using cache
        if (dto.getCabNumber() != null && !dto.getCabNumber().isEmpty()) {
            String cabKey = dto.getCabNumber().toUpperCase();
            Cab cab = cabCache.get(cabKey);
            
            // Try normalized (digits only) if not found
            if (cab == null) {
                String normalized = dto.getCabNumber().replaceAll("[^0-9]", "");
                cab = cabCache.get(normalized);
            }
            
            if (cab != null) {
                dto.setCabNumber(cab.getCabNumber());
                dto.setCabLookupSuccess(true);
                dto.setCabLookupMessage("Cab found");
            } else {
                dto.setCabLookupSuccess(false);
                dto.setCabLookupMessage("Cab not found: " + dto.getCabNumber());
            }
        } else {
            dto.setCabLookupSuccess(false);
            dto.setCabLookupMessage("No cab number");
        }

        // Driver lookup using cache
        if (dto.getDriverNumber() != null && !dto.getDriverNumber().isEmpty()) {
            Driver driver = driverCache.get(dto.getDriverNumber());
            if (driver != null) {
                dto.setDriverLookupSuccess(true);
                dto.setDriverLookupMessage("Driver found: " + driver.getFirstName() + " " + driver.getLastName());
            } else {
                dto.setDriverLookupSuccess(false);
                dto.setDriverLookupMessage("Driver not found: " + dto.getDriverNumber());
            }
        } else {
            dto.setDriverLookupSuccess(false);
            dto.setDriverLookupMessage("No driver number");
        }
    }

    // Keep old method for backward compatibility but mark as deprecated
    @Deprecated
    private void enrichWithCabAndDriverInfo(MileageUploadDTO dto) {
        // Cab lookup
        if (dto.getCabNumber() != null && !dto.getCabNumber().isEmpty()) {
            Optional<Cab> cab = cabRepository.findByCabNumber(dto.getCabNumber());
            if (cab.isPresent()) {
                dto.setCabLookupSuccess(true);
                dto.setCabLookupMessage("Cab found");
            } else {
                dto.setCabLookupSuccess(false);
                dto.setCabLookupMessage("Cab not found: " + dto.getCabNumber());
            }
        } else {
            dto.setCabLookupSuccess(false);
            dto.setCabLookupMessage("No cab number");
        }

        // Driver lookup
        if (dto.getDriverNumber() != null && !dto.getDriverNumber().isEmpty()) {
            Optional<Driver> driver = driverRepository.findByDriverNumber(dto.getDriverNumber());
            if (driver.isPresent()) {
                dto.setDriverLookupSuccess(true);
                dto.setDriverLookupMessage("Driver found: " + driver.get().getFirstName() + " " + driver.get().getLastName());
            } else {
                dto.setDriverLookupSuccess(false);
                dto.setDriverLookupMessage("Driver not found: " + dto.getDriverNumber());
            }
        } else {
            dto.setDriverLookupSuccess(false);
            dto.setDriverLookupMessage("No driver number");
        }
    }

    private void validateRow(MileageUploadDTO dto) {
        StringBuilder errors = new StringBuilder();

        if (dto.getCabNumber() == null || dto.getCabNumber().isEmpty()) {
            errors.append("Cab number required. ");
        }
        if (dto.getLogonTime() == null) {
            errors.append("Logon time required. ");
        }
        
        boolean hasMileage = (dto.getMileageA() != null && dto.getMileageA().compareTo(BigDecimal.ZERO) > 0) ||
                            (dto.getMileageB() != null && dto.getMileageB().compareTo(BigDecimal.ZERO) > 0) ||
                            (dto.getMileageC() != null && dto.getMileageC().compareTo(BigDecimal.ZERO) > 0);
        if (!hasMileage) {
            errors.append("At least one mileage value required. ");
        }

        dto.setValid(errors.length() == 0);
        dto.setValidationMessage(errors.length() > 0 ? errors.toString().trim() : null);
    }

    private boolean isDuplicate(MileageUploadDTO dto) {
        return mileageRecordRepository.existsByCabNumberAndDriverNumberAndLogonTime(
            dto.getCabNumber(), dto.getDriverNumber(), dto.getLogonTime());
    }

    private MileageRecord convertToEntity(MileageUploadDTO dto) {
        MileageRecord entity = new MileageRecord();
        entity.setCabNumber(dto.getCabNumber());
        entity.setDriverNumber(dto.getDriverNumber());
        entity.setLogonTime(dto.getLogonTime());
        entity.setLogoffTime(dto.getLogoffTime());
        entity.setMileageA(dto.getMileageA());
        entity.setMileageB(dto.getMileageB());
        entity.setMileageC(dto.getMileageC());
        entity.setTotalMileage(dto.getTotalMileage());
        entity.setShiftHours(dto.getShiftHours());
        return entity;
    }

    private String getValue(String[] row, int index) {
        if (index >= 0 && index < row.length) {
            String value = row[index];
            return (value != null && !value.trim().isEmpty()) ? value.trim() : null;
        }
        return null;
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) return null;

        // Normalize: collapse multiple spaces to single space
        String cleaned = dateTimeStr.trim().replaceAll("\\s+", " ");
        
        // Try with single space formatters
        List<DateTimeFormatter> singleSpaceFormatters = Arrays.asList(
            DateTimeFormatter.ofPattern("M/d/yyyy h:mm:ssa"),
            DateTimeFormatter.ofPattern("M/d/yyyy h:mm:ss a"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ssa"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss a"),
            DateTimeFormatter.ofPattern("M/d/yyyy hh:mm:ssa"),
            DateTimeFormatter.ofPattern("M/d/yyyy hh:mm:ss a"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        );

        for (DateTimeFormatter formatter : singleSpaceFormatters) {
            try {
                return LocalDateTime.parse(cleaned, formatter);
            } catch (DateTimeParseException ignored) {}
        }

        // Try parsing manually if standard formatters fail
        // Format: "11/26/2025 05:11:22PM" or "11/26/2025 05:11:22 PM"
        try {
            // Remove extra spaces and normalize AM/PM
            String normalized = cleaned.toUpperCase();
            
            // Split date and time
            String[] parts = normalized.split(" ");
            if (parts.length >= 2) {
                String datePart = parts[0]; // "11/26/2025"
                String timePart = parts.length == 2 ? parts[1] : parts[1] + parts[2]; // "05:11:22PM"
                
                // Parse date
                String[] dateParts = datePart.split("/");
                if (dateParts.length == 3) {
                    int month = Integer.parseInt(dateParts[0]);
                    int day = Integer.parseInt(dateParts[1]);
                    int year = Integer.parseInt(dateParts[2]);
                    
                    // Parse time with AM/PM
                    boolean isPM = timePart.contains("PM");
                    boolean isAM = timePart.contains("AM");
                    String timeOnly = timePart.replace("AM", "").replace("PM", "").trim();
                    
                    String[] timeParts = timeOnly.split(":");
                    if (timeParts.length >= 2) {
                        int hour = Integer.parseInt(timeParts[0]);
                        int minute = Integer.parseInt(timeParts[1]);
                        int second = timeParts.length >= 3 ? Integer.parseInt(timeParts[2]) : 0;
                        
                        // Convert to 24-hour format
                        if (isPM && hour != 12) {
                            hour += 12;
                        } else if (isAM && hour == 12) {
                            hour = 0;
                        }
                        
                        return LocalDateTime.of(year, month, day, hour, minute, second);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Manual datetime parsing failed for: {}", dateTimeStr);
        }

        log.warn("Unable to parse datetime: {}", dateTimeStr);
        return null;
    }

    private BigDecimal parseDecimal(String value) {
        if (value == null || value.trim().isEmpty()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(value.replaceAll("[^0-9.-]", ""));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
