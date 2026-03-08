package com.taxi.domain.report.service;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.taxi.web.dto.report.OwnerReportDTO;
import com.taxi.web.dto.expense.StatementLineItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for generating professional PDF reports matching the frontend modal exactly.
 */
@Service
@Slf4j
public class ReportPdfService {

    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("$#,##0.00");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final Pattern CAB_NUMBER_PATTERN = Pattern.compile("Cab\\s+(\\d+)");

    // Colors matching the frontend modal
    private static final BaseColor HEADER_BG = new BaseColor(44, 62, 80);
    private static final BaseColor GREEN = new BaseColor(56, 142, 60);       // #388e3c - Lease Revenue
    private static final BaseColor GREEN_LIGHT = new BaseColor(200, 230, 201); // #c8e6c9
    private static final BaseColor GREEN_BG = new BaseColor(232, 245, 233);   // #e8f5e9
    private static final BaseColor BLUE = new BaseColor(25, 118, 210);       // #1976d2 - Account Charges
    private static final BaseColor BLUE_DARK = new BaseColor(21, 101, 192);  // #1565c0 - Airport Trips
    private static final BaseColor RED = new BaseColor(211, 47, 47);         // #d32f2f - Credit Card
    private static final BaseColor RED_LIGHT = new BaseColor(255, 205, 210); // #ffcdd2
    private static final BaseColor RED_BG = new BaseColor(255, 235, 238);    // #ffebee
    private static final BaseColor ORANGE = new BaseColor(245, 127, 0);      // #f57c00 - Other Revenues
    private static final BaseColor ORANGE_DARK = new BaseColor(230, 81, 0);  // #e65100 - Recurring Expenses
    private static final BaseColor YELLOW_ORANGE = new BaseColor(245, 127, 23); // #f57f17 - Lease Expenses
    private static final BaseColor YELLOW_BG = new BaseColor(255, 243, 224); // #fff3e0
    private static final BaseColor PINK = new BaseColor(194, 24, 91);        // #c2185b - One-Time / Insurance
    private static final BaseColor OLIVE = new BaseColor(85, 139, 47);       // #558b2f - Per-Unit
    private static final BaseColor GRAY_BG = new BaseColor(245, 245, 245);   // #f5f5f5

    private final Font cellFont = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL);
    private final Font cellBoldFont = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD);
    private final Font smallFont = new Font(Font.FontFamily.HELVETICA, 7, Font.NORMAL, new BaseColor(100, 100, 100));

    /**
     * Generate professional PDF report from OwnerReportDTO matching the modal detail view exactly.
     */
    public byte[] generateDriverReportPdf(OwnerReportDTO report) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.LETTER, 36, 36, 36, 36);
            PdfWriter.getInstance(document, baos);
            document.open();

            // 1. Header
            addReportHeader(document, report);

            // 2. Financial Summary Box (top overview)
            addFinancialSummary(document, report);

            // === REVENUE SECTIONS ===

            // Partition revenues
            List<OwnerReportDTO.RevenueLineItem> leaseRevenues = filterRevenues(report, "LEASE_INCOME");
            List<OwnerReportDTO.RevenueLineItem> accountRevenues = filterRevenues(report, "ACCOUNT_REVENUE");
            List<OwnerReportDTO.RevenueLineItem> creditCardRevenues = filterRevenues(report, "CREDIT_CARD_REVENUE");
            List<OwnerReportDTO.RevenueLineItem> otherRevenues = filterRevenues(report, "OTHER_REVENUE");

            // 3. Lease Revenue - grouped by cab
            if (!leaseRevenues.isEmpty()) {
                addLeaseRevenueSection(document, leaseRevenues);
            }

            // 4. Account Charges - sorted by date
            if (!accountRevenues.isEmpty()) {
                addAccountChargesSection(document, accountRevenues);
            }

            // 5. Credit Card Revenue
            if (!creditCardRevenues.isEmpty()) {
                addSimpleRevenueSection(document, "Credit Card Revenue", RED, creditCardRevenues, false);
            }

            // 6. Other Revenues
            if (!otherRevenues.isEmpty()) {
                addSimpleRevenueSection(document, "Other Revenues", ORANGE, otherRevenues, false);
            }

            // === EXPENSE SECTIONS ===

            // 7. Recurring Expenses
            if (report.getRecurringExpenses() != null && !report.getRecurringExpenses().isEmpty()) {
                addRecurringExpensesSection(document, report.getRecurringExpenses());
            }

            // Partition one-time expenses
            List<StatementLineItem> leaseExpenses = report.getOneTimeExpenses() != null
                    ? report.getOneTimeExpenses().stream()
                        .filter(exp -> "LEASE_RENT".equals(exp.getApplicationType()))
                        .collect(Collectors.toList())
                    : List.of();

            List<StatementLineItem> otherOneTimeExpenses = report.getOneTimeExpenses() != null
                    ? report.getOneTimeExpenses().stream()
                        .filter(exp -> !"LEASE_RENT".equals(exp.getApplicationType())
                                && !"AIRPORT_TRIP".equals(exp.getCategoryCode())
                                && !"AIRPORT".equals(exp.getApplicationType()))
                        .collect(Collectors.toList())
                    : List.of();

            // 8. Lease Expenses - grouped by cab
            if (!leaseExpenses.isEmpty()) {
                addLeaseExpensesSection(document, leaseExpenses);
            }

            // 9. One-Time Expenses - sorted by date
            if (!otherOneTimeExpenses.isEmpty()) {
                addOneTimeExpensesSection(document, otherOneTimeExpenses);
            }

            // 10. Per-Unit Expenses
            if (report.getPerUnitExpenses() != null && !report.getPerUnitExpenses().isEmpty()) {
                addPerUnitExpensesSection(document, report.getPerUnitExpenses());
            }

            // 11. Airport Trip Expenses
            List<StatementLineItem> airportExpenses = report.getAirportTripExpenses() != null
                    ? report.getAirportTripExpenses() : List.of();
            if (!airportExpenses.isEmpty()) {
                addAirportTripExpensesSection(document, airportExpenses);
            }

            // 12. Insurance Mileage Expenses
            if (report.getInsuranceMileageExpenses() != null && !report.getInsuranceMileageExpenses().isEmpty()) {
                addInsuranceMileageExpensesSection(document, report.getInsuranceMileageExpenses());
            }

            // 13. Totals Footer (matching modal exactly)
            addTotalsFooter(document, report);

            // 14. Footer
            addFooter(document);

            document.close();
            return baos.toByteArray();

        } catch (DocumentException e) {
            log.error("Error generating PDF report", e);
            throw new RuntimeException("Failed to generate PDF report: " + e.getMessage(), e);
        }
    }

    // ==================== HEADER ====================

    private void addReportHeader(Document document, OwnerReportDTO report) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell();
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setBackgroundColor(HEADER_BG);
        cell.setPadding(15);

        Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, BaseColor.WHITE);
        Paragraph title = new Paragraph("Financial Statement", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(title);

        Font companyFont = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, new BaseColor(189, 195, 199));
        Paragraph company = new Paragraph("Maclures Cabs", companyFont);
        company.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(company);

        headerTable.addCell(cell);
        document.add(headerTable);

        // Driver name, period, status
        Font nameFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, HEADER_BG);
        Paragraph name = new Paragraph(report.getOwnerName(), nameFont);
        name.setAlignment(Element.ALIGN_CENTER);
        name.setSpacingBefore(10);
        document.add(name);

        Font periodFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, new BaseColor(100, 100, 100));
        String period = report.getPeriodFrom().format(DATE_FORMAT) + "  to  " + report.getPeriodTo().format(DATE_FORMAT);
        Paragraph periodPara = new Paragraph(period, periodFont);
        periodPara.setAlignment(Element.ALIGN_CENTER);
        document.add(periodPara);

        String statusText = report.getStatus() != null ? report.getStatus().name() : "DRAFT";
        Paragraph statusPara = new Paragraph("Status: " + statusText, periodFont);
        statusPara.setAlignment(Element.ALIGN_CENTER);
        document.add(statusPara);

        document.add(new Paragraph(" "));
    }

    // ==================== FINANCIAL SUMMARY ====================

    private void addFinancialSummary(Document document, OwnerReportDTO report) throws DocumentException {
        Font summaryTitle = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, HEADER_BG);
        Paragraph title = new Paragraph("Financial Summary", summaryTitle);
        title.setSpacingBefore(5);
        document.add(title);

        // Outer box
        PdfPTable outer = new PdfPTable(1);
        outer.setWidthPercentage(100);
        outer.setSpacingBefore(5);
        outer.setSpacingAfter(10);

        PdfPCell outerCell = new PdfPCell();
        outerCell.setBorderColor(new BaseColor(200, 200, 200));
        outerCell.setPadding(10);

        // Two-column summary table
        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(100);

        Font labelFont = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, new BaseColor(80, 80, 80));
        Font valueFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, new BaseColor(60, 60, 60));
        Font greenValueFont = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, GREEN);
        Font redValueFont = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, RED);

        // Revenue breakdown
        addSummaryHeader(summaryTable, "REVENUES", GREEN);

        // Lease revenue subtotal
        List<OwnerReportDTO.RevenueLineItem> leaseRevs = filterRevenues(report, "LEASE_INCOME");
        if (!leaseRevs.isEmpty()) {
            BigDecimal leaseTotal = leaseRevs.stream().map(OwnerReportDTO.RevenueLineItem::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            addSummaryRow(summaryTable, "  Lease Revenue", CURRENCY_FORMAT.format(leaseTotal), labelFont, valueFont);
        }

        List<OwnerReportDTO.RevenueLineItem> accountRevs = filterRevenues(report, "ACCOUNT_REVENUE");
        if (!accountRevs.isEmpty()) {
            BigDecimal accountTotal = accountRevs.stream().map(OwnerReportDTO.RevenueLineItem::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            addSummaryRow(summaryTable, "  Account Charges", CURRENCY_FORMAT.format(accountTotal), labelFont, valueFont);
        }

        List<OwnerReportDTO.RevenueLineItem> ccRevs = filterRevenues(report, "CREDIT_CARD_REVENUE");
        if (!ccRevs.isEmpty()) {
            BigDecimal ccTotal = ccRevs.stream().map(OwnerReportDTO.RevenueLineItem::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            addSummaryRow(summaryTable, "  Credit Card Revenue", CURRENCY_FORMAT.format(ccTotal), labelFont, valueFont);
        }

        List<OwnerReportDTO.RevenueLineItem> otherRevs = filterRevenues(report, "OTHER_REVENUE");
        if (!otherRevs.isEmpty()) {
            BigDecimal otherTotal = otherRevs.stream().map(OwnerReportDTO.RevenueLineItem::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            addSummaryRow(summaryTable, "  Other Revenues", CURRENCY_FORMAT.format(otherTotal), labelFont, valueFont);
        }

        addSummaryRow(summaryTable, "Total Revenues",
                CURRENCY_FORMAT.format(report.getTotalRevenues() != null ? report.getTotalRevenues() : BigDecimal.ZERO),
                new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, GREEN), greenValueFont);

        // Divider
        addSummaryDivider(summaryTable);

        // Expense breakdown
        addSummaryHeader(summaryTable, "EXPENSES", RED);

        if (report.getTotalRecurringExpenses() != null && report.getTotalRecurringExpenses().compareTo(BigDecimal.ZERO) != 0) {
            addSummaryRow(summaryTable, "  Recurring Expenses", CURRENCY_FORMAT.format(report.getTotalRecurringExpenses()), labelFont, valueFont);
        }

        // Lease expenses subtotal
        BigDecimal leaseExpTotal = report.getOneTimeExpenses() != null
                ? report.getOneTimeExpenses().stream()
                    .filter(e -> "LEASE_RENT".equals(e.getApplicationType()))
                    .map(StatementLineItem::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                : BigDecimal.ZERO;
        if (leaseExpTotal.compareTo(BigDecimal.ZERO) != 0) {
            addSummaryRow(summaryTable, "  Lease Expenses", CURRENCY_FORMAT.format(leaseExpTotal), labelFont, valueFont);
        }

        // Other one-time expenses subtotal
        BigDecimal otherOneTimeTotal = report.getOneTimeExpenses() != null
                ? report.getOneTimeExpenses().stream()
                    .filter(e -> !"LEASE_RENT".equals(e.getApplicationType())
                            && !"AIRPORT_TRIP".equals(e.getCategoryCode())
                            && !"AIRPORT".equals(e.getApplicationType()))
                    .map(StatementLineItem::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                : BigDecimal.ZERO;
        if (otherOneTimeTotal.compareTo(BigDecimal.ZERO) != 0) {
            addSummaryRow(summaryTable, "  One-Time Expenses", CURRENCY_FORMAT.format(otherOneTimeTotal), labelFont, valueFont);
        }

        if (report.getTotalPerUnitExpenses() != null && report.getTotalPerUnitExpenses().compareTo(BigDecimal.ZERO) != 0) {
            addSummaryRow(summaryTable, "  Per-Unit Expenses", CURRENCY_FORMAT.format(report.getTotalPerUnitExpenses()), labelFont, valueFont);
        }

        if (report.getTotalAirportTripExpenses() != null && report.getTotalAirportTripExpenses().compareTo(BigDecimal.ZERO) != 0) {
            addSummaryRow(summaryTable, "  Airport Trip Expenses", CURRENCY_FORMAT.format(report.getTotalAirportTripExpenses()), labelFont, valueFont);
        }

        if (report.getTotalInsuranceMileageExpenses() != null && report.getTotalInsuranceMileageExpenses().compareTo(BigDecimal.ZERO) != 0) {
            addSummaryRow(summaryTable, "  Insurance Mileage", CURRENCY_FORMAT.format(report.getTotalInsuranceMileageExpenses()), labelFont, valueFont);
        }

        addSummaryRow(summaryTable, "Total Expenses",
                CURRENCY_FORMAT.format(report.getTotalExpenses() != null ? report.getTotalExpenses() : BigDecimal.ZERO),
                new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, RED), redValueFont);

        // Divider
        addSummaryDivider(summaryTable);

        // Previous Balance
        BigDecimal prevBalance = report.getPreviousBalance() != null ? report.getPreviousBalance() : BigDecimal.ZERO;
        if (prevBalance.compareTo(BigDecimal.ZERO) != 0) {
            addSummaryRow(summaryTable, "Previous Balance", CURRENCY_FORMAT.format(prevBalance), labelFont, valueFont);
        }

        // Net Due
        BigDecimal netDue = report.getNetDue() != null ? report.getNetDue() : BigDecimal.ZERO;
        String netLabel = netDue.compareTo(BigDecimal.ZERO) > 0 ? "NET PAYABLE" : "NET DUE";
        Font netFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD,
                netDue.compareTo(BigDecimal.ZERO) > 0 ? GREEN : RED);
        String netValue = (netDue.compareTo(BigDecimal.ZERO) > 0 ? "" : "-") + CURRENCY_FORMAT.format(netDue.abs());
        addSummaryRow(summaryTable, netLabel, netValue, netFont, netFont);

        // Paid Amount
        BigDecimal paid = report.getPaidAmount() != null ? report.getPaidAmount() : BigDecimal.ZERO;
        if (paid.compareTo(BigDecimal.ZERO) != 0) {
            addSummaryRow(summaryTable, "Amount Paid", CURRENCY_FORMAT.format(paid), labelFont, valueFont);
        }

        outerCell.addElement(summaryTable);
        outer.addCell(outerCell);
        document.add(outer);
    }

    // ==================== LEASE REVENUE (grouped by cab) ====================

    private void addLeaseRevenueSection(Document document, List<OwnerReportDTO.RevenueLineItem> leaseRevenues) throws DocumentException {
        addSectionTitle(document, "Lease Revenue", GREEN);

        PdfPTable table = new PdfPTable(new float[]{10, 14, 30, 16, 16, 14});
        table.setWidthPercentage(100);
        table.setSpacingBefore(5);
        table.setSpacingAfter(10);

        addColoredHeaders(table, new String[]{"Cab", "Date", "Description", "Fixed Lease", "Mileage Lease", "Total"}, GREEN);

        // Group by cab number (natural number sort)
        Map<String, List<OwnerReportDTO.RevenueLineItem>> grouped = new LinkedHashMap<>();
        for (OwnerReportDTO.RevenueLineItem rev : leaseRevenues) {
            String cabNum = extractCabNumber(rev.getDescription());
            grouped.computeIfAbsent(cabNum, k -> new ArrayList<>()).add(rev);
        }
        List<String> sortedCabs = new ArrayList<>(grouped.keySet());
        sortedCabs.sort((a, b) -> Integer.compare(parseIntSafe(a), parseIntSafe(b)));

        BigDecimal grandTotal = BigDecimal.ZERO;

        for (String cab : sortedCabs) {
            List<OwnerReportDTO.RevenueLineItem> items = grouped.get(cab);
            items.sort(Comparator.comparing(r -> r.getRevenueDate() != null ? r.getRevenueDate().toString() : ""));

            addCabHeaderRow(table, "Cab " + cab, 6, GREEN_LIGHT, GREEN);

            BigDecimal cabSubtotal = BigDecimal.ZERO;
            Font cabFont = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, GREEN);
            Font grayFont = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, new BaseColor(100, 100, 100));
            Font greenBoldFont = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, GREEN);

            for (OwnerReportDTO.RevenueLineItem rev : items) {
                addTableCell(table, "Cab " + cab, cabFont);
                addTableCell(table, rev.getRevenueDate() != null ? rev.getRevenueDate().format(DATE_FORMAT) : "-", cellFont);
                addTableCell(table, rev.getDescription() != null ? rev.getDescription() : "-", cellFont);

                // Fixed lease
                if (rev.getLeaseBreakdown() != null && rev.getLeaseBreakdown().getFixedLeaseAmount() != null) {
                    addTableCell(table, CURRENCY_FORMAT.format(rev.getLeaseBreakdown().getFixedLeaseAmount()), grayFont, Element.ALIGN_RIGHT);
                } else {
                    addTableCell(table, "-", grayFont, Element.ALIGN_RIGHT);
                }

                // Mileage lease
                if (rev.getLeaseBreakdown() != null && rev.getLeaseBreakdown().getMileageLeaseAmount() != null) {
                    addTableCell(table, CURRENCY_FORMAT.format(rev.getLeaseBreakdown().getMileageLeaseAmount()), grayFont, Element.ALIGN_RIGHT);
                } else {
                    addTableCell(table, "-", grayFont, Element.ALIGN_RIGHT);
                }

                // Total
                addTableCell(table, CURRENCY_FORMAT.format(rev.getAmount()), greenBoldFont, Element.ALIGN_RIGHT);
                cabSubtotal = cabSubtotal.add(rev.getAmount());
            }

            addSubtotalRow(table, "Cab " + cab + " Subtotal:", cabSubtotal, 6, GREEN_BG, GREEN);
            grandTotal = grandTotal.add(cabSubtotal);
        }

        addGrandTotalRow(table, "Lease Revenue Total:", grandTotal, 6, GREEN_LIGHT);

        document.add(table);
    }

    // ==================== SIMPLE REVENUE SECTIONS (Account, CC, Other) ====================

    private void addSimpleRevenueSection(Document document, String title, BaseColor color,
                                          List<OwnerReportDTO.RevenueLineItem> revenues, boolean sortByDate) throws DocumentException {
        addSectionTitle(document, title, color);

        PdfPTable table = new PdfPTable(new float[]{18, 22, 10, 28, 22});
        table.setWidthPercentage(100);
        table.setSpacingBefore(5);
        table.setSpacingAfter(10);

        addColoredHeaders(table, new String[]{"Date", "Category", "Cab #", "Description", "Amount"}, color);

        List<OwnerReportDTO.RevenueLineItem> sorted = new ArrayList<>(revenues);
        if (sortByDate) {
            sorted.sort(Comparator.comparing(r -> r.getRevenueDate() != null ? r.getRevenueDate().toString() : ""));
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        for (OwnerReportDTO.RevenueLineItem rev : sorted) {
            addTableCell(table, rev.getRevenueDate() != null ? rev.getRevenueDate().format(DATE_FORMAT) : "-", cellFont);
            addTableCell(table, rev.getCategoryName() != null ? rev.getCategoryName() : "-", cellFont);
            addTableCell(table, rev.getCabNumber() != null ? rev.getCabNumber() : "-", cellFont);
            addTableCell(table, rev.getDescription() != null ? rev.getDescription() : "-", cellFont);
            addTableCell(table, CURRENCY_FORMAT.format(rev.getAmount()), cellFont, Element.ALIGN_RIGHT);
            subtotal = subtotal.add(rev.getAmount());
        }

        addSubtotalRow(table, title + " Subtotal:", subtotal, 5, GRAY_BG, new BaseColor(80, 80, 80));

        document.add(table);
    }

    // ==================== ACCOUNT CHARGES ====================

    private void addAccountChargesSection(Document document, List<OwnerReportDTO.RevenueLineItem> revenues) throws DocumentException {
        addSectionTitle(document, "Account Charges", BLUE);

        PdfPTable table = new PdfPTable(new float[]{15, 10, 20, 20, 20, 15});
        table.setWidthPercentage(100);
        table.setSpacingBefore(5);
        table.setSpacingAfter(10);

        addColoredHeaders(table, new String[]{"Date", "Cab #", "Account", "From", "To", "Amount"}, BLUE);

        List<OwnerReportDTO.RevenueLineItem> sorted = new ArrayList<>(revenues);
        sorted.sort(Comparator.comparing(r -> r.getRevenueDate() != null ? r.getRevenueDate().toString() : ""));

        BigDecimal subtotal = BigDecimal.ZERO;
        for (OwnerReportDTO.RevenueLineItem rev : sorted) {
            addTableCell(table, rev.getRevenueDate() != null ? rev.getRevenueDate().format(DATE_FORMAT) : "-", cellFont);
            addTableCell(table, rev.getCabNumber() != null ? rev.getCabNumber() : "-", cellFont);
            addTableCell(table, rev.getAccountName() != null ? rev.getAccountName() : (rev.getDescription() != null ? rev.getDescription() : "-"), cellFont);
            addTableCell(table, rev.getPickupAddress() != null ? rev.getPickupAddress() : "-", cellFont);
            addTableCell(table, rev.getDropoffAddress() != null ? rev.getDropoffAddress() : "-", cellFont);
            addTableCell(table, CURRENCY_FORMAT.format(rev.getAmount()), cellFont, Element.ALIGN_RIGHT);
            subtotal = subtotal.add(rev.getAmount());
        }

        addSubtotalRow(table, "Account Charges Subtotal:", subtotal, 6, GRAY_BG, new BaseColor(80, 80, 80));

        document.add(table);
    }

    // ==================== RECURRING EXPENSES ====================

    private void addRecurringExpensesSection(Document document, List<StatementLineItem> expenses) throws DocumentException {
        addSectionTitle(document, "Recurring Expenses", ORANGE_DARK);

        PdfPTable table = new PdfPTable(new float[]{35, 42, 23});
        table.setWidthPercentage(100);
        table.setSpacingBefore(5);
        table.setSpacingAfter(10);

        addColoredHeaders(table, new String[]{"Category", "Target", "Amount"}, ORANGE_DARK);

        BigDecimal subtotal = BigDecimal.ZERO;
        for (StatementLineItem exp : expenses) {
            addTableCell(table, exp.getCategoryName() != null ? exp.getCategoryName() : "-", cellFont);
            addTableCell(table, exp.getEntityDescription() != null ? exp.getEntityDescription() : "-", cellFont);
            addTableCell(table, CURRENCY_FORMAT.format(exp.getAmount()), cellFont, Element.ALIGN_RIGHT);
            subtotal = subtotal.add(exp.getAmount());
        }

        addSubtotalRow(table, "Recurring Expenses Subtotal:", subtotal, 3, GRAY_BG, new BaseColor(80, 80, 80));

        document.add(table);
    }

    // ==================== LEASE EXPENSES (grouped by cab) ====================

    private void addLeaseExpensesSection(Document document, List<StatementLineItem> leaseExpenses) throws DocumentException {
        addSectionTitle(document, "Lease Expenses", YELLOW_ORANGE);

        PdfPTable table = new PdfPTable(new float[]{10, 14, 30, 16, 16, 14});
        table.setWidthPercentage(100);
        table.setSpacingBefore(5);
        table.setSpacingAfter(10);

        addColoredHeaders(table, new String[]{"Cab", "Date", "Description", "Fixed Lease", "Mileage Lease", "Total"}, YELLOW_ORANGE);

        // Group by cab number
        Map<String, List<StatementLineItem>> grouped = new LinkedHashMap<>();
        for (StatementLineItem exp : leaseExpenses) {
            String cabNum = exp.getCabNumber() != null ? exp.getCabNumber() : extractCabNumber(exp.getDescription());
            grouped.computeIfAbsent(cabNum, k -> new ArrayList<>()).add(exp);
        }
        List<String> sortedCabs = new ArrayList<>(grouped.keySet());
        sortedCabs.sort((a, b) -> Integer.compare(parseIntSafe(a), parseIntSafe(b)));

        BigDecimal grandTotal = BigDecimal.ZERO;

        Font cabFont = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, RED);
        Font grayFont = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, new BaseColor(100, 100, 100));
        Font redBoldFont = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, RED);

        for (String cab : sortedCabs) {
            List<StatementLineItem> items = grouped.get(cab);
            items.sort(Comparator.comparing(e -> e.getDate() != null ? e.getDate().toString() : ""));

            addCabHeaderRow(table, "Cab " + cab, 6, YELLOW_BG, YELLOW_ORANGE);

            BigDecimal cabSubtotal = BigDecimal.ZERO;
            for (StatementLineItem exp : items) {
                addTableCell(table, "Cab " + cab, cabFont);
                addTableCell(table, exp.getDate() != null ? exp.getDate().format(DATE_FORMAT) : "-", cellFont);
                addTableCell(table, exp.getDescription() != null ? exp.getDescription() : "-", cellFont);

                // Fixed lease
                if (exp.getLeaseBreakdown() != null && exp.getLeaseBreakdown().getFixedLeaseAmount() != null) {
                    addTableCell(table, CURRENCY_FORMAT.format(exp.getLeaseBreakdown().getFixedLeaseAmount()), grayFont, Element.ALIGN_RIGHT);
                } else {
                    addTableCell(table, "-", grayFont, Element.ALIGN_RIGHT);
                }

                // Mileage lease
                if (exp.getLeaseBreakdown() != null && exp.getLeaseBreakdown().getMileageLeaseAmount() != null) {
                    addTableCell(table, CURRENCY_FORMAT.format(exp.getLeaseBreakdown().getMileageLeaseAmount()), grayFont, Element.ALIGN_RIGHT);
                } else {
                    addTableCell(table, "-", grayFont, Element.ALIGN_RIGHT);
                }

                // Total
                addTableCell(table, CURRENCY_FORMAT.format(exp.getAmount()), redBoldFont, Element.ALIGN_RIGHT);
                cabSubtotal = cabSubtotal.add(exp.getAmount());
            }

            addSubtotalRow(table, "Cab " + cab + " Subtotal:", cabSubtotal, 6, RED_BG, RED);
            grandTotal = grandTotal.add(cabSubtotal);
        }

        addGrandTotalRow(table, "Lease Expenses Total:", grandTotal, 6, RED_LIGHT);

        document.add(table);
    }

    // ==================== ONE-TIME EXPENSES (sorted by date) ====================

    private void addOneTimeExpensesSection(Document document, List<StatementLineItem> expenses) throws DocumentException {
        addSectionTitle(document, "One-Time Expenses", PINK);

        PdfPTable table = new PdfPTable(new float[]{14, 14, 20, 9, 10, 14, 19});
        table.setWidthPercentage(100);
        table.setSpacingBefore(5);
        table.setSpacingAfter(10);

        Font smallHeaderFont = new Font(Font.FontFamily.HELVETICA, 7, Font.BOLD, BaseColor.WHITE);
        PdfPCell[] headers = createTableHeaderCells(new String[]{"Date", "Category", "Description", "Cab #", "Shift Type", "Details", "Amount"}, smallHeaderFont, PINK);
        for (PdfPCell h : headers) table.addCell(h);

        List<StatementLineItem> sorted = new ArrayList<>(expenses);
        sorted.sort(Comparator.comparing(e -> e.getDate() != null ? e.getDate().toString() : ""));

        Font smallCellFont = new Font(Font.FontFamily.HELVETICA, 7, Font.NORMAL);
        Font smallBoldCellFont = new Font(Font.FontFamily.HELVETICA, 7, Font.BOLD);

        BigDecimal subtotal = BigDecimal.ZERO;
        for (StatementLineItem exp : sorted) {
            addTableCell(table, exp.getDate() != null ? exp.getDate().format(DATE_FORMAT) : "-", smallCellFont);
            addTableCell(table, exp.getCategoryName() != null ? exp.getCategoryName() : "-", smallCellFont);
            addTableCell(table, exp.getDescription() != null ? exp.getDescription() : "-", smallCellFont);
            addTableCell(table, exp.getCabNumber() != null ? exp.getCabNumber() : "-", smallBoldCellFont);
            addTableCell(table, exp.getShiftType() != null ? exp.getShiftType() : "-", smallCellFont);
            addTableCell(table, exp.getChargeTarget() != null ? exp.getChargeTarget() : "-", smallCellFont);
            addTableCell(table, CURRENCY_FORMAT.format(exp.getAmount()), smallCellFont, Element.ALIGN_RIGHT);
            subtotal = subtotal.add(exp.getAmount());
        }

        // Subtotal row
        PdfPCell subLabelCell = new PdfPCell(new Phrase("One-Time Expenses Subtotal:", cellBoldFont));
        subLabelCell.setColspan(6);
        subLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        subLabelCell.setPadding(4);
        subLabelCell.setBackgroundColor(GRAY_BG);
        table.addCell(subLabelCell);

        PdfPCell subValueCell = new PdfPCell(new Phrase(CURRENCY_FORMAT.format(subtotal), cellBoldFont));
        subValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        subValueCell.setPadding(4);
        subValueCell.setBackgroundColor(GRAY_BG);
        table.addCell(subValueCell);

        document.add(table);
    }

    // ==================== PER-UNIT EXPENSES ====================

    private void addPerUnitExpensesSection(Document document, List<OwnerReportDTO.PerUnitExpenseLineItem> expenses) throws DocumentException {
        addSectionTitle(document, "Per-Unit Expenses", OLIVE);

        PdfPTable table = new PdfPTable(new float[]{25, 20, 15, 17, 23});
        table.setWidthPercentage(100);
        table.setSpacingBefore(5);
        table.setSpacingAfter(10);

        addColoredHeaders(table, new String[]{"Name", "Unit Type", "Units", "Rate/Unit", "Amount"}, OLIVE);

        BigDecimal subtotal = BigDecimal.ZERO;
        for (OwnerReportDTO.PerUnitExpenseLineItem exp : expenses) {
            addTableCell(table, exp.getName() != null ? exp.getName() : "-", cellFont);
            addTableCell(table, exp.getUnitTypeDisplay() != null ? exp.getUnitTypeDisplay() : (exp.getUnitType() != null ? exp.getUnitType() : "-"), cellFont);
            addTableCell(table, exp.getTotalUnits() != null ? exp.getTotalUnits().setScale(2, RoundingMode.HALF_UP).toString() : "-", cellFont, Element.ALIGN_RIGHT);
            addTableCell(table, exp.getRate() != null ? CURRENCY_FORMAT.format(exp.getRate()) : "-", cellFont, Element.ALIGN_RIGHT);
            addTableCell(table, CURRENCY_FORMAT.format(exp.getAmount()), cellFont, Element.ALIGN_RIGHT);
            subtotal = subtotal.add(exp.getAmount());
        }

        // Subtotal
        PdfPCell subLabelCell = new PdfPCell(new Phrase("Per-Unit Expenses Subtotal:", cellBoldFont));
        subLabelCell.setColspan(4);
        subLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        subLabelCell.setPadding(4);
        subLabelCell.setBackgroundColor(GRAY_BG);
        table.addCell(subLabelCell);

        PdfPCell subValueCell = new PdfPCell(new Phrase(CURRENCY_FORMAT.format(subtotal), cellBoldFont));
        subValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        subValueCell.setPadding(4);
        subValueCell.setBackgroundColor(GRAY_BG);
        table.addCell(subValueCell);

        document.add(table);
    }

    // ==================== AIRPORT TRIP EXPENSES (sorted by date desc) ====================

    private void addAirportTripExpensesSection(Document document, List<StatementLineItem> airportExpenses) throws DocumentException {
        addSectionTitle(document, "Airport Trip Expenses", BLUE_DARK);

        PdfPTable table = new PdfPTable(new float[]{20, 15, 15, 20, 30});
        table.setWidthPercentage(100);
        table.setSpacingBefore(5);
        table.setSpacingAfter(10);

        addColoredHeaders(table, new String[]{"Date", "Cab #", "Trips", "Rate/Trip", "Amount"}, BLUE_DARK);

        List<StatementLineItem> sorted = new ArrayList<>(airportExpenses);
        sorted.sort((a, b) -> {
            String da = a.getDate() != null ? a.getDate().toString() : "";
            String db = b.getDate() != null ? b.getDate().toString() : "";
            return db.compareTo(da); // descending
        });

        BigDecimal subtotal = BigDecimal.ZERO;
        int totalTrips = 0;
        for (StatementLineItem exp : sorted) {
            addTableCell(table, exp.getDate() != null ? exp.getDate().format(DATE_FORMAT) : "-", cellFont);
            addTableCell(table, exp.getCabNumber() != null ? exp.getCabNumber() : "-", cellFont);
            addTableCell(table, exp.getTripCount() != null ? String.valueOf(exp.getTripCount()) : "-", cellFont, Element.ALIGN_RIGHT);
            addTableCell(table, exp.getRatePerUnit() != null ? CURRENCY_FORMAT.format(exp.getRatePerUnit()) : "-", cellFont, Element.ALIGN_RIGHT);
            addTableCell(table, CURRENCY_FORMAT.format(exp.getAmount()), cellFont, Element.ALIGN_RIGHT);
            subtotal = subtotal.add(exp.getAmount());
            if (exp.getTripCount() != null) totalTrips += exp.getTripCount();
        }

        // Subtotal with total trips
        PdfPCell tripsCell = new PdfPCell(new Phrase("Total: " + totalTrips + " trips", cellBoldFont));
        tripsCell.setColspan(2);
        tripsCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tripsCell.setPadding(4);
        tripsCell.setBackgroundColor(GRAY_BG);
        table.addCell(tripsCell);

        PdfPCell subLabelCell = new PdfPCell(new Phrase("Airport Trips Subtotal:", cellBoldFont));
        subLabelCell.setColspan(2);
        subLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        subLabelCell.setPadding(4);
        subLabelCell.setBackgroundColor(GRAY_BG);
        table.addCell(subLabelCell);

        PdfPCell subValueCell = new PdfPCell(new Phrase(CURRENCY_FORMAT.format(subtotal), cellBoldFont));
        subValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        subValueCell.setPadding(4);
        subValueCell.setBackgroundColor(GRAY_BG);
        table.addCell(subValueCell);

        document.add(table);
    }

    // ==================== INSURANCE MILEAGE EXPENSES (sorted by date asc) ====================

    private void addInsuranceMileageExpensesSection(Document document, List<StatementLineItem> expenses) throws DocumentException {
        addSectionTitle(document, "Insurance Mileage Expenses", PINK);

        PdfPTable table = new PdfPTable(new float[]{20, 15, 20, 20, 25});
        table.setWidthPercentage(100);
        table.setSpacingBefore(5);
        table.setSpacingAfter(10);

        addColoredHeaders(table, new String[]{"Date", "Cab #", "Mileage", "Insurance Rate", "Total"}, PINK);

        List<StatementLineItem> sorted = new ArrayList<>(expenses);
        sorted.sort(Comparator.comparing(e -> e.getDate() != null ? e.getDate().toString() : ""));

        BigDecimal subtotal = BigDecimal.ZERO;
        for (StatementLineItem exp : sorted) {
            BigDecimal miles = exp.getMiles() != null ? exp.getMiles() : BigDecimal.ZERO;
            BigDecimal amount = exp.getAmount() != null ? exp.getAmount() : BigDecimal.ZERO;
            BigDecimal rate = miles.compareTo(BigDecimal.ZERO) > 0
                    ? amount.divide(miles, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

            addTableCell(table, exp.getDate() != null ? exp.getDate().format(DATE_FORMAT) : "-", cellFont);
            Font boldCell = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD);
            addTableCell(table, exp.getCabNumber() != null ? exp.getCabNumber() : "-", boldCell);
            addTableCell(table, miles.setScale(2, RoundingMode.HALF_UP).toString(), cellFont, Element.ALIGN_RIGHT);
            addTableCell(table, CURRENCY_FORMAT.format(rate), cellFont, Element.ALIGN_RIGHT);
            addTableCell(table, CURRENCY_FORMAT.format(amount), cellFont, Element.ALIGN_RIGHT);
            subtotal = subtotal.add(amount);
        }

        // Subtotal
        PdfPCell subLabelCell = new PdfPCell(new Phrase("Insurance Mileage Subtotal:", cellBoldFont));
        subLabelCell.setColspan(4);
        subLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        subLabelCell.setPadding(4);
        subLabelCell.setBackgroundColor(GRAY_BG);
        table.addCell(subLabelCell);

        PdfPCell subValueCell = new PdfPCell(new Phrase(CURRENCY_FORMAT.format(subtotal), cellBoldFont));
        subValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        subValueCell.setPadding(4);
        subValueCell.setBackgroundColor(GRAY_BG);
        table.addCell(subValueCell);

        document.add(table);
    }

    // ==================== TOTALS FOOTER (matching modal exactly) ====================

    private void addTotalsFooter(Document document, OwnerReportDTO report) throws DocumentException {
        document.add(new Paragraph(" "));

        PdfPTable outer = new PdfPTable(1);
        outer.setWidthPercentage(100);

        PdfPCell outerCell = new PdfPCell();
        outerCell.setBorder(PdfPCell.BOX);
        outerCell.setBorderColor(new BaseColor(224, 224, 224));
        outerCell.setPadding(10);
        outerCell.setBackgroundColor(new BaseColor(249, 249, 249));

        PdfPTable totals = new PdfPTable(2);
        totals.setWidthPercentage(100);

        Font labelBold = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, new BaseColor(60, 60, 60));
        Font valueBold = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, new BaseColor(60, 60, 60));

        // Total Revenues (green background)
        addTotalsRow(totals, "TOTAL REVENUES",
                CURRENCY_FORMAT.format(report.getTotalRevenues() != null ? report.getTotalRevenues() : BigDecimal.ZERO),
                GREEN_BG, GREEN);

        // Total Expenses (red background)
        addTotalsRow(totals, "TOTAL EXPENSES",
                CURRENCY_FORMAT.format(report.getTotalExpenses() != null ? report.getTotalExpenses() : BigDecimal.ZERO),
                RED_BG, RED);

        // Spacer
        PdfPCell spacer = new PdfPCell();
        spacer.setColspan(2);
        spacer.setBorder(PdfPCell.NO_BORDER);
        spacer.setFixedHeight(8);
        totals.addCell(spacer);

        // Previous Balance (if non-zero)
        BigDecimal prevBalance = report.getPreviousBalance() != null ? report.getPreviousBalance() : BigDecimal.ZERO;
        if (prevBalance.compareTo(BigDecimal.ZERO) != 0) {
            addSimpleTotalsRow(totals, "Previous Balance", CURRENCY_FORMAT.format(prevBalance), labelBold, valueBold);
        }

        // Net Due/Payable (highlighted)
        BigDecimal netDue = report.getNetDue() != null ? report.getNetDue() : BigDecimal.ZERO;
        boolean isPayable = netDue.compareTo(BigDecimal.ZERO) > 0;
        BaseColor netColor = isPayable ? new BaseColor(27, 94, 32) : new BaseColor(183, 28, 28);
        BaseColor netBg = isPayable ? GREEN_LIGHT : RED_LIGHT;
        String netLabel = isPayable ? "NET PAYABLE" : "NET DUE";
        String netValue = (isPayable ? "" : "-") + CURRENCY_FORMAT.format(netDue.abs());

        Font netLabelFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, netColor);
        Font netValueFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, netColor);

        PdfPCell netLabelCell = new PdfPCell(new Phrase(netLabel, netLabelFont));
        netLabelCell.setBorder(PdfPCell.TOP);
        netLabelCell.setBorderColor(new BaseColor(153, 153, 153));
        netLabelCell.setBorderWidth(2);
        netLabelCell.setBackgroundColor(netBg);
        netLabelCell.setPadding(8);
        totals.addCell(netLabelCell);

        PdfPCell netValueCell = new PdfPCell(new Phrase(netValue, netValueFont));
        netValueCell.setBorder(PdfPCell.TOP);
        netValueCell.setBorderColor(new BaseColor(153, 153, 153));
        netValueCell.setBorderWidth(2);
        netValueCell.setBackgroundColor(netBg);
        netValueCell.setPadding(8);
        netValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totals.addCell(netValueCell);

        // Amount Paid (if non-zero)
        BigDecimal paid = report.getPaidAmount() != null ? report.getPaidAmount() : BigDecimal.ZERO;
        if (paid.compareTo(BigDecimal.ZERO) != 0) {
            addSimpleTotalsRow(totals, "Amount Paid", CURRENCY_FORMAT.format(paid), labelBold, valueBold);
        }

        outerCell.addElement(totals);
        outer.addCell(outerCell);
        document.add(outer);
    }

    // ==================== FOOTER ====================

    private void addFooter(Document document) throws DocumentException {
        document.add(new Paragraph(" "));

        Font footerFont = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, new BaseColor(120, 120, 120));
        Paragraph footer = new Paragraph("This is an automated financial statement from Maclures Cabs. For questions, please contact the office.", footerFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);

        Paragraph timestamp = new Paragraph("Generated on " + java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm a")), footerFont);
        timestamp.setAlignment(Element.ALIGN_CENTER);
        document.add(timestamp);
    }

    // ==================== HELPER METHODS ====================

    private List<OwnerReportDTO.RevenueLineItem> filterRevenues(OwnerReportDTO report, String subType) {
        if (report.getRevenues() == null) return List.of();
        return report.getRevenues().stream()
                .filter(r -> subType.equals(r.getRevenueSubType()))
                .collect(Collectors.toList());
    }

    private String extractCabNumber(String description) {
        if (description == null) return "Unknown";
        Matcher m = CAB_NUMBER_PATTERN.matcher(description);
        return m.find() ? m.group(1) : "Unknown";
    }

    private int parseIntSafe(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 999; }
    }

    private void addSectionTitle(Document document, String title, BaseColor color) throws DocumentException {
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, color);
        Paragraph section = new Paragraph(title, sectionFont);
        section.setSpacingBefore(8);
        document.add(section);
    }

    private void addColoredHeaders(PdfPTable table, String[] headers, BaseColor color) {
        Font whiteFont = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, BaseColor.WHITE);
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, whiteFont));
            cell.setBackgroundColor(color);
            cell.setPadding(5);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }
    }

    private PdfPCell[] createTableHeaderCells(String[] headers, Font font, BaseColor color) {
        PdfPCell[] cells = new PdfPCell[headers.length];
        for (int i = 0; i < headers.length; i++) {
            PdfPCell cell = new PdfPCell(new Phrase(headers[i], font));
            cell.setBackgroundColor(color);
            cell.setPadding(5);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cells[i] = cell;
        }
        return cells;
    }

    private void addCabHeaderRow(PdfPTable table, String cabLabel, int colspan, BaseColor bgColor, BaseColor textColor) {
        Font cabHeaderFont = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, textColor);
        PdfPCell cell = new PdfPCell(new Phrase(cabLabel, cabHeaderFont));
        cell.setColspan(colspan);
        cell.setBackgroundColor(bgColor);
        cell.setPadding(4);
        table.addCell(cell);
    }

    private void addSubtotalRow(PdfPTable table, String label, BigDecimal amount, int totalCols, BaseColor bgColor, BaseColor textColor) {
        Font subtotalFont = new Font(Font.FontFamily.HELVETICA, 7, Font.BOLD, textColor);

        PdfPCell labelCell = new PdfPCell(new Phrase(label, subtotalFont));
        labelCell.setColspan(totalCols - 1);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        labelCell.setPadding(4);
        labelCell.setBackgroundColor(bgColor);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(CURRENCY_FORMAT.format(amount), subtotalFont));
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPadding(4);
        valueCell.setBackgroundColor(bgColor);
        table.addCell(valueCell);
    }

    private void addGrandTotalRow(PdfPTable table, String label, BigDecimal amount, int totalCols, BaseColor bgColor) {
        Font totalFont = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD);

        PdfPCell labelCell = new PdfPCell(new Phrase(label, totalFont));
        labelCell.setColspan(totalCols - 1);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        labelCell.setPadding(5);
        labelCell.setBackgroundColor(bgColor);
        labelCell.setBorderWidthTop(2);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(CURRENCY_FORMAT.format(amount), totalFont));
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPadding(5);
        valueCell.setBackgroundColor(bgColor);
        valueCell.setBorderWidthTop(2);
        table.addCell(valueCell);
    }

    private void addTableCell(PdfPTable table, String text, Font font) {
        addTableCell(table, text, font, Element.ALIGN_LEFT);
    }

    private void addTableCell(PdfPTable table, String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(4);
        table.addCell(cell);
    }

    private void addTotalsRow(PdfPTable table, String label, String value, BaseColor bgColor, BaseColor textColor) {
        Font labelFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, textColor);
        Font valueFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, textColor);

        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(PdfPCell.NO_BORDER);
        labelCell.setBackgroundColor(bgColor);
        labelCell.setPadding(8);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(PdfPCell.NO_BORDER);
        valueCell.setBackgroundColor(bgColor);
        valueCell.setPadding(8);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueCell);
    }

    private void addSimpleTotalsRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(PdfPCell.NO_BORDER);
        labelCell.setPadding(4);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(PdfPCell.NO_BORDER);
        valueCell.setPadding(4);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueCell);
    }

    private void addSummaryHeader(PdfPTable table, String text, BaseColor color) {
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, color);
        PdfPCell cell = new PdfPCell(new Phrase(text, headerFont));
        cell.setColspan(2);
        cell.setBorder(PdfPCell.BOTTOM);
        cell.setBorderColor(color);
        cell.setBorderWidth(1);
        cell.setPadding(4);
        cell.setPaddingTop(8);
        table.addCell(cell);
    }

    private void addSummaryRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(PdfPCell.NO_BORDER);
        labelCell.setPadding(3);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(PdfPCell.NO_BORDER);
        valueCell.setPadding(3);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueCell);
    }

    private void addSummaryDivider(PdfPTable table) {
        PdfPCell divider = new PdfPCell();
        divider.setColspan(2);
        divider.setBorder(PdfPCell.TOP);
        divider.setBorderColor(new BaseColor(220, 220, 220));
        divider.setPadding(4);
        table.addCell(divider);
    }

    private void addTableRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setPadding(4);
        labelCell.setBorder(PdfPCell.NO_BORDER);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPadding(4);
        valueCell.setBorder(PdfPCell.NO_BORDER);
        table.addCell(valueCell);
    }
}
