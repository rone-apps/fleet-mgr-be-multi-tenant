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
import java.util.Map;

/**
 * Generates a CRA T2125 (Statement of Business or Professional Activities) worksheet PDF.
 * Maps driver/owner financial data to T2125 line numbers.
 */
@Service
@Slf4j
public class T2125PdfService {

    private static final DecimalFormat CURRENCY = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    // Colors
    private static final BaseColor CRA_RED = new BaseColor(185, 28, 28);
    private static final BaseColor HEADER_BG = new BaseColor(31, 41, 55);
    private static final BaseColor SECTION_BG = new BaseColor(243, 244, 246);
    private static final BaseColor LINE_BG = new BaseColor(249, 250, 251);
    private static final BaseColor BORDER_COLOR = new BaseColor(209, 213, 219);
    private static final BaseColor TOTAL_BG = new BaseColor(254, 243, 199);

    // Fonts
    private final Font titleFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, BaseColor.WHITE);
    private final Font subtitleFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, new BaseColor(156, 163, 175));
    private final Font sectionFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, new BaseColor(31, 41, 55));
    private final Font lineNoFont = new Font(Font.FontFamily.COURIER, 8, Font.BOLD, new BaseColor(107, 114, 128));
    private final Font labelFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);
    private final Font amountFont = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD);
    private final Font totalLabelFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
    private final Font totalAmountFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD);
    private final Font noteFont = new Font(Font.FontFamily.HELVETICA, 7, Font.ITALIC, new BaseColor(107, 114, 128));
    private final Font headerInfoFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, new BaseColor(209, 213, 219));
    private final Font headerInfoBoldFont = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, BaseColor.WHITE);

    /**
     * Generate T2125 worksheet PDF from report data.
     */
    public byte[] generateT2125Pdf(Map<String, Object> reportData) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.LETTER, 36, 36, 30, 30);
            PdfWriter.getInstance(document, baos);
            document.open();

            addContent(document, reportData);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generating T2125 PDF", e);
            throw new RuntimeException("Failed to generate T2125 PDF: " + e.getMessage(), e);
        }
    }

    private void addContent(Document document, Map<String, Object> data) throws DocumentException {
        String driverName = str(data, "driverName");
        String driverNumber = str(data, "driverNumber");
        String startDate = str(data, "startDate");
        String endDate = str(data, "endDate");
        Boolean isOwner = (Boolean) data.get("isOwner");

        // === HEADER ===
        PdfPTable header = new PdfPTable(1);
        header.setWidthPercentage(100);

        PdfPCell headerCell = new PdfPCell();
        headerCell.setBackgroundColor(HEADER_BG);
        headerCell.setPadding(18);
        headerCell.setBorder(Rectangle.NO_BORDER);

        Paragraph formTitle = new Paragraph();
        formTitle.setAlignment(Element.ALIGN_CENTER);
        Chunk t2125 = new Chunk("T2125", new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD, CRA_RED));
        Chunk rest = new Chunk("  Statement of Business or Professional Activities", titleFont);
        formTitle.add(t2125);
        formTitle.add(rest);
        headerCell.addElement(formTitle);

        Paragraph sub = new Paragraph("Worksheet — For Tax Preparation Purposes Only", subtitleFont);
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingBefore(4);
        headerCell.addElement(sub);

        header.addCell(headerCell);
        document.add(header);
        document.add(spacer(6));

        // === PART 1: IDENTIFICATION ===
        addSectionTitle(document, "Part 1 — Identification");

        PdfPTable idTable = new PdfPTable(4);
        idTable.setWidthPercentage(100);
        idTable.setWidths(new float[]{25, 25, 25, 25});

        String gstNumber = str(data, "gstNumber");

        addIdField(idTable, "Name", driverName);
        addIdField(idTable, "Driver/Owner #", driverNumber);
        addIdField(idTable, "Business Number (GST)", gstNumber.isEmpty() ? "Not on file" : gstNumber);
        addIdField(idTable, "NAICS Code", "485310 — Taxi Service");

        addIdField(idTable, "Fiscal Period", formatDateRange(startDate, endDate));
        addIdField(idTable, "Tax Year", extractYear(startDate, endDate));
        addIdField(idTable, "Type", Boolean.TRUE.equals(isOwner) ? "Owner-Operator" : "Driver");
        addIdField(idTable, "Prepared", LocalDate.now().format(DATE_FMT));

        document.add(idTable);
        document.add(spacer(10));

        // === Extract financial data ===
        @SuppressWarnings("unchecked")
        Map<String, Object> summary = data.get("summary") != null ? (Map<String, Object>) data.get("summary") : new LinkedHashMap<>();

        BigDecimal totalRevenue = toBd(summary.get("totalRevenue"));
        BigDecimal totalExpenses = toBd(summary.get("totalExpenses"));
        BigDecimal netIncome = toBd(summary.get("netIncome"));

        // Map expenses from report data
        Map<String, BigDecimal> expenseMap = extractExpenses(data);

        // === PART 3: BUSINESS INCOME ===
        addSectionTitle(document, "Part 3 — Business Income");

        PdfPTable incomeTable = createLineTable();
        addLineRow(incomeTable, "8000", "Gross business income (fares, lease income, charges)", totalRevenue, false);
        addLineRow(incomeTable, "8230", "Reserves deducted last year", BigDecimal.ZERO, false);
        addLineRow(incomeTable, "8299", "Gross business income (line 8000 + 8230)", totalRevenue, true);
        document.add(incomeTable);
        document.add(spacer(10));

        // === PART 5: BUSINESS EXPENSES ===
        addSectionTitle(document, "Part 5 — Business Expenses");

        PdfPTable expTable = createLineTable();
        BigDecimal runningTotal = BigDecimal.ZERO;

        // Map financial data to T2125 lines
        BigDecimal insurance = expenseMap.getOrDefault("insurance", BigDecimal.ZERO);
        BigDecimal leaseRent = expenseMap.getOrDefault("lease", BigDecimal.ZERO);
        BigDecimal fixedExpenses = expenseMap.getOrDefault("fixed", BigDecimal.ZERO);
        BigDecimal variableExpenses = expenseMap.getOrDefault("variable", BigDecimal.ZERO);
        BigDecimal airportCharges = expenseMap.getOrDefault("airport", BigDecimal.ZERO);
        BigDecimal taxes = expenseMap.getOrDefault("tax", BigDecimal.ZERO);
        BigDecimal commissions = expenseMap.getOrDefault("commission", BigDecimal.ZERO);
        BigDecimal otherExpenses = expenseMap.getOrDefault("other", BigDecimal.ZERO);

        addLineRow(expTable, "8690", "Insurance", insurance, false);
        runningTotal = runningTotal.add(insurance);

        addLineRow(expTable, "8760", "Business taxes, licences, dues, memberships", taxes, false);
        runningTotal = runningTotal.add(taxes);

        addLineRow(expTable, "8871", "Management and administration fees", commissions, false);
        runningTotal = runningTotal.add(commissions);

        addLineRow(expTable, "8910", "Rent / Lease payments", leaseRent, false);
        runningTotal = runningTotal.add(leaseRent);

        addLineRow(expTable, "9060", "Fixed / Recurring expenses", fixedExpenses, false);
        runningTotal = runningTotal.add(fixedExpenses);

        addLineRow(expTable, "9270", "Other expenses", otherExpenses.add(variableExpenses).add(airportCharges), false);
        runningTotal = runningTotal.add(otherExpenses).add(variableExpenses).add(airportCharges);

        // Blank common lines for accountant to fill
        addLineRow(expTable, "8521", "Advertising", null, false);
        addLineRow(expTable, "8523", "Meals and entertainment (50%)", null, false);
        addLineRow(expTable, "8710", "Interest and bank charges", null, false);
        addLineRow(expTable, "8810", "Office expenses", null, false);
        addLineRow(expTable, "8860", "Professional fees (legal, accounting)", null, false);
        addLineRow(expTable, "9180", "Telephone and utilities", null, false);
        addLineRow(expTable, "9200", "Fuel costs", null, false);

        addLineRow(expTable, "9368", "Total business expenses", totalExpenses, true);

        document.add(expTable);
        document.add(spacer(10));

        // === PART 7: MOTOR VEHICLE EXPENSES ===
        addSectionTitle(document, "Part 7 — Motor Vehicle Expenses (if applicable)");

        PdfPTable mvTable = createLineTable();
        addLineRow(mvTable, "9281", "Fuel and oil", null, false);
        addLineRow(mvTable, "9282", "Insurance", null, false);
        addLineRow(mvTable, "9283", "Licence and registration", null, false);
        addLineRow(mvTable, "9284", "Maintenance and repairs", null, false);
        addLineRow(mvTable, "9285", "Interest on motor vehicle loan", null, false);
        addLineRow(mvTable, "9286", "Leasing costs", null, false);
        addLineRow(mvTable, "9287", "Other (car washes, parking)", null, false);
        addLineRow(mvTable, "9288", "Total motor vehicle expenses", null, true);

        PdfPCell noteCell = new PdfPCell(new Phrase(
                "* Complete this section if the vehicle is also used for personal purposes. " +
                "Dedicated taxi vehicles used 100% for business can claim expenses directly in Part 5.",
                noteFont));
        noteCell.setColspan(3);
        noteCell.setBorder(Rectangle.NO_BORDER);
        noteCell.setPaddingTop(4);
        mvTable.addCell(noteCell);

        document.add(mvTable);
        document.add(spacer(10));

        // === NET INCOME ===
        addSectionTitle(document, "Net Income (Loss)");

        PdfPTable netTable = createLineTable();
        addLineRow(netTable, "8299", "Gross business income", totalRevenue, false);
        addLineRow(netTable, "9368", "Total business expenses", totalExpenses, false);
        addLineRow(netTable, "9945", "Net income (loss) before adjustments", netIncome, true);
        document.add(netTable);
        document.add(spacer(15));

        // === BREAKDOWN DETAIL ===
        addSectionTitle(document, "Appendix — Revenue & Expense Detail from System");
        addBreakdownDetail(document, data);
        document.add(spacer(10));

        // === FOOTER / DISCLAIMER ===
        Paragraph disclaimer = new Paragraph(
                "This is a tax preparation worksheet generated from Smart Fleets fleet management data. " +
                "It is NOT an official CRA form. Please review all amounts with a qualified tax professional " +
                "before filing. Some line items may need to be reclassified based on your specific tax situation. " +
                "Generated on " + LocalDate.now().format(DATE_FMT) + ".",
                noteFont);
        disclaimer.setAlignment(Element.ALIGN_CENTER);
        disclaimer.setSpacingBefore(8);
        document.add(disclaimer);
    }

    private void addBreakdownDetail(Document document, Map<String, Object> data) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{70, 30});
        table.setSpacingBefore(4);

        // Revenues
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> revenues = (java.util.List<Map<String, Object>>) data.get("revenues");
        if (revenues != null && !revenues.isEmpty()) {
            addDetailHeader(table, "Revenue Items");
            for (Map<String, Object> r : revenues) {
                addDetailRow(table, str(r, "label"), toBd(r.get("amount")));
            }
        }

        // Expenses
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> expenses = (java.util.List<Map<String, Object>>) data.get("expenses");
        if (expenses != null && !expenses.isEmpty()) {
            addDetailHeader(table, "Expense Items");
            for (Map<String, Object> e : expenses) {
                addDetailRow(table, str(e, "label"), toBd(e.get("amount")));
            }
        }

        document.add(table);
    }

    // =====================
    // Table helpers
    // =====================

    private PdfPTable createLineTable() throws DocumentException {
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{10, 60, 30});
        table.setSpacingBefore(4);
        return table;
    }

    private void addLineRow(PdfPTable table, String lineNo, String label, BigDecimal amount, boolean isTotal) {
        BaseColor bg = isTotal ? TOTAL_BG : BaseColor.WHITE;
        Font lbl = isTotal ? totalLabelFont : labelFont;
        Font amt = isTotal ? totalAmountFont : amountFont;

        PdfPCell lineCell = new PdfPCell(new Phrase(lineNo, lineNoFont));
        lineCell.setBackgroundColor(bg);
        lineCell.setBorderColor(BORDER_COLOR);
        lineCell.setPadding(5);
        lineCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(lineCell);

        PdfPCell labelCell = new PdfPCell(new Phrase(label, lbl));
        labelCell.setBackgroundColor(bg);
        labelCell.setBorderColor(BORDER_COLOR);
        labelCell.setPadding(5);
        labelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(labelCell);

        String amtStr = amount != null ? CURRENCY.format(amount) : "________";
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

    private void addDetailHeader(PdfPTable table, String title) {
        Font hFont = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, new BaseColor(55, 65, 81));
        PdfPCell cell = new PdfPCell(new Phrase(title, hFont));
        cell.setColspan(2);
        cell.setBackgroundColor(SECTION_BG);
        cell.setPadding(4);
        cell.setBorderColor(BORDER_COLOR);
        table.addCell(cell);
    }

    private void addDetailRow(PdfPTable table, String label, BigDecimal amount) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL)));
        labelCell.setBorderColor(BORDER_COLOR);
        labelCell.setPaddingLeft(12);
        labelCell.setPadding(3);
        table.addCell(labelCell);

        PdfPCell amtCell = new PdfPCell(new Phrase(CURRENCY.format(amount), new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD)));
        amtCell.setBorderColor(BORDER_COLOR);
        amtCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        amtCell.setPadding(3);
        table.addCell(amtCell);
    }

    // =====================
    // Utility
    // =====================

    private Map<String, BigDecimal> extractExpenses(Map<String, Object> data) {
        Map<String, BigDecimal> map = new LinkedHashMap<>();

        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> expenses = (java.util.List<Map<String, Object>>) data.get("expenses");
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

    private Paragraph spacer(float height) {
        Paragraph p = new Paragraph(" ", new Font(Font.FontFamily.HELVETICA, height / 2));
        return p;
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

    private String formatDateRange(String start, String end) {
        try {
            LocalDate s = LocalDate.parse(start);
            LocalDate e = LocalDate.parse(end);
            return s.format(DATE_FMT) + " — " + e.format(DATE_FMT);
        } catch (Exception ex) {
            return start + " — " + end;
        }
    }

    private String extractYear(String start, String end) {
        try {
            return String.valueOf(LocalDate.parse(start).getYear());
        } catch (Exception e) {
            return "";
        }
    }
}
