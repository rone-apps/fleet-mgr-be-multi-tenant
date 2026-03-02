package com.taxi.domain.report;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class ReportJobStatus {

    public enum Status {
        PENDING, PROCESSING, COMPLETED, FAILED
    }

    private String jobId;
    private Status status = Status.PENDING;
    private String message;
    private int totalDrivers;
    private int processedDrivers;
    private int totalPages;
    private int pagesReady;
    private List<String> errors = new ArrayList<>();
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public int getProgressPercent() {
        if (totalDrivers == 0) return 0;
        return (int) ((processedDrivers * 100.0) / totalDrivers);
    }

    // In-memory store for job statuses
    private static final ConcurrentHashMap<String, ReportJobStatus> jobStore = new ConcurrentHashMap<>();

    public static ReportJobStatus create(String jobId) {
        ReportJobStatus status = new ReportJobStatus();
        status.setJobId(jobId);
        status.setStartTime(LocalDateTime.now());
        jobStore.put(jobId, status);
        return status;
    }

    public static ReportJobStatus get(String jobId) {
        return jobStore.get(jobId);
    }

    public static void remove(String jobId) {
        jobStore.remove(jobId);
    }

    public static ConcurrentHashMap<String, ReportJobStatus> getJobStore() {
        return jobStore;
    }
}
