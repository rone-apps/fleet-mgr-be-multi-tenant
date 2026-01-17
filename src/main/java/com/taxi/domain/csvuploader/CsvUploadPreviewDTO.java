package com.taxi.domain.csvuploader;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * DTO for CSV upload preview response - supports multiple upload types.
 * Data is cached server-side; sessionId is used to retrieve it for import.
 */
@Data
public class CsvUploadPreviewDTO {
    
    /**
     * Session ID for retrieving cached data during import
     */
    private String sessionId;
    
    /**
     * Original filename
     */
    private String filename;
    
    /**
     * Total number of rows in the CSV (excluding header)
     */
    private int totalRows;
    
    /**
     * List of column headers from CSV
     */
    private List<String> headers;
    
    /**
     * Detected column mappings (field name -> column index)
     */
    private Map<String, Integer> columnMappings;
    
    /**
     * Summary of detected mappings (field name -> column header)
     */
    private Map<String, String> detectedMappings;
    
    /**
     * Preview data for credit card transactions (first 100 rows for display)
     */
    private List<CreditCardTransactionUploadDTO> previewData;
    
    /**
     * Preview data for airport trips
     */
    private List<AirportTripUploadDTO> airportTripPreviewData;
    
    /**
     * Preview data for mileage records
     */
    private List<MileageUploadDTO> mileagePreviewData;
    
    /**
     * Statistics about the preview
     * - validRows: number of valid rows
     * - invalidRows: number of invalid rows
     * - cabMatches: number of successful cab lookups
     * - driverMatches: number of successful driver lookups
     * - uploadType: CREDIT_CARD, MILEAGE, or AIRPORT_TRIPS
     */
    private Map<String, Object> statistics;
}
