package com.taxi.web.controller;

import com.taxi.domain.csvuploader.CreditCardTransactionUploadDTO;
import com.taxi.domain.csvuploader.CreditCardTransactionUploadService;
import com.taxi.domain.csvuploader.CsvUploadCacheService;
import com.taxi.domain.csvuploader.CsvUploadPreviewDTO;
import com.taxi.domain.csvuploader.UploadJobStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for uploading and importing credit card transaction CSV files.
 * Uses server-side caching to avoid sending large datasets back and forth.
 */
@RestController
@RequestMapping("/uploads/credit-card-transactions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT', 'MANAGER')")
@Slf4j
public class CreditCardTransactionUploadController {
    
    private final CreditCardTransactionUploadService uploadService;
    private final CsvUploadCacheService cacheService;
    
    /**
     * Upload and preview CSV file.
     * Parsed data is cached server-side; only preview metadata is returned.
     * 
     * @param file The CSV file to upload
     * @return Preview data with sessionId, statistics, and sample rows
     */
    @PostMapping("/preview")
    @Transactional(timeout = 1800) // 30 minutes
    public ResponseEntity<CsvUploadPreviewDTO> previewCsv(@RequestParam("file") MultipartFile file) {
        log.info("Received CSV upload request: filename={}, size={} bytes", 
                 file.getOriginalFilename(), file.getSize());
        
        try {
            if (file.isEmpty()) {
                log.error("Empty file uploaded");
                return ResponseEntity.badRequest().build();
            }
            
            if (!file.getOriginalFilename().toLowerCase().endsWith(".csv")) {
                log.error("Invalid file type: {}", file.getOriginalFilename());
                return ResponseEntity.badRequest().build();
            }
            
            // Generate session ID for this upload
            String sessionId = UUID.randomUUID().toString();
            
            // Parse CSV
            long startTime = System.currentTimeMillis();
            CsvUploadPreviewDTO preview = uploadService.parseAndPreview(file);
            long duration = System.currentTimeMillis() - startTime;
            
            // Cache the full data server-side
            @SuppressWarnings("unchecked")
            List<CreditCardTransactionUploadDTO> fullData = (List<CreditCardTransactionUploadDTO>) preview.getPreviewData();
            cacheService.storeCreditCardData(sessionId, fullData, preview);
            
            // Add sessionId to preview and limit preview data sent to FE
            preview.setSessionId(sessionId);
            
            // Only send first 100 rows to FE for display
            if (fullData.size() > 100) {
                preview.setPreviewData(fullData.subList(0, 100));
            }
            
            log.info("CSV preview successful: {} total rows cached in {}ms, sessionId={}", 
                     preview.getTotalRows(), duration, sessionId);
            
            return ResponseEntity.ok(preview);
            
        } catch (Exception e) {
            log.error("Error previewing CSV: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Import transactions from cached data.
     * Uses sessionId to retrieve data from server-side cache.
     * 
     * @param sessionId The session ID from preview
     * @param filename Original filename for tracking
     * @param userDetails Current authenticated user
     * @return Import results with success/error counts
     */
    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Transactional(timeout = 1800) // 30 minutes
    public ResponseEntity<Map<String, Object>> importTransactions(
            @RequestParam String sessionId,
            @RequestParam String filename,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("Import request: sessionId={}, filename='{}', user='{}'", 
                 sessionId, filename, userDetails.getUsername());
        
        try {
            // Retrieve cached data
            List<CreditCardTransactionUploadDTO> transactions = cacheService.getCreditCardData(sessionId);
            
            if (transactions == null || transactions.isEmpty()) {
                log.error("No cached data found for session: {}", sessionId);
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Session expired or not found. Please re-upload the file.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            
            log.info("Retrieved {} transactions from cache for session {}", transactions.size(), sessionId);
            
            // Generate unique batch ID for this import
            String uploadBatchId = UUID.randomUUID().toString();
            
            // Import transactions
            long startTime = System.currentTimeMillis();
            Map<String, Object> result = uploadService.importTransactions(
                transactions, 
                uploadBatchId, 
                filename, 
                userDetails.getUsername()
            );
            long duration = System.currentTimeMillis() - startTime;
            
            // Clear cache after successful import
            cacheService.remove(sessionId);
            
            log.info("Import completed: batchId={}, success={}, skipped={}, errors={} in {}ms", 
                     uploadBatchId,
                     result.get("successCount"),
                     result.get("skipCount"),
                     result.get("errorCount"),
                     duration);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error importing transactions: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Async import - returns immediately with a jobId, processes in background.
     * Frontend should poll /upload-jobs/{jobId}/status for progress.
     */
    @PostMapping("/import-async")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> importTransactionsAsync(
            @RequestParam String sessionId,
            @RequestParam String filename,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("Async import request: sessionId={}, filename='{}', user='{}'", 
                 sessionId, filename, userDetails.getUsername());
        
        try {
            // Retrieve cached data
            List<CreditCardTransactionUploadDTO> transactions = cacheService.getCreditCardData(sessionId);
            
            if (transactions == null || transactions.isEmpty()) {
                log.error("No cached data found for session: {}", sessionId);
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Session expired or not found. Please re-upload the file.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            
            log.info("Retrieved {} transactions from cache for async import", transactions.size());
            
            // Generate unique IDs
            String uploadBatchId = UUID.randomUUID().toString();
            String jobId = UUID.randomUUID().toString();
            
            // Create job status tracker
            UploadJobStatus jobStatus = UploadJobStatus.create(jobId);
            jobStatus.setTotalRecords(transactions.size());
            jobStatus.setMessage("Queued for processing");
            jobStatus.update();
            
            // Start async processing (returns immediately)
            uploadService.importTransactionsAsync(
                transactions, 
                uploadBatchId, 
                filename, 
                userDetails.getUsername(),
                jobId
            );
            
            // Clear cache (data is now being processed)
            cacheService.remove(sessionId);
            
            // Return job ID for polling
            Map<String, Object> response = new HashMap<>();
            response.put("jobId", jobId);
            response.put("uploadBatchId", uploadBatchId);
            response.put("status", "PENDING");
            response.put("message", "Import started. Poll /upload-jobs/" + jobId + "/status for progress.");
            response.put("totalRecords", transactions.size());
            
            return ResponseEntity.accepted().body(response);
            
        } catch (Exception e) {
            log.error("Error starting async import: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Cancel an upload session and clear cached data
     */
    @DeleteMapping("/cancel/{sessionId}")
    public ResponseEntity<Void> cancelUpload(@PathVariable String sessionId) {
        log.info("Cancelling upload session: {}", sessionId);
        cacheService.remove(sessionId);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Get column mapping help/documentation
     */
    @GetMapping("/column-mappings")
    public ResponseEntity<Map<String, String>> getColumnMappings() {
        Map<String, String> mappings = Map.ofEntries(
            Map.entry("Merchant Number", "merchantId - Merchant identifier"),
            Map.entry("Site Number", "terminalId - Terminal/site identifier"),
            Map.entry("Device Number", "terminalId - Device identifier (fallback)"),
            Map.entry("Authorization Code", "authorizationCode - Auth code from processor"),
            Map.entry("Transaction Date", "transactionDate - Date of transaction"),
            Map.entry("Transaction Time", "transactionTime - Time of transaction"),
            Map.entry("Settlement Date", "settlementDate - Date funds settled"),
            Map.entry("Transaction Amount", "amount - Transaction amount"),
            Map.entry("Card Type", "cardType - Card type (VISA, MC, AMEX, etc.)"),
            Map.entry("Cardholder Number", "cardLastFour - Last 4 digits extracted"),
            Map.entry("Cardholder's Financial Institution", "cardBrand - Card issuer"),
            Map.entry("Batch Number", "batchNumber - Settlement batch"),
            Map.entry("Invoice Number", "referenceNumber - Invoice/reference number"),
            Map.entry("Transaction Type", "transactionStatus - Transaction status"),
            Map.entry("POT Code", "notes - Point of transaction code"),
            Map.entry("Store Number", "notes - Store number"),
            Map.entry("Clerk ID", "notes - Clerk identifier"),
            Map.entry("Currency", "notes - Currency code")
        );
        
        return ResponseEntity.ok(mappings);
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Credit Card Transaction Upload Service is running");
    }
}
