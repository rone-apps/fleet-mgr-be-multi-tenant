package com.taxi.domain.csvuploader;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataMapperService {

    private final CreditCardTransactionUploadService uploadService;

    /**
     * Target fields for credit card transactions with display labels.
     */
    public static final LinkedHashMap<String, String> CREDIT_CARD_TARGET_FIELDS = new LinkedHashMap<>() {{
        put("merchantId", "Merchant Number");
        put("siteNumber", "Site / Terminal Number");
        put("deviceNumber", "Device Number");
        put("authorizationCode", "Authorization Code");
        put("transactionDate", "Transaction Date");
        put("transactionTime", "Transaction Time");
        put("settlementDate", "Settlement Date");
        put("amount", "Transaction Amount");
        put("tipAmount", "Tip Amount");
        put("cardType", "Card Type");
        put("cardholderNumber", "Cardholder Number");
        put("cardBrand", "Card Brand / Issuer");
        put("cabNumber", "Cab ID / Number");
        put("driverNumber", "Driver ID / Number");
        put("batchNumber", "Batch Number");
        put("invoiceNumber", "Invoice / Reference Number");
        put("merchantReferenceNumber", "Merchant Reference Number");
        put("transactionType", "Transaction Type");
        put("potCode", "POT Code");
        put("storeNumber", "Store Number");
        put("clerkId", "Clerk ID");
        put("currency", "Currency");
    }};

    /**
     * Analyze uploaded file: extract headers and suggest mappings.
     */
    public Map<String, Object> analyzeFile(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        List<String> headers;
        List<String[]> sampleRows;

        if (filename != null && (filename.endsWith(".xlsx") || filename.endsWith(".xls"))) {
            ExcelParseResult result = parseExcelHeaders(file);
            headers = result.headers;
            sampleRows = result.sampleRows;
        } else {
            CsvParseResult result = parseCsvHeaders(file);
            headers = result.headers;
            sampleRows = result.sampleRows;
        }

        // Suggest mappings using fuzzy matching
        Map<String, String> suggestedMappings = suggestMappings(headers);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("filename", filename);
        response.put("sourceHeaders", headers);
        response.put("targetFields", CREDIT_CARD_TARGET_FIELDS);
        response.put("suggestedMappings", suggestedMappings);
        response.put("sampleData", formatSampleData(headers, sampleRows));
        response.put("totalRows", sampleRows.size());

        return response;
    }

    /**
     * Parse file with user-confirmed mappings and return data as CreditCardTransactionUploadDTOs.
     * Leverages existing upload service for enrichment and validation.
     */
    public CsvUploadPreviewDTO parseWithMappings(MultipartFile file, Map<String, String> userMappings) throws IOException {
        String filename = file.getOriginalFilename();
        List<String> headers;
        List<String[]> dataRows;

        if (filename != null && (filename.endsWith(".xlsx") || filename.endsWith(".xls"))) {
            ExcelParseResult result = parseExcelFull(file);
            headers = result.headers;
            dataRows = result.sampleRows;
        } else {
            CsvParseResult result = parseCsvFull(file);
            headers = result.headers;
            dataRows = result.sampleRows;
        }

        // Build column index mapping: targetField -> column index
        Map<String, Integer> columnMappings = new HashMap<>();
        for (Map.Entry<String, String> entry : userMappings.entrySet()) {
            String sourceHeader = entry.getKey();
            String targetField = entry.getValue();
            if (targetField != null && !targetField.isEmpty() && !"unmapped".equals(targetField)) {
                int idx = headers.indexOf(sourceHeader);
                if (idx >= 0) {
                    columnMappings.put(targetField, idx);
                }
            }
        }

        log.info("Data mapper: parsing {} rows with {} column mappings", dataRows.size(), columnMappings.size());

        // Use the upload service to parse, enrich, and validate
        return uploadService.parseWithCustomMappings(dataRows, columnMappings, filename);
    }

    /**
     * Suggest target field for each source header using fuzzy matching.
     * Returns map of sourceHeader -> targetFieldKey (or "unmapped").
     */
    private Map<String, String> suggestMappings(List<String> sourceHeaders) {
        Map<String, String> mappings = new LinkedHashMap<>();
        Set<String> usedTargets = new HashSet<>();

        for (String header : sourceHeaders) {
            String bestMatch = findBestMatch(header, usedTargets);
            mappings.put(header, bestMatch != null ? bestMatch : "unmapped");
            if (bestMatch != null) {
                usedTargets.add(bestMatch);
            }
        }

        return mappings;
    }

    private String findBestMatch(String sourceHeader, Set<String> usedTargets) {
        String lower = sourceHeader.toLowerCase().trim();
        String bestMatch = null;
        double bestScore = 0.3; // Minimum threshold

        for (Map.Entry<String, String> entry : CREDIT_CARD_TARGET_FIELDS.entrySet()) {
            String fieldKey = entry.getKey();
            String fieldLabel = entry.getValue();
            if (usedTargets.contains(fieldKey)) continue;

            double score = calculateSimilarity(lower, fieldKey.toLowerCase(), fieldLabel.toLowerCase());
            if (score > bestScore) {
                bestScore = score;
                bestMatch = fieldKey;
            }
        }

        return bestMatch;
    }

    private double calculateSimilarity(String source, String fieldKey, String fieldLabel) {
        // Exact match on key or label
        if (source.equals(fieldKey) || source.equals(fieldLabel)) return 1.0;

        // Contains match
        if (source.contains(fieldKey) || fieldKey.contains(source)) return 0.8;
        if (source.contains(fieldLabel) || fieldLabel.contains(source)) return 0.75;

        // Keyword matching
        String[] sourceWords = source.split("[\\s_\\-]+");
        String[] labelWords = fieldLabel.split("[\\s_\\-/]+");
        String[] keyWords = fieldKey.replaceAll("([A-Z])", " $1").toLowerCase().split("\\s+");

        int matchCount = 0;
        for (String sw : sourceWords) {
            if (sw.length() < 2) continue;
            for (String lw : labelWords) {
                if (lw.toLowerCase().contains(sw) || sw.contains(lw.toLowerCase())) {
                    matchCount++;
                    break;
                }
            }
            for (String kw : keyWords) {
                if (kw.contains(sw) || sw.contains(kw)) {
                    matchCount++;
                    break;
                }
            }
        }

        if (sourceWords.length > 0) {
            double ratio = (double) matchCount / (sourceWords.length * 2);
            return Math.min(ratio, 0.7);
        }

        // Common abbreviations
        Map<String, String> abbreviations = Map.of(
            "amt", "amount",
            "txn", "transaction",
            "auth", "authorization",
            "merch", "merchant",
            "ref", "reference",
            "num", "number",
            "dt", "date",
            "tm", "time",
            "crd", "card"
        );

        for (Map.Entry<String, String> abbr : abbreviations.entrySet()) {
            if (source.contains(abbr.getKey())) {
                String expanded = source.replace(abbr.getKey(), abbr.getValue());
                if (fieldLabel.contains(expanded) || fieldKey.contains(expanded)) {
                    return 0.6;
                }
            }
        }

        return 0.0;
    }

    // --- CSV Parsing ---

    private CsvParseResult parseCsvHeaders(MultipartFile file) throws IOException {
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            List<String[]> allRows = reader.readAll();
            if (allRows.isEmpty()) throw new IllegalArgumentException("CSV file is empty");

            String[] headerRow = allRows.get(0);
            if (headerRow.length > 0 && headerRow[0].startsWith("\uFEFF")) {
                headerRow[0] = headerRow[0].substring(1);
            }

            List<String> headers = Arrays.stream(headerRow).map(String::trim).collect(Collectors.toList());
            List<String[]> sample = allRows.subList(1, Math.min(allRows.size(), 6));

            return new CsvParseResult(headers, sample);
        } catch (CsvException e) {
            throw new IOException("Failed to parse CSV: " + e.getMessage(), e);
        }
    }

    private CsvParseResult parseCsvFull(MultipartFile file) throws IOException {
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            List<String[]> allRows = reader.readAll();
            if (allRows.isEmpty()) throw new IllegalArgumentException("CSV file is empty");

            String[] headerRow = allRows.get(0);
            if (headerRow.length > 0 && headerRow[0].startsWith("\uFEFF")) {
                headerRow[0] = headerRow[0].substring(1);
            }

            List<String> headers = Arrays.stream(headerRow).map(String::trim).collect(Collectors.toList());
            List<String[]> data = allRows.subList(1, allRows.size());

            return new CsvParseResult(headers, data);
        } catch (CsvException e) {
            throw new IOException("Failed to parse CSV: " + e.getMessage(), e);
        }
    }

    // --- Excel Parsing ---

    private ExcelParseResult parseExcelHeaders(MultipartFile file) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) throw new IllegalArgumentException("Excel file has no header row");

            List<String> headers = new ArrayList<>();
            DataFormatter formatter = new DataFormatter();
            for (Cell cell : headerRow) {
                headers.add(formatter.formatCellValue(cell).trim());
            }

            List<String[]> sample = new ArrayList<>();
            int lastRow = Math.min(sheet.getLastRowNum(), 5);
            for (int i = 1; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String[] values = new String[headers.size()];
                for (int j = 0; j < headers.size(); j++) {
                    Cell cell = row.getCell(j);
                    values[j] = cell != null ? formatter.formatCellValue(cell).trim() : "";
                }
                sample.add(values);
            }

            return new ExcelParseResult(headers, sample);
        }
    }

    private ExcelParseResult parseExcelFull(MultipartFile file) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) throw new IllegalArgumentException("Excel file has no header row");

            List<String> headers = new ArrayList<>();
            DataFormatter formatter = new DataFormatter();
            for (Cell cell : headerRow) {
                headers.add(formatter.formatCellValue(cell).trim());
            }

            List<String[]> data = new ArrayList<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String[] values = new String[headers.size()];
                for (int j = 0; j < headers.size(); j++) {
                    Cell cell = row.getCell(j);
                    values[j] = cell != null ? formatter.formatCellValue(cell).trim() : "";
                }
                data.add(values);
            }

            return new ExcelParseResult(headers, data);
        }
    }

    private List<Map<String, String>> formatSampleData(List<String> headers, List<String[]> rows) {
        List<Map<String, String>> sample = new ArrayList<>();
        for (String[] row : rows) {
            Map<String, String> rowMap = new LinkedHashMap<>();
            for (int i = 0; i < headers.size() && i < row.length; i++) {
                rowMap.put(headers.get(i), row[i]);
            }
            sample.add(rowMap);
        }
        return sample;
    }

    private static class CsvParseResult {
        final List<String> headers;
        final List<String[]> sampleRows;
        CsvParseResult(List<String> headers, List<String[]> sampleRows) {
            this.headers = headers;
            this.sampleRows = sampleRows;
        }
    }

    private static class ExcelParseResult {
        final List<String> headers;
        final List<String[]> sampleRows;
        ExcelParseResult(List<String> headers, List<String[]> sampleRows) {
            this.headers = headers;
            this.sampleRows = sampleRows;
        }
    }
}
