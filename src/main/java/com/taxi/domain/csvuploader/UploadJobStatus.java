package com.taxi.domain.csvuploader;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class UploadJobStatus {
    
    public enum Status {
        PENDING, PROCESSING, COMPLETED, FAILED
    }
    
    private String jobId;
    private Status status = Status.PENDING;
    private String message;
    private int totalRecords;
    private int processedRecords;
    private int successCount;
    private int skipCount;
    private int errorCount;
    private List<String> errors = new ArrayList<>();
    private Map<String, Object> result;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    
    public int getProgressPercent() {
        if (totalRecords == 0) return 0;
        return (int) ((processedRecords * 100.0) / totalRecords);
    }
    
    // In-memory store for job statuses (could be replaced with Redis for distributed systems)
    private static final ConcurrentHashMap<String, UploadJobStatus> jobStore = new ConcurrentHashMap<>();
    
    public static UploadJobStatus create(String jobId) {
        UploadJobStatus status = new UploadJobStatus();
        status.setJobId(jobId);
        status.setStartTime(LocalDateTime.now());
        jobStore.put(jobId, status);
        return status;
    }
    
    public static UploadJobStatus get(String jobId) {
        return jobStore.get(jobId);
    }
    
    public static void remove(String jobId) {
        jobStore.remove(jobId);
    }
    
    public void update() {
        jobStore.put(this.jobId, this);
    }
}
