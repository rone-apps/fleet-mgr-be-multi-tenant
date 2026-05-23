package com.taxi.infrastructure.plugin.types;

import com.taxi.infrastructure.plugin.core.Plugin;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Plugin interface for data import integrations (CSV, Excel, API-based data sources).
 * Supports previewing and importing data from various file formats.
 */
public interface DataImportPlugin extends Plugin {

    /**
     * Preview data from uploaded file with field mappings.
     *
     * @param file Uploaded file
     * @param mappings Field name mappings (source field → target field)
     * @return Import preview result
     */
    ImportPreview preview(MultipartFile file, Map<String, String> mappings);

    /**
     * Import data from uploaded file with field mappings.
     *
     * @param file Uploaded file
     * @param mappings Field name mappings (source field → target field)
     * @return Import result with success/error counts
     */
    ImportResult importData(MultipartFile file, Map<String, String> mappings);

    /**
     * Preview result containing sample data and validation.
     */
    interface ImportPreview {
        int getTotalRows();
        Object getSampleData();
        boolean isValid();
        String getErrorMessage();
    }

    /**
     * Import result with statistics.
     */
    interface ImportResult {
        int getRecordsProcessed();
        int getRecordsSuccess();
        int getRecordsFailed();
        String getMessage();
    }
}
