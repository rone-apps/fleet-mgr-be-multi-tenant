package com.taxi.web.controller;

import com.taxi.domain.csvuploader.UploadJobStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/upload-jobs")
@PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT', 'MANAGER')")
@Slf4j
public class UploadJobController {

    @GetMapping("/{jobId}/status")
    public ResponseEntity<?> getJobStatus(@PathVariable String jobId) {
        UploadJobStatus status = UploadJobStatus.get(jobId);
        
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("jobId", status.getJobId());
        response.put("status", status.getStatus().name());
        response.put("message", status.getMessage());
        response.put("totalRecords", status.getTotalRecords());
        response.put("processedRecords", status.getProcessedRecords());
        response.put("progressPercent", status.getProgressPercent());
        response.put("successCount", status.getSuccessCount());
        response.put("skipCount", status.getSkipCount());
        response.put("errorCount", status.getErrorCount());
        
        // Include full result when completed
        if (status.getStatus() == UploadJobStatus.Status.COMPLETED || 
            status.getStatus() == UploadJobStatus.Status.FAILED) {
            response.put("result", status.getResult());
            response.put("errors", status.getErrors().size() > 20 ? 
                status.getErrors().subList(0, 20) : status.getErrors());
        }
        
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{jobId}")
    public ResponseEntity<?> clearJobStatus(@PathVariable String jobId) {
        UploadJobStatus.remove(jobId);
        return ResponseEntity.ok().build();
    }
}
