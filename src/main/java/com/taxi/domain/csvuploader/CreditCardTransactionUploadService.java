package com.taxi.domain.csvuploader;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.taxi.domain.driver.model.Driver;
import com.taxi.domain.driver.repository.DriverRepository;
import com.taxi.domain.financial.Merchant2Cab;
import com.taxi.domain.financial.Merchant2CabRepository;
import com.taxi.domain.payment.model.CreditCardTransaction;
import com.taxi.domain.payment.repository.CreditCardTransactionRepository;
import com.taxi.domain.shift.model.DriverShift;
import com.taxi.domain.shift.repository.DriverShiftRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditCardTransactionUploadService {
    
    private final CreditCardTransactionRepository transactionRepository;
    private final DriverRepository driverRepository;
    private final Merchant2CabRepository merchant2CabRepository;
    private final DriverShiftRepository driverShiftRepository;
    
    // UPDATED: Added formats for the actual CSV
    private static final List<DateTimeFormatter> DATE_FORMATTERS = Arrays.asList(
        DateTimeFormatter.ofPattern("yyyy/MM/dd"),  // NEW: Primary format from CSV
        DateTimeFormatter.ofPattern("yyyyMMdd"),    // NEW: Settlement date format (20251223)
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("M/d/yyyy"),
        DateTimeFormatter.ofPattern("dd-MMM-yyyy")
    );
    
    private static final List<DateTimeFormatter> TIME_FORMATTERS = Arrays.asList(
        DateTimeFormatter.ofPattern("HH:mm:ss"),
        DateTimeFormatter.ofPattern("HH:mm"),
        DateTimeFormatter.ofPattern("h:mm:ss a"),
        DateTimeFormatter.ofPattern("h:mm a"),
        DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
    );
    
    @Transactional(readOnly = true)
    public CsvUploadPreviewDTO parseAndPreview(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        
        // UPDATED: Use UTF-8 with BOM handling
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            List<String[]> allRows = reader.readAll();
            
            if (allRows.isEmpty()) {
                throw new IllegalArgumentException("CSV file is empty");
            }
            
            String[] headers = allRows.get(0);
            
            // UPDATED: Remove BOM from first header if present
            if (headers.length > 0 && headers[0].startsWith("\uFEFF")) {
                headers[0] = headers[0].substring(1);
            }
            
            List<String[]> dataRows = allRows.subList(1, allRows.size());
            
            log.info("Parsing CSV with {} rows. Headers: {}", dataRows.size(), Arrays.toString(headers));
            
            // Auto-detect column mappings
            Map<String, Integer> columnMappings = detectColumnMappings(headers);
            
            log.info("Detected column mappings: {}", columnMappings);
            
            // Parse ALL rows, not just preview
            List<CreditCardTransactionUploadDTO> previewData = new ArrayList<>();
            
            int successCount = 0;
            int errorCount = 0;
            
            for (int i = 0; i < dataRows.size(); i++) {
                try {
                    String[] row = dataRows.get(i);
                    CreditCardTransactionUploadDTO dto = parseRow(row, columnMappings, filename);
                    
                    // Perform cab and driver lookup
                    enrichWithCabAndDriver(dto);
                    
                    // Validate
                    validateRow(dto);
                    
                    previewData.add(dto);
                    
                    if (dto.isValid()) {
                        successCount++;
                    } else {
                        errorCount++;
                    }
                } catch (Exception e) {
                    log.error("Error parsing row {}: {}", i + 2, e.getMessage());
                    errorCount++;
                }
            }
            
            CsvUploadPreviewDTO preview = new CsvUploadPreviewDTO();
            preview.setFilename(filename);
            preview.setTotalRows(dataRows.size());
            preview.setHeaders(Arrays.asList(headers));
            preview.setColumnMappings(columnMappings);
            preview.setPreviewData(previewData);
            preview.setDetectedMappings(getDetectedMappingsSummary(columnMappings, headers));
            
            // Add statistics
            long validRows = previewData.stream().filter(CreditCardTransactionUploadDTO::isValid).count();
            long cabMatches = previewData.stream().filter(CreditCardTransactionUploadDTO::isCabLookupSuccess).count();
            long driverMatches = previewData.stream().filter(CreditCardTransactionUploadDTO::isDriverLookupSuccess).count();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("validRows", validRows);
            stats.put("invalidRows", dataRows.size() - validRows);
            stats.put("cabMatches", cabMatches);
            stats.put("driverMatches", driverMatches);
            preview.setStatistics(stats);
            
            log.info("Preview complete: {} valid, {} invalid, {} cab matches, {} driver matches", 
                     validRows, dataRows.size() - validRows, cabMatches, driverMatches);
            
            return preview;
            
        } catch (CsvException e) {
            log.error("CSV parsing error: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Failed to parse CSV file: " + e.getMessage());
        }
    }
    
    @Transactional
    public Map<String, Object> importTransactions(
            List<CreditCardTransactionUploadDTO> transactions,
            String uploadBatchId,
            String filename,
            String username) {
        
        int successCount = 0;
        int skipCount = 0;
        int errorCount = 0;
        List<String> errors = new ArrayList<>();
        List<CreditCardTransaction> batchToSave = new ArrayList<>();
        
        LocalDateTime uploadDate = LocalDateTime.now();
        final int BATCH_SIZE = 100;
        
        log.info("Starting import of {} transactions", transactions.size());
        
        // Pre-load existing transaction keys for duplicate checking
        Set<String> existingKeys = preloadExistingTransactionKeys(transactions);
        log.info("Loaded {} existing transaction keys for duplicate checking", existingKeys.size());
        
        // Track keys we're adding in this batch to prevent duplicates within the upload
        Set<String> newKeys = new HashSet<>();
        
        for (int i = 0; i < transactions.size(); i++) {
            CreditCardTransactionUploadDTO dto = transactions.get(i);
            int rowNumber = i + 2; // +2 for header and 0-based index
            
            try {
                // Re-enrich with cab and driver (in case data was edited)
                enrichWithCabAndDriver(dto);
                
                // Validate
                validateRow(dto);
                
                if (!dto.isValid()) {
                    errors.add("Row " + rowNumber + ": " + dto.getValidationMessage());
                    errorCount++;
                    continue;
                }
                
                // Check for duplicates using cached keys
                String key = buildTransactionKey(dto);
                if (existingKeys.contains(key) || newKeys.contains(key)) {
                    log.debug("Skipping duplicate transaction at row {}: Auth={}, Amount={}", 
                             rowNumber, dto.getAuthorizationCode(), dto.getAmount());
                    skipCount++;
                    continue;
                }
                
                // Create entity
                CreditCardTransaction transaction = convertToEntity(dto);
                transaction.setUploadBatchId(uploadBatchId);
                transaction.setUploadFilename(filename);
                transaction.setUploadDate(uploadDate);
                
                batchToSave.add(transaction);
                newKeys.add(key);
                successCount++;
                
                // Batch save every BATCH_SIZE records
                if (batchToSave.size() >= BATCH_SIZE) {
                    transactionRepository.saveAll(batchToSave);
                    transactionRepository.flush();
                    log.info("Saved batch of {} records, progress: {}/{}", BATCH_SIZE, i + 1, transactions.size());
                    batchToSave.clear();
                }
                
            } catch (Exception e) {
                log.error("Error importing transaction at row {}: {}", rowNumber, e.getMessage(), e);
                errors.add("Row " + rowNumber + ": " + e.getMessage());
                errorCount++;
            }
        }
        
        // Save remaining records
        if (!batchToSave.isEmpty()) {
            transactionRepository.saveAll(batchToSave);
            transactionRepository.flush();
            log.info("Saved final batch of {} records", batchToSave.size());
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("skipCount", skipCount);
        result.put("errorCount", errorCount);
        result.put("errors", errors.size() > 50 ? errors.subList(0, 50) : errors); // Limit errors returned
        result.put("uploadBatchId", uploadBatchId);
        result.put("totalProcessed", transactions.size());
        
        log.info("Import completed: {} success, {} skipped, {} errors out of {} total", 
                 successCount, skipCount, errorCount, transactions.size());
        
        return result;
    }
    
    /**
     * Async version of importTransactions - processes in background and updates job status
     */
    @Async("uploadTaskExecutor")
    @Transactional
    public void importTransactionsAsync(
            List<CreditCardTransactionUploadDTO> transactions,
            String uploadBatchId,
            String filename,
            String username,
            String jobId) {
        
        UploadJobStatus jobStatus = UploadJobStatus.get(jobId);
        if (jobStatus == null) {
            log.error("Job status not found for jobId: {}", jobId);
            return;
        }
        
        jobStatus.setStatus(UploadJobStatus.Status.PROCESSING);
        jobStatus.setTotalRecords(transactions.size());
        jobStatus.setMessage("Starting import...");
        jobStatus.update();
        
        int successCount = 0;
        int skipCount = 0;
        int errorCount = 0;
        List<String> errors = new ArrayList<>();
        List<CreditCardTransaction> batchToSave = new ArrayList<>();
        
        LocalDateTime uploadDate = LocalDateTime.now();
        final int BATCH_SIZE = 100;
        
        try {
            // Pre-load existing transaction keys for duplicate checking
            jobStatus.setMessage("Checking for duplicates...");
            jobStatus.update();
            
            Set<String> existingKeys = preloadExistingTransactionKeys(transactions);
            Set<String> newKeys = new HashSet<>();
            
            jobStatus.setMessage("Processing records...");
            jobStatus.update();
            
            for (int i = 0; i < transactions.size(); i++) {
                CreditCardTransactionUploadDTO dto = transactions.get(i);
                int rowNumber = i + 2;
                
                try {
                    enrichWithCabAndDriver(dto);
                    validateRow(dto);
                    
                    if (!dto.isValid()) {
                        errors.add("Row " + rowNumber + ": " + dto.getValidationMessage());
                        errorCount++;
                        continue;
                    }
                    
                    String key = buildTransactionKey(dto);
                    if (existingKeys.contains(key) || newKeys.contains(key)) {
                        skipCount++;
                        continue;
                    }
                    
                    CreditCardTransaction transaction = convertToEntity(dto);
                    transaction.setUploadBatchId(uploadBatchId);
                    transaction.setUploadFilename(filename);
                    transaction.setUploadDate(uploadDate);
                    
                    batchToSave.add(transaction);
                    newKeys.add(key);
                    successCount++;
                    
                    if (batchToSave.size() >= BATCH_SIZE) {
                        transactionRepository.saveAll(batchToSave);
                        transactionRepository.flush();
                        batchToSave.clear();
                    }
                    
                } catch (Exception e) {
                    errors.add("Row " + rowNumber + ": " + e.getMessage());
                    errorCount++;
                }
                
                // Update progress every 50 records
                if (i % 50 == 0 || i == transactions.size() - 1) {
                    jobStatus.setProcessedRecords(i + 1);
                    jobStatus.setSuccessCount(successCount);
                    jobStatus.setSkipCount(skipCount);
                    jobStatus.setErrorCount(errorCount);
                    jobStatus.setMessage(String.format("Processing %d of %d...", i + 1, transactions.size()));
                    jobStatus.update();
                }
            }
            
            // Save remaining
            if (!batchToSave.isEmpty()) {
                transactionRepository.saveAll(batchToSave);
                transactionRepository.flush();
            }
            
            // Build result
            Map<String, Object> result = new HashMap<>();
            result.put("successCount", successCount);
            result.put("skipCount", skipCount);
            result.put("errorCount", errorCount);
            result.put("errors", errors.size() > 50 ? errors.subList(0, 50) : errors);
            result.put("uploadBatchId", uploadBatchId);
            result.put("totalProcessed", transactions.size());
            
            jobStatus.setStatus(UploadJobStatus.Status.COMPLETED);
            jobStatus.setMessage("Import completed successfully");
            jobStatus.setResult(result);
            jobStatus.setErrors(errors);
            jobStatus.setEndTime(LocalDateTime.now());
            jobStatus.update();
            
            log.info("Async import completed: {} success, {} skipped, {} errors", 
                     successCount, skipCount, errorCount);
                     
        } catch (Exception e) {
            log.error("Async import failed: {}", e.getMessage(), e);
            jobStatus.setStatus(UploadJobStatus.Status.FAILED);
            jobStatus.setMessage("Import failed: " + e.getMessage());
            jobStatus.setEndTime(LocalDateTime.now());
            jobStatus.update();
        }
    }
    
    /**
     * Pre-load existing transaction keys for the date range in the upload
     */
    private Set<String> preloadExistingTransactionKeys(List<CreditCardTransactionUploadDTO> transactions) {
        // Find min/max dates in the upload
        LocalDate minDate = null;
        LocalDate maxDate = null;
        
        for (CreditCardTransactionUploadDTO dto : transactions) {
            if (dto.getTransactionDate() != null) {
                if (minDate == null || dto.getTransactionDate().isBefore(minDate)) {
                    minDate = dto.getTransactionDate();
                }
                if (maxDate == null || dto.getTransactionDate().isAfter(maxDate)) {
                    maxDate = dto.getTransactionDate();
                }
            }
        }
        
        Set<String> keys = new HashSet<>();
        if (minDate != null && maxDate != null) {
            // Load all transactions in the date range
            List<CreditCardTransaction> existing = transactionRepository
                .findByTransactionDateBetween(minDate, maxDate);
            
            for (CreditCardTransaction t : existing) {
                keys.add(buildTransactionKey(t));
            }
        }
        
        return keys;
    }
    
    private String buildTransactionKey(CreditCardTransactionUploadDTO dto) {
        return String.format("%s|%s|%s|%s|%s",
            dto.getTerminalId(),
            dto.getAuthorizationCode(),
            dto.getAmount(),
            dto.getTransactionDate(),
            dto.getTransactionTime()
        );
    }
    
    private String buildTransactionKey(CreditCardTransaction t) {
        return String.format("%s|%s|%s|%s|%s",
            t.getTerminalId(),
            t.getAuthorizationCode(),
            t.getAmount(),
            t.getTransactionDate(),
            t.getTransactionTime()
        );
    }
    
    private void enrichWithCabAndDriver(CreditCardTransactionUploadDTO dto) {
        StringBuilder lookupMsg = new StringBuilder();
        
        // Step 1: Lookup cab number from merchant2cab mapping
        if (dto.getMerchantId() != null && dto.getTransactionDate() != null) {
            String cabNumber = lookupCabNumber(dto.getMerchantId(), dto.getTransactionDate());
            
            if (cabNumber != null) {
                dto.setCabNumber(cabNumber);
                dto.setCabLookupSuccess(true);
                lookupMsg.append("Cab: ").append(cabNumber).append(" (from merchant mapping). ");
            } else {
                dto.setCabLookupSuccess(false);
                lookupMsg.append("No cab mapping found for merchant ").append(dto.getMerchantId()).append(". ");
            }
        }
        
        // Step 2: Lookup driver from shift based on cab, date, and time
        if (dto.getCabNumber() != null && 
            dto.getTransactionDate() != null && 
            dto.getTransactionTime() != null) {
            
            DriverInfo driverInfo = lookupDriver(
                dto.getCabNumber(), 
                dto.getTransactionDate(), 
                dto.getTransactionTime()
            );
            
            if (driverInfo != null) {
                dto.setDriverNumber(driverInfo.getDriverNumber());
                dto.setDriverName(driverInfo.getDriverName());
                dto.setDriverLookupSuccess(true);
                lookupMsg.append("Driver: ").append(driverInfo.getDriverName()).append(". ");
            } else {
                dto.setDriverLookupSuccess(false);
                lookupMsg.append("No active shift found for this cab/time. ");
            }
        }
        
        dto.setLookupMessage(lookupMsg.toString().trim());
    }
    
    private String lookupCabNumber(String merchantId, LocalDate transactionDate) {
        try {
            List<Merchant2Cab> mappings = merchant2CabRepository
                .findByMerchantNumberAndActiveDateRange(merchantId, transactionDate);
            
            if (!mappings.isEmpty()) {
                return mappings.get(0).getCabNumber();
            }
        } catch (Exception e) {
            log.warn("Error looking up cab for merchant {}: {}", merchantId, e.getMessage());
        }
        
        return null;
    }
    
    private DriverInfo lookupDriver(String cabNumber, LocalDate transactionDate, LocalTime transactionTime) {
        try {
            // Transaction date/time is in Pacific time, but DB stores UTC
            // We need to search a wider date range to account for timezone differences
            // Pacific is UTC-8 (or UTC-7 during DST), so a Pacific date could span 2 UTC dates
            
            LocalDate searchStartDate = transactionDate.minusDays(1);
            LocalDate searchEndDate = transactionDate.plusDays(1);
            
            List<DriverShift> shifts = driverShiftRepository.findByCabNumberAndDateRange(
                cabNumber, searchStartDate, searchEndDate);

            // Truncate transaction time to minutes to handle edge cases where shift logon/logoff
            // doesn't include seconds (e.g., shift ends at 1:30:00 but transaction is at 1:30:13)
            LocalTime truncatedTime = transactionTime.truncatedTo(java.time.temporal.ChronoUnit.MINUTES);
            LocalDateTime txnDateTimeUtc = LocalDateTime.of(transactionDate, truncatedTime);
            
            log.debug("Looking up driver for cab {} - Pacific: {} {} (truncated to {}) -> UTC: {}", 
                cabNumber, transactionDate, transactionTime, truncatedTime, txnDateTimeUtc);
            
            for (DriverShift shift : shifts) {
                if (isTimeInShift(txnDateTimeUtc, shift)) {
                    Driver driver = driverRepository.findByDriverNumber(shift.getDriverNumber()).orElse(null);
                    if (driver != null) {
                        DriverInfo info = new DriverInfo();
                        info.setDriverNumber(driver.getDriverNumber());
                        info.setDriverName(driver.getFirstName() + " " + driver.getLastName());
                        return info;
                    }
                }
            }
            
            // If no match found, try with a 1-minute buffer to catch edge cases
            // where transaction seconds push it just past the shift boundary
            LocalDateTime txnDateTimeWithBuffer = LocalDateTime.of(transactionDate, transactionTime.minusMinutes(1));
            for (DriverShift shift : shifts) {
                if (isTimeInShift(txnDateTimeWithBuffer, shift)) {
                    Driver driver = driverRepository.findByDriverNumber(shift.getDriverNumber()).orElse(null);
                    if (driver != null) {
                        DriverInfo info = new DriverInfo();
                        info.setDriverNumber(driver.getDriverNumber());
                        info.setDriverName(driver.getFirstName() + " " + driver.getLastName());
                        log.debug("Found driver {} using 1-minute buffer for cab {}", 
                            driver.getDriverNumber(), cabNumber);
                        return info;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error looking up driver for cab {} on {}: {}", cabNumber, transactionDate, e.getMessage());
        }
        
        return null;
    }
    
    private boolean isTimeInShift(LocalDateTime transactionDateTime, DriverShift shift) {
        LocalDateTime start = shift.getLogonTime();
        if (start == null) {
            return false;
        }

        LocalDateTime end = shift.getLogoffTime();
        if (end == null) {
            end = start.plusHours(12);
        }

        return !transactionDateTime.isBefore(start) && !transactionDateTime.isAfter(end);
    }
    
    private Map<String, Integer> detectColumnMappings(String[] headers) {
        Map<String, Integer> mappings = new HashMap<>();
        
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i].trim();
            // Remove quotes if present
            header = header.replaceAll("^\"|\"$", "");
            String mappedField = detectFieldMapping(header);
            if (mappedField != null) {
                mappings.put(mappedField, i);
                log.debug("Mapped column '{}' to field '{}'", header, mappedField);
            }
        }
        
        return mappings;
    }
    
    private String detectFieldMapping(String header) {
        String headerLower = header.toLowerCase().trim();
        
        // Exact matches first (for this specific CSV format)
        if (header.equals("Merchant Number")) return "merchantId";
        if (header.equals("Site Number")) return "siteNumber";
        if (header.equals("Device Number")) return "deviceNumber";
        if (header.equals("Transaction Date")) return "transactionDate";
        if (header.equals("Transaction Time")) return "transactionTime";
        if (header.equals("Settlement Date")) return "settlementDate";
        if (header.equals("Authorization Code")) return "authorizationCode";
        if (header.equals("Transaction Amount")) return "amount";
        if (header.equals("Card Type")) return "cardType";
        if (header.equals("Cardholder Number")) return "cardholderNumber";
        if (header.equals("Cardholder's Financial Institution")) return "cardBrand";
        if (header.equals("Batch Number")) return "batchNumber";
        if (header.equals("Invoice Number")) return "invoiceNumber";
        if (header.equals("Merchant Reference Number")) return "merchantReferenceNumber";
        if (header.equals("Transaction Type")) return "transactionType";
        if (header.equals("POT Code")) return "potCode";
        if (header.equals("Store Number")) return "storeNumber";
        if (header.equals("Clerk ID")) return "clerkId";
        if (header.equals("Currency")) return "currency";
        
        // Pattern matching fallback
        if (headerLower.matches(".*merchant.*number.*")) return "merchantId";
        if (headerLower.matches(".*site.*number.*")) return "siteNumber";
        if (headerLower.matches(".*device.*number.*")) return "deviceNumber";
        if (headerLower.matches(".*terminal.*")) return "terminalId";
        if (headerLower.matches(".*auth.*code.*")) return "authorizationCode";
        if (headerLower.matches(".*transaction.*date.*")) return "transactionDate";
        if (headerLower.matches(".*transaction.*time.*")) return "transactionTime";
        if (headerLower.matches(".*settle.*date.*")) return "settlementDate";
        if (headerLower.matches(".*card.*type.*")) return "cardType";
        if (headerLower.matches(".*cardholder.*number.*")) return "cardholderNumber";
        if (headerLower.matches(".*amount.*")) return "amount";
        if (headerLower.matches(".*batch.*")) return "batchNumber";
        
        return null;
    }
    
    private CreditCardTransactionUploadDTO parseRow(
            String[] row, 
            Map<String, Integer> mappings, 
            String filename) {
        
        CreditCardTransactionUploadDTO dto = new CreditCardTransactionUploadDTO();
        dto.setUploadFilename(filename);
        
        try {
            // Parse merchant and terminal
            if (mappings.containsKey("merchantId")) {
                dto.setMerchantId(getValue(row, mappings.get("merchantId")));
            }
            if (mappings.containsKey("siteNumber")) {
                dto.setSiteNumber(getValue(row, mappings.get("siteNumber")));
                dto.setTerminalId(dto.getSiteNumber());
            }
            if (mappings.containsKey("deviceNumber")) {
                dto.setDeviceNumber(getValue(row, mappings.get("deviceNumber")));
                if (dto.getTerminalId() == null) {
                    dto.setTerminalId(dto.getDeviceNumber());
                }
            }
            
            // Transaction identifiers
            if (mappings.containsKey("authorizationCode")) {
                dto.setAuthorizationCode(getValue(row, mappings.get("authorizationCode")));
            }
            if (mappings.containsKey("invoiceNumber")) {
                dto.setInvoiceNumber(getValue(row, mappings.get("invoiceNumber")));
                dto.setReferenceNumber(dto.getInvoiceNumber());
            }
            if (mappings.containsKey("merchantReferenceNumber")) {
                dto.setMerchantReferenceNumber(getValue(row, mappings.get("merchantReferenceNumber")));
                if (dto.getReferenceNumber() == null) {
                    dto.setReferenceNumber(dto.getMerchantReferenceNumber());
                }
            }
            
            // Dates
            if (mappings.containsKey("transactionDate")) {
                dto.setTransactionDate(parseDate(getValue(row, mappings.get("transactionDate"))));
            }
            if (mappings.containsKey("transactionTime")) {
                dto.setTransactionTime(parseTime(getValue(row, mappings.get("transactionTime"))));
            }
            if (mappings.containsKey("settlementDate")) {
                dto.setSettlementDate(parseDate(getValue(row, mappings.get("settlementDate"))));
            }
            
            // Card info
            if (mappings.containsKey("cardType")) {
                String cardType = getValue(row, mappings.get("cardType"));
                dto.setCardType(normalizeCardType(cardType));
            }
            if (mappings.containsKey("cardholderNumber")) {
                String cardNum = getValue(row, mappings.get("cardholderNumber"));
                dto.setCardholderNumber(cardNum);
                if (cardNum != null) {
                    String cleaned = cardNum.replaceAll("[^0-9]", "");
                    if (cleaned.length() >= 4) {
                        dto.setCardLastFour(cleaned.substring(cleaned.length() - 4));
                    }
                }
            }
            if (mappings.containsKey("cardBrand")) {
                dto.setCardBrand(getValue(row, mappings.get("cardBrand")));
            }
            
            // Transaction details
            if (mappings.containsKey("transactionType")) {
                String txnType = getValue(row, mappings.get("transactionType"));
                dto.setTransactionType(txnType);
                dto.setTransactionStatus(mapTransactionStatus(txnType));
            }
            if (mappings.containsKey("amount")) {
                BigDecimal amount = parseAmount(getValue(row, mappings.get("amount")));
                dto.setAmount(amount != null ? amount.abs() : null);
                
                if (amount != null && amount.compareTo(BigDecimal.ZERO) < 0) {
                    dto.setIsRefunded(true);
                    dto.setRefundAmount(amount.abs());
                    dto.setTransactionStatus("REFUNDED");
                }
            }
            if (mappings.containsKey("batchNumber")) {
                dto.setBatchNumber(getValue(row, mappings.get("batchNumber")));
            }
            
            // Other fields
            if (mappings.containsKey("potCode")) {
                dto.setPotCode(getValue(row, mappings.get("potCode")));
            }
            if (mappings.containsKey("storeNumber")) {
                dto.setStoreNumber(getValue(row, mappings.get("storeNumber")));
            }
            if (mappings.containsKey("clerkId")) {
                dto.setClerkId(getValue(row, mappings.get("clerkId")));
            }
            if (mappings.containsKey("currency")) {
                dto.setCurrency(getValue(row, mappings.get("currency")));
            }
            
            // Build notes
            dto.setNotes(buildNotes(dto));
            
            // Set defaults
            if (dto.getTipAmount() == null) {
                dto.setTipAmount(BigDecimal.ZERO);
            }
            if (dto.getTransactionStatus() == null) {
                dto.setTransactionStatus("SETTLED");
            }
            if (dto.getIsSettled() == null) {
                dto.setIsSettled(true);
            }
            if (dto.getIsRefunded() == null) {
                dto.setIsRefunded(false);
            }
            
            // Calculate computed fields
            if (dto.getAmount() != null) {
                dto.setTotalAmount(dto.getAmount().add(dto.getTipAmount()));
                if (dto.getProcessingFee() != null) {
                    dto.setNetAmount(dto.getTotalAmount().subtract(dto.getProcessingFee()));
                } else {
                    dto.setNetAmount(dto.getTotalAmount());
                }
            }
            
        } catch (Exception e) {
            dto.setValid(false);
            dto.setValidationMessage("Parse error: " + e.getMessage());
            log.error("Error parsing row: {}", e.getMessage());
        }
        
        return dto;
    }
    
    private String buildNotes(CreditCardTransactionUploadDTO dto) {
        List<String> noteParts = new ArrayList<>();
        
        if (dto.getPotCode() != null && !dto.getPotCode().isEmpty()) {
            noteParts.add("POT: " + dto.getPotCode());
        }
        if (dto.getStoreNumber() != null && !dto.getStoreNumber().isEmpty()) {
            noteParts.add("Store: " + dto.getStoreNumber());
        }
        if (dto.getClerkId() != null && !dto.getClerkId().isEmpty()) {
            noteParts.add("Clerk: " + dto.getClerkId());
        }
        if (dto.getCurrency() != null && !dto.getCurrency().isEmpty()) {
            noteParts.add("Currency: " + dto.getCurrency());
        }
        
        return noteParts.isEmpty() ? null : String.join(" | ", noteParts);
    }
    
    private String normalizeCardType(String cardType) {
        if (cardType == null) return null;
        
        // Map numeric codes to card types (from your CSV)
        String trimmed = cardType.trim();
        switch (trimmed) {
            case "01": return "VISA";
            case "02": return "MASTERCARD";
            case "03": return "AMEX";
            case "10": return "VISA";  // Alternate code
            default:
                // Try text matching
                String upper = trimmed.toUpperCase();
                if (upper.contains("VISA")) return "VISA";
                if (upper.contains("MASTER") || upper.contains("MC")) return "MASTERCARD";
                if (upper.contains("AMEX") || upper.contains("AMERICAN")) return "AMEX";
                if (upper.contains("DISC")) return "DISCOVER";
                return cardType;
        }
    }
    
    private String mapTransactionStatus(String transactionType) {
        if (transactionType == null) return "SETTLED";
        
        String trimmed = transactionType.trim();
        if (trimmed.equals("1")) return "SETTLED";  // Type 1 = Sale
        if (trimmed.equals("2")) return "REFUNDED"; // Type 2 = Refund
        
        String upper = trimmed.toUpperCase();
        if (upper.contains("SALE") || upper.contains("PURCHASE")) return "SETTLED";
        if (upper.contains("REFUND") || upper.contains("RETURN")) return "REFUNDED";
        if (upper.contains("VOID")) return "DECLINED";
        
        return "SETTLED";
    }
    
    private void validateRow(CreditCardTransactionUploadDTO dto) {
        StringBuilder errors = new StringBuilder();
        
        // Required fields
        if (dto.getAuthorizationCode() == null || dto.getAuthorizationCode().trim().isEmpty()) {
            errors.append("Authorization code is required. ");
        }
        if (dto.getTerminalId() == null || dto.getTerminalId().trim().isEmpty()) {
            errors.append("Terminal/Site ID is required. ");
        }
        if (dto.getMerchantId() == null || dto.getMerchantId().trim().isEmpty()) {
            errors.append("Merchant number is required. ");
        }
        if (dto.getTransactionDate() == null) {
            errors.append("Transaction date is required. ");
        }
        if (dto.getTransactionTime() == null) {
            errors.append("Transaction time is required. ");
        }
        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            errors.append("Valid amount is required. ");
        }
        
        if (errors.length() > 0) {
            dto.setValid(false);
            dto.setValidationMessage(errors.toString().trim());
        } else {
            dto.setValid(true);
            dto.setValidationMessage(null);
        }
    }
    
    private boolean isDuplicate(CreditCardTransactionUploadDTO dto) {
        return transactionRepository.existsByTerminalIdAndAuthorizationCodeAndAmountAndTransactionDateAndTransactionTime(
            dto.getTerminalId(),
            dto.getAuthorizationCode(),
            dto.getAmount(),
            dto.getTransactionDate(),
            dto.getTransactionTime()
        );
    }
    
    private CreditCardTransaction convertToEntity(CreditCardTransactionUploadDTO dto) {
        CreditCardTransaction entity = new CreditCardTransaction();
        
        if (dto.getTransactionId() == null || dto.getTransactionId().trim().isEmpty()) {
            entity.setTransactionId(generateTransactionId(dto));
        } else {
            entity.setTransactionId(dto.getTransactionId());
        }
        
        entity.setAuthorizationCode(dto.getAuthorizationCode());
        entity.setMerchantId(dto.getMerchantId());
        entity.setTerminalId(dto.getTerminalId());
        entity.setTransactionDate(dto.getTransactionDate());
        entity.setTransactionTime(dto.getTransactionTime());
        entity.setSettlementDate(dto.getSettlementDate());
        entity.setCardType(dto.getCardType());
        entity.setCardLastFour(dto.getCardLastFour());
        entity.setCardBrand(dto.getCardBrand());
        entity.setCabNumber(dto.getCabNumber());
        entity.setDriverNumber(dto.getDriverNumber());
        entity.setJobId(dto.getJobId());
        entity.setAmount(dto.getAmount());
        entity.setTipAmount(dto.getTipAmount());
        entity.setProcessingFee(dto.getProcessingFee());
        entity.setTransactionStatus(parseTransactionStatus(dto.getTransactionStatus()));
        entity.setIsSettled(dto.getIsSettled());
        entity.setIsRefunded(dto.getIsRefunded());
        entity.setRefundAmount(dto.getRefundAmount());
        entity.setRefundDate(dto.getRefundDate());
        entity.setBatchNumber(dto.getBatchNumber());
        entity.setReferenceNumber(dto.getReferenceNumber());
        entity.setCustomerName(dto.getCustomerName());
        entity.setReceiptNumber(dto.getReceiptNumber());
        entity.setNotes(dto.getNotes());
        
        return entity;
    }

    private CreditCardTransaction.TransactionStatus parseTransactionStatus(String status) {
        if (status == null || status.isBlank()) {
            return CreditCardTransaction.TransactionStatus.PENDING;
        }
        try {
            return CreditCardTransaction.TransactionStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return CreditCardTransaction.TransactionStatus.PENDING;
        }
    }
    
    private String generateTransactionId(CreditCardTransactionUploadDTO dto) {
        return String.format("TXN-%s-%s-%d",
            dto.getTerminalId(),
            dto.getAuthorizationCode(),
            System.currentTimeMillis());
    }
    
    private String getValue(String[] row, int index) {
        if (index >= 0 && index < row.length) {
            String value = row[index];
            return (value != null && !value.trim().isEmpty()) ? value.trim() : null;
        }
        return null;
    }
    
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) return null;
        
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(dateStr.trim(), formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        
        throw new IllegalArgumentException("Unable to parse date: " + dateStr);
    }
    
    private LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) return null;
        
        for (DateTimeFormatter formatter : TIME_FORMATTERS) {
            try {
                return LocalTime.parse(timeStr.trim(), formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        
        throw new IllegalArgumentException("Unable to parse time: " + timeStr);
    }
    
    private BigDecimal parseAmount(String amountStr) {
        if (amountStr == null || amountStr.trim().isEmpty()) return null;
        
        try {
            String cleaned = amountStr.replaceAll("[^0-9.-]", "");
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Unable to parse amount: " + amountStr);
        }
    }
    
    private Map<String, String> getDetectedMappingsSummary(Map<String, Integer> mappings, String[] headers) {
        Map<String, String> summary = new HashMap<>();
        for (Map.Entry<String, Integer> entry : mappings.entrySet()) {
            summary.put(entry.getKey(), headers[entry.getValue()]);
        }
        return summary;
    }
    
    @Data
    private static class DriverInfo {
        private String driverNumber;
        private String driverName;
    }
}