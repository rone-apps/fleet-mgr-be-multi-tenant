package com.taxi.domain.report.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates a CRA GST/HST Return worksheet PDF (based on GST34 form).
 * Maps driver/owner financial data to GST/HST return line numbers.
 */
@Service
@Slf4j
public class GstReturnPdfService {

    private static final DecimalFormat CURRENCY = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    // GST rate for British Columbia (GST only — PST is filed separately with BC)
    private static final BigDecimal GST_RATE = new BigDecimal("0.05");

    // Colors
    private static final BaseColor CRA_BLUE = new BaseColor(0, 51, 102);
    private static final BaseColor HEADER_BG = new BaseColor(0, 51, 102);
    private static final BaseColor SECTION_BG = new BaseColor(240, 244, 248);
    private static final BaseColor BORDER_COLOR = new BaseColor(200, 206, 213);
    private static final BaseColor TOTAL_BG = new BaseColor(219, 234, 254);
    private static final BaseColor REFUND_BG = new BaseColor(220, 252, 231);
    private static final BaseColor OWING_BG = new BaseColor(254, 226, 226);
    private static final BaseColor MAPLE_RED = new BaseColor(185, 28, 28);

    // Fonts
    private final Font titleFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, BaseColor.WHITE);
    private final Font subtitleFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, new BaseColor(180, 198, 216));
    private final Font sectionFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, CRA_BLUE);
    private final Font lineNoFont = new Font(Font.FontFamily.COURIER, 8, Font.BOLD, new BaseColor(107, 114, 128));
    private final Font labelFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);
    private final Font amountFont = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD);
    private final Font totalLabelFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
    private final Font totalAmountFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
    private final Font noteFont = new Font(Font.FontFamily.HELVETICA, 7, Font.ITALIC, new BaseColor(107, 114, 128));
    private final Font helpFont = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, new BaseColor(75, 85, 99));
    private final Font resultFont = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD);

    public byte[] generateGstReturnPdf(Map<String, Object> reportData) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.LETTER, 36, 36, 30, 30);
            PdfWriter.getInstance(document, baos);
            document.open();

            addContent(document, reportData);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generating GST/HST return PDF", e);
            throw new RuntimeException("Failed to generate GST/HST return PDF: " + e.getMessage(), e);
        }
    }

    private void addContent(Document document, Map<String, Object> data) throws DocumentException {
        String driverName = str(data, "driverName");
        String driverNumber = str(data, "driverNumber");
        String startDate = str(data, "startDate");
        String endDate = str(data, "endDate");
        Boolean isOwner = (Boolean) data.get("isOwner");

        @SuppressWarnings("unchecked")
        Map<String, Object> summary = data.get("summary") != null ? (Map<String, Object>) data.get("summary") : new LinkedHashMap<>();

        BigDecimal totalRevenue = toBd(summary.get("totalRevenue"));
        BigDecimal totalExpenses = toBd(summary.get("totalExpenses"));

        // Extract tax amounts from expenses
        BigDecimal taxFromExpenses = extractTaxExpense(data);

        // Calculate GST amounts
        BigDecimal gstCollected = totalRevenue.multiply(GST_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal itcTotal = calculateITCs(data, totalExpenses, taxFromExpenses);
        BigDecimal netTax = gstCollected.subtract(itcTotal);

        // === HEADER ===
        PdfPTable header = new PdfPTable(1);
        header.setWidthPercentage(100);

        PdfPCell headerCell = new PdfPCell();
        headerCell.setBackgroundColor(HEADER_BG);
        headerCell.setPadding(18);
        headerCell.setBorder(Rectangle.NO_BORDER);

        Paragraph formTitle = new Paragraph();
        formTitle.setAlignment(Element.ALIGN_CENTER);
        Chunk gst = new Chunk("GST/HST ", new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD, MAPLE_RED));
        Chunk rest = new Chunk("Return Worksheet", titleFont);
        formTitle.add(gst);
        formTitle.add(rest);
        headerCell.addElement(formTitle);

        Paragraph sub = new Paragraph("Based on CRA Form GST34 — For Tax Preparation Purposes Only", subtitleFont);
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingBefore(4);
        headerCell.addElement(sub);

        header.addCell(headerCell);
        document.add(header);
        document.add(spacer(6));

        // === IDENTIFICATION ===
        addSectionTitle(document, "Business Information");

        PdfPTable idTable = new PdfPTable(4);
        idTable.setWidthPercentage(100);
        idTable.setWidths(new float[]{25, 25, 25, 25});

        String gstNumber = str(data, "gstNumber");

        addIdField(idTable, "Business Name", driverName);
        addIdField(idTable, "Driver/Owner #", driverNumber);
        addIdField(idTable, "GST/HST Number", gstNumber.isEmpty() ? "Not on file" : gstNumber);
        addIdField(idTable, "Reporting Period", formatDateRange(startDate, endDate));

        addIdField(idTable, "Type", Boolean.TRUE.equals(isOwner) ? "Owner-Operator" : "Driver");
        addIdField(idTable, "Province", "British Columbia");
        addIdField(idTable, "GST Rate", "5%");
        addIdField(idTable, "Filing Frequency", determineFiling(startDate, endDate));

        document.add(idTable);
        document.add(spacer(10));

        // === PART 1: SALES AND OTHER REVENUE ===
        addSectionTitle(document, "Part 1 — Sales and Other Revenue");

        PdfPTable salesTable = createLineTable();
        addLineRow(salesTable, "101", "Total sales and other revenue (before GST/HST)", totalRevenue, false, null);
        addLineRow(salesTable, "", "GST/HST collected or collectible on above", null, false, "Calculated at 5% GST rate");

        document.add(salesTable);
        document.add(spacer(6));

        // Revenue breakdown
        addSubSectionTitle(document, "Revenue Breakdown");
        addRevenueBreakdown(document, data);
        document.add(spacer(10));

        // === PART 2: GST/HST COLLECTED ===
        addSectionTitle(document, "Part 2 — GST/HST Collected or Collectible");

        PdfPTable collectedTable = createLineTable();
        addLineRow(collectedTable, "103", "Total GST/HST collected on Line 101 revenue", null, false, "Enter actual GST/HST collected");
        addLineRow(collectedTable, "104", "Adjustments (bad debts recovered, etc.)", BigDecimal.ZERO, false, null);
        addLineRow(collectedTable, "105", "Total GST/HST and adjustments", gstCollected, true, "Estimated at 5% GST of revenue");

        document.add(collectedTable);
        document.add(spacer(10));

        // === PART 3: INPUT TAX CREDITS (ITCs) ===
        addSectionTitle(document, "Part 3 — Input Tax Credits (ITCs)");

        PdfPTable itcTable = createLineTable();

        // Calculate ITC breakdown
        Map<String, BigDecimal> expenseMap = extractExpensesByCategory(data);
        BigDecimal leaseITC = calcHst(expenseMap.getOrDefault("lease", BigDecimal.ZERO));
        BigDecimal insuranceITC = BigDecimal.ZERO; // Insurance is HST-exempt in Ontario
        BigDecimal fixedITC = calcHst(expenseMap.getOrDefault("fixed", BigDecimal.ZERO));
        BigDecimal variableITC = calcHst(expenseMap.getOrDefault("variable", BigDecimal.ZERO));
        BigDecimal airportITC = calcHst(expenseMap.getOrDefault("airport", BigDecimal.ZERO));
        BigDecimal otherITC = calcHst(expenseMap.getOrDefault("other", BigDecimal.ZERO));

        addLineRow(itcTable, "", "Lease / Rent payments — ITC", leaseITC, false, "HST on " + fmtCurrency(expenseMap.getOrDefault("lease", BigDecimal.ZERO)));
        addLineRow(itcTable, "", "Insurance — ITC", insuranceITC, false, "Insurance is GST-exempt");
        addLineRow(itcTable, "", "Fixed / Recurring expenses — ITC", fixedITC, false, "HST on " + fmtCurrency(expenseMap.getOrDefault("fixed", BigDecimal.ZERO)));
        addLineRow(itcTable, "", "Variable / One-time expenses — ITC", variableITC, false, "HST on " + fmtCurrency(expenseMap.getOrDefault("variable", BigDecimal.ZERO)));
        addLineRow(itcTable, "", "Airport charges — ITC", airportITC, false, "HST on " + fmtCurrency(expenseMap.getOrDefault("airport", BigDecimal.ZERO)));
        addLineRow(itcTable, "", "Other expenses — ITC", otherITC, false, "HST on " + fmtCurrency(expenseMap.getOrDefault("other", BigDecimal.ZERO)));

        addLineRow(itcTable, "106", "Total ITCs", itcTotal, true, null);
        addLineRow(itcTable, "107", "Adjustments", BigDecimal.ZERO, false, null);
        addLineRow(itcTable, "108", "Total ITCs and adjustments", itcTotal, true, null);

        document.add(itcTable);
        document.add(spacer(10));

        // === PART 4: NET TAX CALCULATION ===
        addSectionTitle(document, "Part 4 — Net Tax Calculation");

        PdfPTable netTable = createLineTable();
        addLineRow(netTable, "105", "Total GST/HST collected", gstCollected, false, null);
        addLineRow(netTable, "108", "Total ITCs claimed", itcTotal, false, null);
        addLineRow(netTable, "109", "Instalments paid", BigDecimal.ZERO, false, "Enter if applicable");

        boolean isRefund = netTax.compareTo(BigDecimal.ZERO) < 0;
        document.add(netTable);
        document.add(spacer(4));

        // Result box
        PdfPTable resultBox = new PdfPTable(1);
        resultBox.setWidthPercentage(100);
        PdfPCell resultCell = new PdfPCell();
        resultCell.setBackgroundColor(isRefund ? REFUND_BG : OWING_BG);
        resultCell.setBorderColor(isRefund ? new BaseColor(34, 197, 94) : new BaseColor(239, 68, 68));
        resultCell.setPadding(15);

        String resultLine = isRefund ? "113" : "112";
        String resultLabel = isRefund ? "Refund claimed" : "Balance owing";
        BigDecimal resultAmount = netTax.abs();

        Paragraph resultP = new Paragraph();
        resultP.setAlignment(Element.ALIGN_CENTER);
        resultP.add(new Chunk("Line " + resultLine + ": ", lineNoFont));
        resultP.add(new Chunk(resultLabel + "  ", totalLabelFont));
        Font rFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, isRefund ? new BaseColor(22, 163, 74) : MAPLE_RED);
        resultP.add(new Chunk("$" + CURRENCY.format(resultAmount), rFont));
        resultCell.addElement(resultP);

        Paragraph calcP = new Paragraph("($" + CURRENCY.format(gstCollected) + " collected  −  $" + CURRENCY.format(itcTotal) + " ITCs  =  $" + CURRENCY.format(netTax) + ")", helpFont);
        calcP.setAlignment(Element.ALIGN_CENTER);
        calcP.setSpacingBefore(4);
        resultCell.addElement(calcP);

        resultBox.addCell(resultCell);
        document.add(resultBox);
        document.add(spacer(12));

        // === QUICK REFERENCE ===
        addSectionTitle(document, "Quick Reference — GST/HST Rates");
        PdfPTable refTable = new PdfPTable(3);
        refTable.setWidthPercentage(100);
        refTable.setWidths(new float[]{40, 30, 30});
        refTable.setSpacingBefore(4);

        addRefHeader(refTable, "Item", "GST Applicable?", "Notes");
        addRefRow(refTable, "Taxi fares", "Yes — 5%", "GST collected from passengers");
        addRefRow(refTable, "Lease/rental payments", "Yes — 5%", "ITC claimable");
        addRefRow(refTable, "Vehicle insurance", "No", "GST-exempt");
        addRefRow(refTable, "Fuel", "Yes — 5%", "ITC claimable (PST filed separately with BC)");
        addRefRow(refTable, "Repairs & maintenance", "Yes — 5%", "ITC claimable");
        addRefRow(refTable, "Licence & registration", "No", "Government fees are exempt");
        addRefRow(refTable, "Dispatch / radio fees", "Yes — 5%", "ITC claimable");
        addRefRow(refTable, "Airport charges", "Yes — 5%", "ITC claimable");
        addRefRow(refTable, "Accounting / legal fees", "Yes — 5%", "ITC claimable");

        document.add(refTable);
        document.add(spacer(12));

        // === DISCLAIMER ===
        Paragraph disclaimer = new Paragraph(
                "This is a GST/HST return preparation worksheet generated from Smart Fleets fleet management data. " +
                "It is NOT an official CRA form. GST amounts are estimated at 5% (British Columbia). PST (7%) is filed separately with BC. " +
                "Actual GST collected and ITCs may differ — verify all amounts with receipts and a qualified tax professional before filing. " +
                "Insurance is shown as GST-exempt. " +
                "Generated on " + LocalDate.now().format(DATE_FMT) + ".",
                noteFont);
        disclaimer.setAlignment(Element.ALIGN_CENTER);
        document.add(disclaimer);
    }

    private void addRevenueBreakdown(Document document, Map<String, Object> data) throws DocumentException {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> revenues = (List<Map<String, Object>>) data.get("revenues");
        if (revenues == null || revenues.isEmpty()) return;

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{50, 25, 25});
        table.setSpacingBefore(4);

        // Header
        addSmallHeader(table, "Revenue Source");
        addSmallHeader(table, "Amount (excl. GST)");
        addSmallHeader(table, "Est. GST Collected");

        for (Map<String, Object> r : revenues) {
            BigDecimal amt = toBd(r.get("amount"));
            BigDecimal gst = amt.multiply(GST_RATE).setScale(2, RoundingMode.HALF_UP);

            addSmallCell(table, str(r, "label"), false);
            addSmallCellRight(table, fmtCurrency(amt));
            addSmallCellRight(table, fmtCurrency(gst));
        }

        document.add(table);
    }

    // =====================
    // Table helpers
    // =====================

    private PdfPTable createLineTable() throws DocumentException {
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{8, 57, 35});
        table.setSpacingBefore(4);
        return table;
    }

    private void addLineRow(PdfPTable table, String lineNo, String label, BigDecimal amount, boolean isTotal, String help) {
        BaseColor bg = isTotal ? TOTAL_BG : BaseColor.WHITE;
        Font lbl = isTotal ? totalLabelFont : labelFont;
        Font amt = isTotal ? totalAmountFont : amountFont;

        PdfPCell lineCell = new PdfPCell(new Phrase(lineNo, lineNoFont));
        lineCell.setBackgroundColor(bg);
        lineCell.setBorderColor(BORDER_COLOR);
        lineCell.setPadding(5);
        lineCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(lineCell);

        // Label with optional help text
        Paragraph labelP = new Paragraph(label, lbl);
        if (help != null) {
            labelP.add(new Chunk("\n" + help, helpFont));
        }
        PdfPCell labelCell = new PdfPCell(labelP);
        labelCell.setBackgroundColor(bg);
        labelCell.setBorderColor(BORDER_COLOR);
        labelCell.setPadding(5);
        labelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(labelCell);

        String amtStr = amount != null ? "$" + CURRENCY.format(amount) : "________";
        PdfPCell amtCell = new PdfPCell(new Phrase(amtStr, amount != null ? amt : lineNoFont));
        amtCell.setBackgroundColor(bg);
        amtCell.setBorderColor(BORDER_COLOR);
        amtCell.setPadding(5);
        amtCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        amtCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(amtCell);
    }

    private void addSectionTitle(Document document, String title) throws DocumentException {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingBefore(4);
        PdfPCell cell = new PdfPCell(new Phrase(title, sectionFont));
        cell.setBackgroundColor(SECTION_BG);
        cell.setPadding(8);
        cell.setBorderColor(BORDER_COLOR);
        table.addCell(cell);
        document.add(table);
    }

    private void addSubSectionTitle(Document document, String title) throws DocumentException {
        Font f = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, new BaseColor(75, 85, 99));
        Paragraph p = new Paragraph(title, f);
        p.setSpacingBefore(4);
        document.add(p);
    }

    private void addIdField(PdfPTable table, String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(BORDER_COLOR);
        cell.setPadding(6);
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + ": ", new Font(Font.FontFamily.HELVETICA, 7, Font.NORMAL, new BaseColor(107, 114, 128))));
        p.add(new Chunk(value != null ? value : "", new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD)));
        cell.addElement(p);
        table.addCell(cell);
    }

    private void addSmallHeader(PdfPTable table, String text) {
        Font f = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, new BaseColor(55, 65, 81));
        PdfPCell cell = new PdfPCell(new Phrase(text, f));
        cell.setBackgroundColor(SECTION_BG);
        cell.setBorderColor(BORDER_COLOR);
        cell.setPadding(4);
        table.addCell(cell);
    }

    private void addSmallCell(PdfPTable table, String text, boolean bold) {
        Font f = bold ? new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD) : new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL);
        PdfPCell cell = new PdfPCell(new Phrase(text, f));
        cell.setBorderColor(BORDER_COLOR);
        cell.setPadding(3);
        cell.setPaddingLeft(8);
        table.addCell(cell);
    }

    private void addSmallCellRight(PdfPTable table, String text) {
        Font f = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD);
        PdfPCell cell = new PdfPCell(new Phrase(text, f));
        cell.setBorderColor(BORDER_COLOR);
        cell.setPadding(3);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(cell);
    }

    private void addRefHeader(PdfPTable table, String... headers) {
        Font f = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, BaseColor.WHITE);
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, f));
            cell.setBackgroundColor(CRA_BLUE);
            cell.setBorderColor(BORDER_COLOR);
            cell.setPadding(5);
            table.addCell(cell);
        }
    }

    private void addRefRow(PdfPTable table, String item, String applicable, String notes) {
        Font f = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL);
        for (String val : new String[]{item, applicable, notes}) {
            PdfPCell cell = new PdfPCell(new Phrase(val, f));
            cell.setBorderColor(BORDER_COLOR);
            cell.setPadding(4);
            table.addCell(cell);
        }
    }

    // =====================
    // Calculation helpers
    // =====================

    private BigDecimal calcHst(BigDecimal expenseAmount) {
        // Calculate HST portion: amount × 13/113 (extract HST from HST-inclusive amount)
        // Or if expenses are pre-HST: amount × 0.13
        // Assuming expenses in system are the actual amounts paid (HST-inclusive)
        return expenseAmount.multiply(new BigDecimal("5")).divide(new BigDecimal("105"), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateITCs(Map<String, Object> data, BigDecimal totalExpenses, BigDecimal taxFromExpenses) {
        Map<String, BigDecimal> expenses = extractExpensesByCategory(data);

        BigDecimal itcEligible = BigDecimal.ZERO;

        // Lease — HST claimable
        itcEligible = itcEligible.add(expenses.getOrDefault("lease", BigDecimal.ZERO));
        // Fixed/recurring — HST claimable
        itcEligible = itcEligible.add(expenses.getOrDefault("fixed", BigDecimal.ZERO));
        // Variable — HST claimable
        itcEligible = itcEligible.add(expenses.getOrDefault("variable", BigDecimal.ZERO));
        // Airport — HST claimable
        itcEligible = itcEligible.add(expenses.getOrDefault("airport", BigDecimal.ZERO));
        // Other — HST claimable
        itcEligible = itcEligible.add(expenses.getOrDefault("other", BigDecimal.ZERO));
        // Insurance is NOT ITC-eligible (exempt)
        // Tax expense itself is not ITC-eligible
        // Commission — HST claimable
        itcEligible = itcEligible.add(expenses.getOrDefault("commission", BigDecimal.ZERO));

        // Extract HST from ITC-eligible expenses (13/113 for HST-inclusive amounts)
        return itcEligible.multiply(new BigDecimal("5")).divide(new BigDecimal("105"), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal extractTaxExpense(Map<String, Object> data) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> expenses = (List<Map<String, Object>>) data.get("expenses");
        if (expenses == null) return BigDecimal.ZERO;

        BigDecimal tax = BigDecimal.ZERO;
        for (Map<String, Object> exp : expenses) {
            String label = str(exp, "label").toLowerCase();
            if (label.contains("tax") || label.contains("hst") || label.contains("gst")) {
                tax = tax.add(toBd(exp.get("amount")));
            }
        }
        return tax;
    }

    private Map<String, BigDecimal> extractExpensesByCategory(Map<String, Object> data) {
        Map<String, BigDecimal> map = new LinkedHashMap<>();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> expenses = (List<Map<String, Object>>) data.get("expenses");
        if (expenses == null) return map;

        for (Map<String, Object> exp : expenses) {
            String label = str(exp, "label").toLowerCase();
            BigDecimal amount = toBd(exp.get("amount"));

            if (label.contains("insurance") || label.contains("mileage")) {
                map.merge("insurance", amount, BigDecimal::add);
            } else if (label.contains("lease")) {
                map.merge("lease", amount, BigDecimal::add);
            } else if (label.contains("fixed") || label.contains("recurring")) {
                map.merge("fixed", amount, BigDecimal::add);
            } else if (label.contains("variable") || label.contains("one-time")) {
                map.merge("variable", amount, BigDecimal::add);
            } else if (label.contains("airport")) {
                map.merge("airport", amount, BigDecimal::add);
            } else if (label.contains("tax") || label.contains("hst") || label.contains("gst")) {
                map.merge("tax", amount, BigDecimal::add);
            } else if (label.contains("commission")) {
                map.merge("commission", amount, BigDecimal::add);
            } else {
                map.merge("other", amount, BigDecimal::add);
            }
        }
        return map;
    }

    private String determineFiling(String startDate, String endDate) {
        try {
            LocalDate s = LocalDate.parse(startDate);
            LocalDate e = LocalDate.parse(endDate);
            long months = java.time.temporal.ChronoUnit.MONTHS.between(s, e) + 1;
            if (months <= 1) return "Monthly";
            if (months <= 3) return "Quarterly";
            return "Annual";
        } catch (Exception ex) {
            return "Annual";
        }
    }

    // =====================
    // Utility
    // =====================

    private Paragraph spacer(float height) {
        return new Paragraph(" ", new Font(Font.FontFamily.HELVETICA, height / 2));
    }

    private BigDecimal toBd(Object val) {
        if (val == null) return BigDecimal.ZERO;
        if (val instanceof BigDecimal) return (BigDecimal) val;
        try { return new BigDecimal(val.toString()); } catch (Exception e) { return BigDecimal.ZERO; }
    }

    private String str(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : "";
    }

    private String fmtCurrency(BigDecimal val) {
        return "$" + CURRENCY.format(val);
    }

    private String formatDateRange(String start, String end) {
        try {
            LocalDate s = LocalDate.parse(start);
            LocalDate e = LocalDate.parse(end);
            return s.format(DATE_FMT) + " — " + e.format(DATE_FMT);
        } catch (Exception ex) {
            return start + " — " + end;
        }
    }
}
