package com.taxi.domain.report.service;

import com.taxi.domain.account.service.EmailService;
import com.taxi.domain.driver.model.Driver;
import com.taxi.domain.driver.repository.DriverRepository;
import com.taxi.domain.expense.model.ExpenseCategory;
import com.taxi.domain.expense.repository.ExpenseCategoryRepository;
import com.taxi.domain.report.model.YearEndReportConfig;
import com.taxi.domain.report.repository.YearEndReportConfigRepository;
import com.taxi.domain.revenue.entity.RevenueCategory;
import com.taxi.domain.revenue.repository.RevenueCategoryRepository;
import com.taxi.web.dto.report.DriverSummaryDTO;
import com.taxi.web.dto.report.DriverSummaryReportDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class YearEndReportService {

    private final YearEndReportConfigRepository configRepository;
    private final ExpenseCategoryRepository expenseCategoryRepository;
    private final RevenueCategoryRepository revenueCategoryRepository;
    private final ReportService reportService;
    private final DriverRepository driverRepository;
    private final YearEndReportPdfService pdfService;
    private final EmailService emailService;

    // ========================
    // Config Management
    // ========================

    public List<YearEndReportConfig> getAllConfig() {
        return configRepository.findAllByOrderBySectionAscDisplayOrderAsc();
    }

    public List<YearEndReportConfig> getVisibleConfig() {
        return configRepository.findByIsVisibleTrueOrderBySectionAscDisplayOrderAsc();
    }

    @Transactional
    public void updateConfig(List<Map<String, Object>> updates) {
        for (Map<String, Object> update : updates) {
            Long id = Long.valueOf(update.get("id").toString());
            configRepository.findById(id).ifPresent(config -> {
                if (update.containsKey("isVisible")) {
                    config.setIsVisible(Boolean.valueOf(update.get("isVisible").toString()));
                }
                if (update.containsKey("displayOrder")) {
                    config.setDisplayOrder(Integer.parseInt(update.get("displayOrder").toString()));
                }
                if (update.containsKey("itemLabel")) {
                    config.setItemLabel(update.get("itemLabel").toString());
                }
                configRepository.save(config);
            });
        }
    }

    /**
     * Add a new custom config item (e.g., a new expense/revenue category)
     */
    @Transactional
    public YearEndReportConfig addConfigItem(String section, String itemKey, String itemLabel, int displayOrder) {
        if (configRepository.existsBySectionAndItemKey(section, itemKey)) {
            throw new RuntimeException("Config item already exists: " + section + "/" + itemKey);
        }
        return configRepository.save(YearEndReportConfig.builder()
                .section(section)
                .itemKey(itemKey)
                .itemLabel(itemLabel)
                .isVisible(true)
                .displayOrder(displayOrder)
                .build());
    }

    @Transactional
    public void deleteConfigItem(Long id) {
        configRepository.deleteById(id);
    }

    /**
     * Sync config with current expense/revenue categories.
     * Adds any new categories that aren't in the config yet.
     */
    @Transactional
    public Map<String, Object> syncConfigWithCategories() {
        int added = 0;

        // Add expense categories
        List<ExpenseCategory> expenseCategories = expenseCategoryRepository.findAll();
        int expOrder = 100;
        for (ExpenseCategory ec : expenseCategories) {
            String key = "EC_" + ec.getId();
            if (!configRepository.existsBySectionAndItemKey("EXPENSE", key)) {
                configRepository.save(YearEndReportConfig.builder()
                        .section("EXPENSE")
                        .itemKey(key)
                        .itemLabel(ec.getCategoryName())
                        .isVisible(true)
                        .displayOrder(expOrder++)
                        .build());
                added++;
            }
        }

        // Add revenue categories
        List<RevenueCategory> revenueCategories = revenueCategoryRepository.findAll();
        int revOrder = 100;
        for (RevenueCategory rc : revenueCategories) {
            String key = "RC_" + rc.getId();
            if (!configRepository.existsBySectionAndItemKey("REVENUE", key)) {
                configRepository.save(YearEndReportConfig.builder()
                        .section("REVENUE")
                        .itemKey(key)
                        .itemLabel(rc.getCategoryName())
                        .isVisible(true)
                        .displayOrder(revOrder++)
                        .build());
                added++;
            }
        }

        return Map.of("added", added, "total", configRepository.count());
    }

    // ========================
    // Report Generation
    // ========================

    /**
     * Generate financial report for a single driver over a date range.
     */
    public Map<String, Object> generateReport(String driverNumber, LocalDate startDate, LocalDate endDate) {
        log.info("Generating report for driver {} from {} to {}", driverNumber, startDate, endDate);

        // Get visible config items
        List<YearEndReportConfig> visibleConfig = getVisibleConfig();
        Set<String> visibleRevKeys = visibleConfig.stream()
                .filter(c -> "REVENUE".equals(c.getSection()))
                .map(YearEndReportConfig::getItemKey)
                .collect(Collectors.toSet());
        Set<String> visibleExpKeys = visibleConfig.stream()
                .filter(c -> "EXPENSE".equals(c.getSection()))
                .map(YearEndReportConfig::getItemKey)
                .collect(Collectors.toSet());
        Set<String> visibleSummaryKeys = visibleConfig.stream()
                .filter(c -> "SUMMARY".equals(c.getSection()))
                .map(YearEndReportConfig::getItemKey)
                .collect(Collectors.toSet());
        boolean showTax = visibleConfig.stream().anyMatch(c -> "TAX".equals(c.getSection()) && c.getIsVisible());
        boolean showCommission = visibleConfig.stream().anyMatch(c -> "COMMISSION".equals(c.getSection()) && c.getIsVisible());

        // Generate report for this single driver only (not all drivers)
        DriverSummaryDTO driverSummary = reportService.generateSingleDriverSummary(driverNumber, startDate, endDate);

        if (driverSummary == null) {
            return Map.of("error", "No data found for driver " + driverNumber + " from " + startDate + " to " + endDate,
                    "driverNumber", driverNumber, "startDate", startDate.toString(), "endDate", endDate.toString());
        }

        return buildReportResponse(driverSummary, startDate, endDate, visibleRevKeys, visibleExpKeys,
                visibleSummaryKeys, showTax, showCommission, visibleConfig);
    }

    /**
     * Generate year-end reports for all drivers.
     */
    public Map<String, Object> generateAllReports(LocalDate startDate, LocalDate endDate) {
        log.info("Generating reports for all drivers from {} to {}", startDate, endDate);

        List<YearEndReportConfig> visibleConfig = getVisibleConfig();
        Set<String> visibleRevKeys = visibleConfig.stream()
                .filter(c -> "REVENUE".equals(c.getSection()))
                .map(YearEndReportConfig::getItemKey)
                .collect(Collectors.toSet());
        Set<String> visibleExpKeys = visibleConfig.stream()
                .filter(c -> "EXPENSE".equals(c.getSection()))
                .map(YearEndReportConfig::getItemKey)
                .collect(Collectors.toSet());
        Set<String> visibleSummaryKeys = visibleConfig.stream()
                .filter(c -> "SUMMARY".equals(c.getSection()))
                .map(YearEndReportConfig::getItemKey)
                .collect(Collectors.toSet());
        boolean showTax = visibleConfig.stream().anyMatch(c -> "TAX".equals(c.getSection()) && c.getIsVisible());
        boolean showCommission = visibleConfig.stream().anyMatch(c -> "COMMISSION".equals(c.getSection()) && c.getIsVisible());

        DriverSummaryReportDTO fullReport = reportService.generateDriverSummaryReport(startDate, endDate);

        List<Map<String, Object>> reports = fullReport.getDriverSummaries().stream()
                .map(ds -> buildReportResponse(ds, startDate, endDate, visibleRevKeys, visibleExpKeys,
                        visibleSummaryKeys, showTax, showCommission, visibleConfig))
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("startDate", startDate.toString());
        result.put("endDate", endDate.toString());
        result.put("driverCount", reports.size());
        result.put("reports", reports);
        return result;
    }

    /**
     * Build the filtered report response for a single driver.
     */
    private Map<String, Object> buildReportResponse(DriverSummaryDTO ds, LocalDate startDate, LocalDate endDate,
            Set<String> visibleRevKeys, Set<String> visibleExpKeys,
            Set<String> visibleSummaryKeys, boolean showTax, boolean showCommission,
            List<YearEndReportConfig> visibleConfig) {

        Map<String, Object> report = new LinkedHashMap<>();

        // Driver info
        report.put("driverNumber", ds.getDriverNumber());
        report.put("driverName", ds.getDriverName());
        report.put("isOwner", ds.getIsOwner());
        report.put("startDate", startDate.toString());
        report.put("endDate", endDate.toString());

        // GST number
        driverRepository.findByDriverNumber(ds.getDriverNumber()).ifPresent(driver -> {
            if (driver.getGstNumber() != null && !driver.getGstNumber().isBlank()) {
                report.put("gstNumber", driver.getGstNumber());
            }
        });

        // Revenue section - filter by visible keys
        List<Map<String, Object>> revenues = new ArrayList<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;

        // Built-in revenue items
        if (visibleRevKeys.contains("LEASE_REVENUE") && ds.getLeaseRevenue() != null && ds.getLeaseRevenue().compareTo(BigDecimal.ZERO) != 0) {
            revenues.add(lineItem("Lease Revenue", ds.getLeaseRevenue()));
            totalRevenue = totalRevenue.add(ds.getLeaseRevenue());
        }
        if (visibleRevKeys.contains("CREDIT_CARD_REVENUE") && ds.getCreditCardRevenue() != null && ds.getCreditCardRevenue().compareTo(BigDecimal.ZERO) != 0) {
            revenues.add(lineItem("Credit Card Revenue", ds.getCreditCardRevenue()));
            totalRevenue = totalRevenue.add(ds.getCreditCardRevenue());
        }
        if (visibleRevKeys.contains("CHARGES_REVENUE") && ds.getChargesRevenue() != null && ds.getChargesRevenue().compareTo(BigDecimal.ZERO) != 0) {
            revenues.add(lineItem("Account Charges Revenue", ds.getChargesRevenue()));
            totalRevenue = totalRevenue.add(ds.getChargesRevenue());
        }
        if (visibleRevKeys.contains("OTHER_REVENUE") && ds.getOtherRevenue() != null && ds.getOtherRevenue().compareTo(BigDecimal.ZERO) != 0) {
            revenues.add(lineItem("Other Revenue", ds.getOtherRevenue()));
            totalRevenue = totalRevenue.add(ds.getOtherRevenue());
        }

        // Itemized revenue breakdown items (from dynamic categories)
        if (ds.getRevenueBreakdown() != null) {
            for (DriverSummaryDTO.ItemizedBreakdown item : ds.getRevenueBreakdown()) {
                // Check if the item's key matches any visible config key
                if (visibleRevKeys.contains(item.getKey()) && item.getAmount() != null && item.getAmount().compareTo(BigDecimal.ZERO) != 0) {
                    revenues.add(lineItem(item.getDisplayName(), item.getAmount()));
                    // Don't double-count — these are part of the totals above
                }
            }
        }

        report.put("revenues", revenues);

        // Expense section
        List<Map<String, Object>> expenses = new ArrayList<>();
        BigDecimal totalExpense = BigDecimal.ZERO;

        // Show all individual expense items from the itemized breakdown
        if (ds.getExpenseBreakdown() != null && !ds.getExpenseBreakdown().isEmpty()) {
            // Separate tax items from other expenses
            List<DriverSummaryDTO.ItemizedBreakdown> regularItems = new ArrayList<>();
            List<DriverSummaryDTO.ItemizedBreakdown> taxItems = new ArrayList<>();
            for (DriverSummaryDTO.ItemizedBreakdown item : ds.getExpenseBreakdown()) {
                if (item.getKey() != null && item.getKey().startsWith("TAX:")) {
                    taxItems.add(item);
                } else {
                    regularItems.add(item);
                }
            }

            // Add regular expense items
            for (DriverSummaryDTO.ItemizedBreakdown item : regularItems) {
                if (item.getAmount() != null && item.getAmount().compareTo(BigDecimal.ZERO) != 0) {
                    expenses.add(lineItem(item.getDisplayName(), item.getAmount()));
                    totalExpense = totalExpense.add(item.getAmount());
                }
            }

            // Add tax items as indented sub-items with a total
            if (!taxItems.isEmpty()) {
                BigDecimal totalTax = BigDecimal.ZERO;
                for (DriverSummaryDTO.ItemizedBreakdown item : taxItems) {
                    if (item.getAmount() != null && item.getAmount().compareTo(BigDecimal.ZERO) != 0) {
                        expenses.add(subLineItem(item.getDisplayName(), item.getAmount()));
                        totalTax = totalTax.add(item.getAmount());
                    }
                }
                if (totalTax.compareTo(BigDecimal.ZERO) != 0) {
                    expenses.add(lineItem("Total GST/HST", totalTax));
                    totalExpense = totalExpense.add(totalTax);
                }
            }
        } else {
            // Fallback to grouped totals if no breakdown available
            if (visibleExpKeys.contains("LEASE_EXPENSE") && ds.getLeaseExpense() != null && ds.getLeaseExpense().compareTo(BigDecimal.ZERO) != 0) {
                expenses.add(lineItem("Lease Expense", ds.getLeaseExpense()));
                totalExpense = totalExpense.add(ds.getLeaseExpense());
            }
            if (visibleExpKeys.contains("FIXED_EXPENSE") && ds.getFixedExpense() != null && ds.getFixedExpense().compareTo(BigDecimal.ZERO) != 0) {
                expenses.add(lineItem("Fixed Expenses", ds.getFixedExpense()));
                totalExpense = totalExpense.add(ds.getFixedExpense());
            }
            if (visibleExpKeys.contains("VARIABLE_EXPENSE") && ds.getVariableExpense() != null && ds.getVariableExpense().compareTo(BigDecimal.ZERO) != 0) {
                expenses.add(lineItem("Variable / One-Time Expenses", ds.getVariableExpense()));
                totalExpense = totalExpense.add(ds.getVariableExpense());
            }
            if (visibleExpKeys.contains("INSURANCE_MILEAGE") && ds.getInsuranceMileageExpense() != null && ds.getInsuranceMileageExpense().compareTo(BigDecimal.ZERO) != 0) {
                expenses.add(lineItem("Insurance & Mileage", ds.getInsuranceMileageExpense()));
                totalExpense = totalExpense.add(ds.getInsuranceMileageExpense());
            }
            if (visibleExpKeys.contains("AIRPORT_TRIPS") && ds.getAirportTripCost() != null && ds.getAirportTripCost().compareTo(BigDecimal.ZERO) != 0) {
                String label = "Airport Trip Charges";
                if (ds.getAirportTripCount() != null && ds.getAirportTripCount() > 0) {
                    label += " (" + ds.getAirportTripCount() + " trips)";
                }
                expenses.add(lineItem(label, ds.getAirportTripCost()));
                totalExpense = totalExpense.add(ds.getAirportTripCost());
            }
            if (showTax && ds.getTaxExpense() != null && ds.getTaxExpense().compareTo(BigDecimal.ZERO) != 0) {
                expenses.add(lineItem("Taxes (HST/GST)", ds.getTaxExpense()));
                totalExpense = totalExpense.add(ds.getTaxExpense());
            }
            if (showCommission && ds.getCommissionExpense() != null && ds.getCommissionExpense().compareTo(BigDecimal.ZERO) != 0) {
                expenses.add(lineItem("Commissions", ds.getCommissionExpense()));
                totalExpense = totalExpense.add(ds.getCommissionExpense());
            }
        }

        report.put("expenses", expenses);

        // Summary
        BigDecimal netIncome = totalRevenue.subtract(totalExpense);
        Map<String, Object> summary = new LinkedHashMap<>();
        if (visibleSummaryKeys.contains("TOTAL_REVENUE")) summary.put("totalRevenue", totalRevenue);
        if (visibleSummaryKeys.contains("TOTAL_EXPENSE")) summary.put("totalExpenses", totalExpense);
        if (visibleSummaryKeys.contains("NET_INCOME")) summary.put("netIncome", netIncome);
        if (visibleSummaryKeys.contains("PREVIOUS_BALANCE")) summary.put("previousBalance", safe(ds.getPreviousBalance()));
        if (visibleSummaryKeys.contains("PAYMENTS_MADE")) summary.put("paymentsMade", safe(ds.getPaid()));
        report.put("summary", summary);

        return report;
    }

    private Map<String, Object> lineItem(String label, BigDecimal amount) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("label", label);
        item.put("amount", amount != null ? amount : BigDecimal.ZERO);
        return item;
    }

    private Map<String, Object> subLineItem(String label, BigDecimal amount) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("label", label);
        item.put("amount", amount != null ? amount : BigDecimal.ZERO);
        item.put("indent", true);
        return item;
    }

    private BigDecimal safe(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }

    // ========================
    // PDF (from already-generated data)
    // ========================

    /**
     * Convert already-generated single report data to PDF.
     */
    public byte[] convertToPdf(Map<String, Object> reportData) {
        return pdfService.generatePdf(reportData);
    }

    /**
     * Convert already-generated all-driver report data to PDF.
     */
    public byte[] convertAllToPdf(Map<String, Object> allReportsData) {
        return pdfService.generateAllPdf(allReportsData);
    }

    // ========================
    // Email (from already-generated data)
    // ========================

    /**
     * Email already-generated report data as PDF attachment.
     * If toEmail is null/blank, sends to the driver's email on file.
     */
    public String emailReportFromData(Map<String, Object> reportData, String toEmail) {
        String driverNumber = reportData.getOrDefault("driverNumber", "").toString();
        String driverName = reportData.getOrDefault("driverName", driverNumber).toString();

        // Resolve email
        if (toEmail == null || toEmail.isBlank()) {
            Driver driver = driverRepository.findByDriverNumber(driverNumber)
                    .orElseThrow(() -> new RuntimeException("Driver not found: " + driverNumber));
            if (driver.getEmail() == null || driver.getEmail().isBlank()) {
                throw new RuntimeException("No email on file for driver " + driverNumber);
            }
            toEmail = driver.getEmail();
        }

        byte[] pdf = pdfService.generatePdf(reportData);

        String startDate = reportData.getOrDefault("startDate", "").toString();
        String endDate = reportData.getOrDefault("endDate", "").toString();
        String summary = buildEmailSummary(reportData, startDate, endDate);

        emailService.sendDriverReport(toEmail, driverName, summary, pdf);
        log.info("Emailed report for {} to {}", driverNumber, toEmail);
        return toEmail;
    }

    private String buildEmailSummary(Map<String, Object> reportData, String startDate, String endDate) {
        String dateRange = formatDateRange(startDate, endDate);
        StringBuilder sb = new StringBuilder();
        sb.append("<h3>Financial Report: ").append(dateRange).append("</h3>");
        sb.append("<p><strong>Driver:</strong> ").append(reportData.get("driverName"))
          .append(" (").append(reportData.get("driverNumber")).append(")</p>");

        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) reportData.get("summary");
        if (summary != null) {
            if (summary.containsKey("totalRevenue")) {
                sb.append("<p><strong>Total Revenue:</strong> $").append(summary.get("totalRevenue")).append("</p>");
            }
            if (summary.containsKey("totalExpenses")) {
                sb.append("<p><strong>Total Expenses:</strong> $").append(summary.get("totalExpenses")).append("</p>");
            }
            if (summary.containsKey("netIncome")) {
                sb.append("<p><strong>Net Income:</strong> $").append(summary.get("netIncome")).append("</p>");
            }
        }
        sb.append("<p><em>See attached PDF for full details.</em></p>");
        return sb.toString();
    }

    private String formatDateRange(String startStr, String endStr) {
        try {
            DateTimeFormatter in = DateTimeFormatter.ISO_LOCAL_DATE;
            DateTimeFormatter out = DateTimeFormatter.ofPattern("MMM dd, yyyy");
            return LocalDate.parse(startStr, in).format(out) + " — " + LocalDate.parse(endStr, in).format(out);
        } catch (Exception e) {
            return startStr + " — " + endStr;
        }
    }
}
