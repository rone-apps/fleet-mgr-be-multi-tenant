package com.taxi.web.controller;

import com.taxi.domain.csvuploader.*;
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

import java.util.*;

@RestController
@RequestMapping("/uploads/data-mapper")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT', 'MANAGER')")
@Slf4j
public class DataMapperController {

    private final DataMapperService dataMapperService;
    private final CreditCardTransactionUploadService uploadService;
    private final CsvUploadCacheService cacheService;

    /**
     * Analyze uploaded file: extract headers and suggest column mappings.
     * Accepts CSV or Excel (.xlsx) files.
     */
    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeFile(@RequestParam("file") MultipartFile file) {
        log.info("Data mapper analyze: filename={}, size={} bytes", file.getOriginalFilename(), file.getSize());

        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            }

            String filename = file.getOriginalFilename();
            if (filename == null || (!filename.endsWith(".csv") && !filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
                return ResponseEntity.badRequest().body(Map.of("error", "Unsupported file type. Please upload CSV or Excel (.xlsx) file."));
            }

            Map<String, Object> result = dataMapperService.analyzeFile(file);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error analyzing file: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to analyze file: " + e.getMessage()));
        }
    }

    /**
     * Preview data with user-confirmed mappings.
     * Parses the full file using the provided column mappings, enriches with cab/driver lookups.
     *
     * @param file The CSV or Excel file
     * @param mappingsJson JSON string of source header -> target field mappings
     */
    @PostMapping("/preview")
    @Transactional(timeout = 1800)
    public ResponseEntity<?> previewWithMappings(
            @RequestParam("file") MultipartFile file,
            @RequestParam("mappings") String mappingsJson) {

        log.info("Data mapper preview: filename={}, mappings={}", file.getOriginalFilename(), mappingsJson);

        try {
            // Parse mappings JSON
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, String> userMappings = objectMapper.readValue(mappingsJson, Map.class);

            CsvUploadPreviewDTO preview = dataMapperService.parseWithMappings(file, userMappings);

            // Generate session ID and cache full data
            String sessionId = UUID.randomUUID().toString();

            @SuppressWarnings("unchecked")
            List<CreditCardTransactionUploadDTO> fullData = (List<CreditCardTransactionUploadDTO>) preview.getPreviewData();
            cacheService.storeCreditCardData(sessionId, fullData, preview);

            preview.setSessionId(sessionId);

            // Only send first 100 rows to FE for display
            if (fullData.size() > 100) {
                preview.setPreviewData(fullData.subList(0, 100));
            }

            log.info("Data mapper preview: {} total rows cached, sessionId={}", preview.getTotalRows(), sessionId);
            return ResponseEntity.ok(preview);

        } catch (Exception e) {
            log.error("Error previewing mapped data: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to preview data: " + e.getMessage()));
        }
    }

    /**
     * Import mapped data. Uses the same import logic as the standard credit card upload.
     * Retrieves cached data from the preview step.
     */
    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Transactional(timeout = 1800)
    public ResponseEntity<Map<String, Object>> importMappedData(
            @RequestParam String sessionId,
            @RequestParam String filename,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Data mapper import: sessionId={}, filename={}", sessionId, filename);

        try {
            List<CreditCardTransactionUploadDTO> transactions = cacheService.getCreditCardData(sessionId);

            if (transactions == null || transactions.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Session expired or not found. Please re-upload the file."));
            }

            String uploadBatchId = UUID.randomUUID().toString();

            Map<String, Object> result = uploadService.importTransactions(
                    transactions, uploadBatchId, filename, userDetails.getUsername());

            cacheService.remove(sessionId);

            log.info("Data mapper import completed: batchId={}, success={}, skipped={}, errors={}",
                    uploadBatchId, result.get("successCount"), result.get("skipCount"), result.get("errorCount"));

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error importing mapped data: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Import failed: " + e.getMessage()));
        }
    }

    /**
     * Get available target fields for a data type.
     */
    @GetMapping("/target-fields/{dataType}")
    public ResponseEntity<Map<String, String>> getTargetFields(@PathVariable String dataType) {
        if ("credit-card".equals(dataType)) {
            return ResponseEntity.ok(DataMapperService.CREDIT_CARD_TARGET_FIELDS);
        }
        return ResponseEntity.badRequest().body(Map.of("error", "Unknown data type: " + dataType));
    }
}
