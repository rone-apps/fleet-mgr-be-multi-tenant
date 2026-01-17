package com.taxi.web.controller;

import com.taxi.domain.csvuploader.CsvUploadCacheService;
import com.taxi.domain.csvuploader.CsvUploadPreviewDTO;
import com.taxi.domain.csvuploader.MileageUploadDTO;
import com.taxi.domain.csvuploader.MileageUploadService;
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

@RestController
@RequestMapping("/uploads/mileage")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT', 'MANAGER')")
@Slf4j
public class MileageUploadController {

    private final MileageUploadService uploadService;
    private final CsvUploadCacheService cacheService;

    @PostMapping("/preview")
    @Transactional(timeout = 1800) // 30 minutes
    public ResponseEntity<CsvUploadPreviewDTO> previewCsv(@RequestParam("file") MultipartFile file) {
        log.info("Mileage CSV upload: filename={}, size={} bytes",
                 file.getOriginalFilename(), file.getSize());

        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            if (!file.getOriginalFilename().toLowerCase().endsWith(".csv")) {
                return ResponseEntity.badRequest().build();
            }

            // Generate session ID
            String sessionId = UUID.randomUUID().toString();

            long startTime = System.currentTimeMillis();
            CsvUploadPreviewDTO preview = uploadService.parseAndPreview(file);
            long duration = System.currentTimeMillis() - startTime;

            // Cache the full data server-side
            @SuppressWarnings("unchecked")
            List<MileageUploadDTO> fullData = (List<MileageUploadDTO>) preview.getMileagePreviewData();
            cacheService.storeMileageData(sessionId, fullData, preview);

            // Add sessionId and limit preview data
            preview.setSessionId(sessionId);
            if (fullData != null && fullData.size() > 100) {
                preview.setMileagePreviewData(fullData.subList(0, 100));
            }

            log.info("Preview complete: {} total rows cached in {}ms, sessionId={}", 
                     preview.getTotalRows(), duration, sessionId);

            return ResponseEntity.ok(preview);

        } catch (Exception e) {
            log.error("Error previewing CSV: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Transactional(timeout = 1800) // 30 minutes
    public ResponseEntity<Map<String, Object>> importRecords(
            @RequestParam String sessionId,
            @RequestParam String filename,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Mileage import: sessionId={}, filename='{}', user='{}'",
                 sessionId, filename, userDetails.getUsername());

        try {
            // Retrieve cached data
            List<MileageUploadDTO> records = cacheService.getMileageData(sessionId);

            if (records == null || records.isEmpty()) {
                log.error("No cached data found for session: {}", sessionId);
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Session expired or not found. Please re-upload the file.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            log.info("Retrieved {} records from cache for session {}", records.size(), sessionId);

            String uploadBatchId = UUID.randomUUID().toString();

            long startTime = System.currentTimeMillis();
            Map<String, Object> result = uploadService.importRecords(
                records, uploadBatchId, filename, userDetails.getUsername());
            long duration = System.currentTimeMillis() - startTime;

            // Clear cache after import
            cacheService.remove(sessionId);

            log.info("Import complete: success={}, skipped={}, errors={} in {}ms",
                     result.get("successCount"), result.get("skipCount"), result.get("errorCount"), duration);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error importing records: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/cancel/{sessionId}")
    public ResponseEntity<Void> cancelUpload(@PathVariable String sessionId) {
        log.info("Cancelling mileage upload session: {}", sessionId);
        cacheService.remove(sessionId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Mileage Upload Service is running");
    }
}
