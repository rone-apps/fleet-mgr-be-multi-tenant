package com.taxi.domain.csvuploader;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.taxi.domain.airport.model.AirportTrip;
import com.taxi.domain.airport.repository.AirportTripRepository;
import com.taxi.domain.cab.model.Cab;
import com.taxi.domain.cab.repository.CabRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AirportTripUploadService {

    private final AirportTripRepository airportTripRepository;
    private final CabRepository cabRepository;

    private static final Pattern CAB_NUMBER_PATTERN = Pattern.compile(
        "(?:MACLURES?\\s*)(\\d+[A-Za-z]?)", Pattern.CASE_INSENSITIVE);

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
            log.info("Parsing airport trips CSV: {} rows", dataRows.size());

            Map<String, Integer> columnMappings = detectColumnMappings(headers);

            // PRE-LOAD all cabs for fast lookup
            Map<String, Cab> cabCache = new HashMap<>();
            cabRepository.findAll().forEach(cab -> {
                cabCache.put(cab.getCabNumber().toUpperCase(), cab);
                String normalized = cab.getCabNumber().replaceAll("[^0-9]", "");
                if (!normalized.isEmpty()) {
                    cabCache.put(normalized, cab);
                }
            });
            
            log.info("Loaded {} cabs into cache", cabCache.size());

            // Parse ALL rows (one per cab per date, with all 24 hours)
            List<AirportTripUploadDTO> previewData = new ArrayList<>();
            int validCount = 0, invalidCount = 0, cabMatchCount = 0;

            for (int i = 0; i < dataRows.size(); i++) {
                try {
                    // Parse the raw row
                    AirportTripUploadDTO dto = parseRow(dataRows.get(i), columnMappings, headers, i + 2);
                    enrichWithCabInfoCached(dto, cabCache);

                    // Set shift to "BOTH" - indicates full 24-hour data
                    dto.setShift("BOTH");

                    validateRow(dto);
                    previewData.add(dto);

                    if (dto.isValid()) validCount++;
                    else invalidCount++;
                    if (dto.isCabLookupSuccess()) cabMatchCount++;

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
            preview.setAirportTripPreviewData(previewData);

            Map<String, Object> stats = new HashMap<>();
            stats.put("validRows", validCount);
            stats.put("invalidRows", invalidCount);
            stats.put("cabMatches", cabMatchCount);
            stats.put("uploadType", "AIRPORT_TRIPS");
            stats.put("totalRows", dataRows.size());
            preview.setStatistics(stats);

            return preview;

        } catch (CsvException e) {
            throw new IllegalArgumentException("Failed to parse CSV: " + e.getMessage());
        }
    }


    @Transactional
    public Map<String, Object> importRecords(
            List<AirportTripUploadDTO> records,
            String uploadBatchId,
            String filename,
            String username,
            boolean overwriteExisting) {

        int successCount = 0, skipCount = 0, updateCount = 0, errorCount = 0;
        List<String> errors = new ArrayList<>();
        LocalDateTime uploadDate = LocalDateTime.now();

        for (int i = 0; i < records.size(); i++) {
            AirportTripUploadDTO dto = records.get(i);
            int rowNumber = dto.getRowNumber() > 0 ? dto.getRowNumber() : i + 2;

            try {
                validateRow(dto);
                if (!dto.isValid()) {
                    errors.add("Row " + rowNumber + ": " + dto.getValidationMessage());
                    errorCount++;
                    continue;
                }

                // Check for existing record by cab + date (shift is always "BOTH")
                Optional<AirportTrip> existing = airportTripRepository
                    .findByCabNumberAndTripDate(dto.getCabNumber(), dto.getTripDate());

                if (existing.isPresent()) {
                    if (overwriteExisting) {
                        AirportTrip record = existing.get();
                        updateEntityFromDTO(record, dto);
                        record.setUploadBatchId(uploadBatchId);
                        record.setUploadFilename(filename);
                        record.setUploadDate(uploadDate);
                        record.setUploadedBy(username);
                        airportTripRepository.save(record);
                        updateCount++;
                    } else {
                        skipCount++;
                    }
                    continue;
                }

                AirportTrip record = convertToEntity(dto);
                record.setUploadBatchId(uploadBatchId);
                record.setUploadFilename(filename);
                record.setUploadDate(uploadDate);
                record.setUploadedBy(username);
                airportTripRepository.save(record);
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
        result.put("updateCount", updateCount);
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

            if (header.equals("vehicle")) mappings.put("vehicle", i);
            else if (header.equals("year")) mappings.put("year", i);
            else if (header.equals("month")) mappings.put("month", i);
            else if (header.equals("day")) mappings.put("day", i);
            else if (header.contains("grand total") || header.equals("total")) mappings.put("grandTotal", i);
            else {
                try {
                    int hour = Integer.parseInt(header.trim());
                    if (hour >= 0 && hour <= 23) mappings.put("hour_" + hour, i);
                } catch (NumberFormatException ignored) {}
            }
        }

        return mappings;
    }

    private AirportTripUploadDTO parseRow(String[] row, Map<String, Integer> mappings, String[] headers, int rowNumber) {
        AirportTripUploadDTO dto = new AirportTripUploadDTO();
        dto.setRowNumber(rowNumber);

        String vehicleName = getValue(row, mappings.getOrDefault("vehicle", 0));
        dto.setVehicleName(extractCabNumber(vehicleName));
        dto.setCabNumber(extractCabNumber(vehicleName));

        dto.setYear(parseInteger(getValue(row, mappings.getOrDefault("year", 1))));
        dto.setMonth(parseInteger(getValue(row, mappings.getOrDefault("month", 2))));
        dto.setDay(parseInteger(getValue(row, mappings.getOrDefault("day", 3))));

        if (dto.getYear() != null && dto.getMonth() != null && dto.getDay() != null) {
            try {
                dto.setTripDate(LocalDate.of(dto.getYear(), dto.getMonth(), dto.getDay()));
            } catch (Exception e) {
                log.warn("Invalid date at row {}", rowNumber);
            }
        }

        // Parse hourly counts
        Map<Integer, Integer> hourlyTrips = new HashMap<>();
        for (int hour = 0; hour <= 23; hour++) {
            Integer hourIndex = mappings.get("hour_" + hour);
            if (hourIndex != null) {
                int trips = parseInteger(getValue(row, hourIndex));
                if (trips > 0) hourlyTrips.put(hour, trips);
            } else {
                for (int i = 0; i < headers.length; i++) {
                    try {
                        if (Integer.parseInt(headers[i].trim()) == hour) {
                            int trips = parseInteger(getValue(row, i));
                            if (trips > 0) hourlyTrips.put(hour, trips);
                            break;
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        dto.setHourlyTrips(hourlyTrips);

        Integer grandTotal = parseInteger(getValue(row, mappings.getOrDefault("grandTotal", -1)));
        if (grandTotal != null && grandTotal > 0) {
            dto.setGrandTotal(grandTotal);
        } else {
            dto.calculateGrandTotal();
        }

        return dto;
    }

    private String extractCabNumber(String vehicleName) {
        if (vehicleName == null || vehicleName.trim().isEmpty()) return null;

        Matcher matcher = CAB_NUMBER_PATTERN.matcher(vehicleName);
        if (matcher.find()) {
            return matcher.group(1).toUpperCase();
        }

        // Remove MACLURES prefix if present, then extract digits only
        String cleaned = vehicleName.toUpperCase()
            .replaceAll("MACLURES?\\s*", "")
            .replaceAll("[^0-9]", "")
            .trim();
        
        return cleaned.isEmpty() ? null : cleaned;
    }

    private void enrichWithCabInfoCached(AirportTripUploadDTO dto, Map<String, Cab> cabCache) {
        if (dto.getCabNumber() == null || dto.getCabNumber().isEmpty()) {
            dto.setCabLookupSuccess(false);
            dto.setCabLookupMessage("No cab number extracted");
            return;
        }

        String cabKey = dto.getCabNumber().toUpperCase();
        Cab cab = cabCache.get(cabKey);
        
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
    }

    private void validateRow(AirportTripUploadDTO dto) {
        StringBuilder errors = new StringBuilder();

        if (dto.getCabNumber() == null || dto.getCabNumber().isEmpty()) errors.append("Cab number required. ");
        if (dto.getTripDate() == null) errors.append("Valid date required. ");
        if (dto.getGrandTotal() == null || dto.getGrandTotal() == 0) errors.append("No trips recorded. ");

        dto.setValid(errors.length() == 0);
        dto.setValidationMessage(errors.length() > 0 ? errors.toString().trim() : null);
    }

    private AirportTrip convertToEntity(AirportTripUploadDTO dto) {
        AirportTrip entity = new AirportTrip();
        entity.setCabNumber(dto.getCabNumber());
        entity.setShift(dto.getShift());
        entity.setVehicleName(dto.getVehicleName());
        entity.setTripDate(dto.getTripDate());
        for (int hour = 0; hour <= 23; hour++) {
            entity.setTripsByHour(hour, dto.getTripsForHour(hour));
        }
        entity.setGrandTotal(dto.getGrandTotal());
        return entity;
    }

    private void updateEntityFromDTO(AirportTrip entity, AirportTripUploadDTO dto) {
        entity.setShift(dto.getShift());
        entity.setVehicleName(dto.getVehicleName());
        for (int hour = 0; hour <= 23; hour++) {
            entity.setTripsByHour(hour, dto.getTripsForHour(hour));
        }
        entity.setGrandTotal(dto.getGrandTotal());
    }

    private String getValue(String[] row, int index) {
        if (index >= 0 && index < row.length) {
            String value = row[index];
            return (value != null && !value.trim().isEmpty()) ? value.trim() : null;
        }
        return null;
    }

    private Integer parseInteger(String value) {
        if (value == null || value.trim().isEmpty()) return 0;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
