package com.taxi.domain.account.dto;


import java.util.ArrayList;
import java.util.List;

public class TaxiCallerImportResult {
    private int totalRecords;
    private int successCount;
    private int duplicateCount;
    private int errorCount;
    private List<String> errors;
    private List<String> duplicateJobIds;

    public TaxiCallerImportResult() {
        this.errors = new ArrayList<>();
        this.duplicateJobIds = new ArrayList<>();
    }

    public void incrementSuccess() {
        this.successCount++;
    }

    public void incrementDuplicate(String jobId) {
        this.duplicateCount++;
        this.duplicateJobIds.add(jobId);
    }

    public void incrementError(String errorMessage) {
        this.errorCount++;
        this.errors.add(errorMessage);
    }

    // Getters and Setters
    public int getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(int totalRecords) {
        this.totalRecords = totalRecords;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getDuplicateCount() {
        return duplicateCount;
    }

    public void setDuplicateCount(int duplicateCount) {
        this.duplicateCount = duplicateCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public List<String> getDuplicateJobIds() {
        return duplicateJobIds;
    }

    public void setDuplicateJobIds(List<String> duplicateJobIds) {
        this.duplicateJobIds = duplicateJobIds;
    }

    @Override
    public String toString() {
        return String.format(
            "Import Result: Total=%d, Success=%d, Duplicates=%d, Errors=%d",
            totalRecords, successCount, duplicateCount, errorCount
        );
    }
}
