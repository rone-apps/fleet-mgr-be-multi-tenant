package com.taxi.domain.shift.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DTO to hold import result statistics for driver shifts
 */
@Data
@NoArgsConstructor
public class DriverShiftImportResult {
    private int totalRecords = 0;
    private int successCount = 0;
    private int duplicateCount = 0;
    private int skippedCount = 0;
    private int failedCount = 0;
    private List<String> errors = new ArrayList<>();
    private Map<String, Integer> skippedReasons = new HashMap<>();
    
    public void incrementSuccess() {
        successCount++;
    }
    
    public void incrementDuplicate() {
        duplicateCount++;
    }
    
    public void incrementSkipped() {
        skippedCount++;
    }
    
    public void incrementFailed() {
        failedCount++;
    }
}