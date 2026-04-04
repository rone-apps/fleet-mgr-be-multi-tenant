package com.taxi.web.controller;

import com.taxi.domain.report.model.YearEndReportConfig;
import com.taxi.domain.report.service.GstReturnPdfService;
import com.taxi.domain.report.service.T2125PdfService;
import com.taxi.domain.report.service.YearEndReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/year-end-report")
@RequiredArgsConstructor
public class YearEndReportController {

    private final YearEndReportService yearEndReportService;
    private final T2125PdfService t2125PdfService;
    private final GstReturnPdfService gstReturnPdfService;

    // ========================
    // Config Endpoints
    // ========================

    /**
     * Get all config items (for admin config panel)
     */
    @GetMapping("/config")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<?> getConfig() {
        List<YearEndReportConfig> configs = yearEndReportService.getAllConfig();
        return ResponseEntity.ok(configs.stream().map(this::configToMap).collect(Collectors.toList()));
    }

    /**
     * Update config items (toggle visibility, reorder, rename)
     */
    @PutMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateConfig(@RequestBody List<Map<String, Object>> updates) {
        try {
            yearEndReportService.updateConfig(updates);
            return ResponseEntity.ok(Map.of("message", "Config updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Add a custom config item
     */
    @PostMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addConfigItem(@RequestBody Map<String, String> body) {
        try {
            String section = body.get("section");
            String itemKey = body.get("itemKey");
            String itemLabel = body.get("itemLabel");
            int order = Integer.parseInt(body.getOrDefault("displayOrder", "99"));

            YearEndReportConfig item = yearEndReportService.addConfigItem(section, itemKey, itemLabel, order);
            return ResponseEntity.ok(configToMap(item));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete a config item
     */
    @DeleteMapping("/config/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteConfigItem(@PathVariable Long id) {
        try {
            yearEndReportService.deleteConfigItem(id);
            return ResponseEntity.ok(Map.of("message", "Config item deleted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Sync config with current expense/revenue categories
     */
    @PostMapping("/config/sync-categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> syncCategories() {
        try {
            return ResponseEntity.ok(yearEndReportService.syncConfigWithCategories());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================
    // Report Generation
    // ========================

    /**
     * Generate report for a single driver over a custom date range
     */
    @GetMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DISPATCHER')")
    public ResponseEntity<?> generateReport(
            @RequestParam String driverNumber,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            return ResponseEntity.ok(yearEndReportService.generateReport(driverNumber, startDate, endDate));
        } catch (Exception e) {
            log.error("Error generating report for driver {} ({} to {})", driverNumber, startDate, endDate, e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Generate reports for all drivers over a custom date range
     */
    @GetMapping("/generate-all")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<?> generateAllReports(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            return ResponseEntity.ok(yearEndReportService.generateAllReports(startDate, endDate));
        } catch (Exception e) {
            log.error("Error generating reports for all drivers ({} to {})", startDate, endDate, e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================
    // PDF Download
    // ========================

    /**
     * Convert already-generated report data to PDF for download
     */
    @PostMapping("/download")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DISPATCHER')")
    public ResponseEntity<?> downloadReport(@RequestBody Map<String, Object> reportData) {
        try {
            byte[] pdf = yearEndReportService.convertToPdf(reportData);
            String driverNumber = reportData.getOrDefault("driverNumber", "report").toString();
            String startDate = reportData.getOrDefault("startDate", "").toString();
            String endDate = reportData.getOrDefault("endDate", "").toString();
            String filename = "report_" + driverNumber + "_" + startDate + "_" + endDate + ".pdf";
            return ResponseEntity.ok()
                    .header("Content-Type", "application/pdf")
                    .header("Content-Disposition", "attachment; filename=" + filename)
                    .body(pdf);
        } catch (Exception e) {
            log.error("Error generating PDF from report data", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Convert already-generated all-driver reports to PDF for download
     */
    @PostMapping("/download-all")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<?> downloadAllReports(@RequestBody Map<String, Object> allReportsData) {
        try {
            byte[] pdf = yearEndReportService.convertAllToPdf(allReportsData);
            String startDate = allReportsData.getOrDefault("startDate", "").toString();
            String endDate = allReportsData.getOrDefault("endDate", "").toString();
            String filename = "all_reports_" + startDate + "_" + endDate + ".pdf";
            return ResponseEntity.ok()
                    .header("Content-Type", "application/pdf")
                    .header("Content-Disposition", "attachment; filename=" + filename)
                    .body(pdf);
        } catch (Exception e) {
            log.error("Error generating all-driver PDF", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================
    // Email
    // ========================

    /**
     * Email already-generated report as PDF attachment
     */
    @PostMapping("/email")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<?> emailReport(@RequestBody Map<String, Object> body) {
        try {
            String toEmail = body.get("toEmail") != null ? body.get("toEmail").toString() : null;
            @SuppressWarnings("unchecked")
            Map<String, Object> reportData = (Map<String, Object>) body.get("reportData");
            if (reportData == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "reportData is required"));
            }

            String sentTo = yearEndReportService.emailReportFromData(reportData, toEmail);
            return ResponseEntity.ok(Map.of("message", "Report emailed to " + sentTo));
        } catch (Exception e) {
            log.error("Error emailing report", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================
    // T2125 Tax Form
    // ========================

    /**
     * Generate T2125 worksheet PDF from already-generated report data
     */
    @PostMapping("/t2125")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<?> downloadT2125(@RequestBody Map<String, Object> reportData) {
        try {
            byte[] pdf = t2125PdfService.generateT2125Pdf(reportData);
            String driverNumber = reportData.getOrDefault("driverNumber", "report").toString();
            String startDate = reportData.getOrDefault("startDate", "").toString();
            String endDate = reportData.getOrDefault("endDate", "").toString();
            String filename = "T2125_" + driverNumber + "_" + startDate + "_" + endDate + ".pdf";
            return ResponseEntity.ok()
                    .header("Content-Type", "application/pdf")
                    .header("Content-Disposition", "attachment; filename=" + filename)
                    .body(pdf);
        } catch (Exception e) {
            log.error("Error generating T2125 PDF", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Generate GST/HST return worksheet PDF from already-generated report data
     */
    @PostMapping("/gst-return")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<?> downloadGstReturn(@RequestBody Map<String, Object> reportData) {
        try {
            byte[] pdf = gstReturnPdfService.generateGstReturnPdf(reportData);
            String driverNumber = reportData.getOrDefault("driverNumber", "report").toString();
            String startDate = reportData.getOrDefault("startDate", "").toString();
            String endDate = reportData.getOrDefault("endDate", "").toString();
            String filename = "GST_HST_Return_" + driverNumber + "_" + startDate + "_" + endDate + ".pdf";
            return ResponseEntity.ok()
                    .header("Content-Type", "application/pdf")
                    .header("Content-Disposition", "attachment; filename=" + filename)
                    .body(pdf);
        } catch (Exception e) {
            log.error("Error generating GST/HST return PDF", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Map<String, Object> configToMap(YearEndReportConfig c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("section", c.getSection());
        m.put("itemKey", c.getItemKey());
        m.put("itemLabel", c.getItemLabel());
        m.put("isVisible", c.getIsVisible());
        m.put("displayOrder", c.getDisplayOrder());
        return m;
    }
}
