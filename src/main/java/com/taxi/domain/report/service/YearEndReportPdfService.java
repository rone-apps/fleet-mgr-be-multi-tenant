package com.taxi.domain.report.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class YearEndReportPdfService {

    private static final DecimalFormat CURRENCY = new DecimalFormat("$#,##0.00");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    private static final BaseColor HEADER_BG = new BaseColor(62, 82, 68);       // #3e5244
    private static final BaseColor GREEN = new BaseColor(67, 160, 71);           // #43a047
    private static final BaseColor GREEN_BG = new BaseColor(232, 245, 233);      // #e8f5e9
    private static final BaseColor RED = new BaseColor(229, 57, 53);             // #e53935
    private static final BaseColor RED_BG = new BaseColor(255, 235, 238);        // #ffebee
    private static final BaseColor BLUE = new BaseColor(30, 136, 229);           // #1e88e5
    private static final BaseColor BLUE_BG = new BaseColor(227, 242, 253);       // #e3f2fd
    private static final BaseColor GRAY_BG = new BaseColor(248, 250, 251);       // #f8fafb

    private final Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, BaseColor.WHITE);
    private final Font subtitleFont = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, new BaseColor(168, 213, 186));
    private final Font driverFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, BaseColor.WHITE);
    private final Font sectionFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD);
    private final Font labelFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);
    private final Font amountFont = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD);
    private final Font summaryLabelFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
    private final Font summaryAmountFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD);
    private final Font footerFont = new Font(Font.FontFamily.HELVETICA, 7, Font.NORMAL, new BaseColor(150, 150, 150));

    /**
     * Generate PDF for a single driver report.
     */
    public byte[] generatePdf(Map<String, Object> reportData) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.LETTER, 40, 40, 30, 30);
            PdfWriter.getInstance(document, baos);
            document.open();

            addDriverReport(document, reportData);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generating year-end report PDF", e);
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Generate PDF containing multiple driver reports (one per page).
     */
    public byte[] generateAllPdf(Map<String, Object> allReportsData) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.LETTER, 40, 40, 30, 30);
            PdfWriter.getInstance(document, baos);
            document.open();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> reports = (List<Map<String, Object>>) allReportsData.get("reports");
            if (reports != null) {
                for (int i = 0; i < reports.size(); i++) {
                    if (i > 0) document.newPage();
                    addDriverReport(document, reports.get(i));
                }
            }

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generating all-drivers year-end report PDF", e);
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage(), e);
        }
    }

    private void addDriverReport(Document document, Map<String, Object> data) throws DocumentException {
        String driverName = str(data, "driverName");
        String driverNumber = str(data, "driverNumber");
        String startDate = str(data, "startDate");
        String endDate = str(data, "endDate");
        Boolean isOwner = (Boolean) data.get("isOwner");

        // Header
        PdfPTable header = new PdfPTable(1);
        header.setWidthPercentage(100);
        PdfPCell headerCell = new PdfPCell();
        headerCell.setBackgroundColor(HEADER_BG);
        headerCell.setPadding(20);
        headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        headerCell.setBorder(Rectangle.NO_BORDER);

        Paragraph title = new Paragraph("Financial Report", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        headerCell.addElement(title);

        String dateRange = formatDateRange(startDate, endDate);
        Paragraph period = new Paragraph(dateRange, subtitleFont);
        period.setAlignment(Element.ALIGN_CENTER);
        period.setSpacingBefore(4);
        headerCell.addElement(period);

        String driverLine = driverName + " (" + driverNumber + ")";
        if (Boolean.TRUE.equals(isOwner)) driverLine += "  [Owner]";
        Paragraph driverP = new Paragraph(driverLine, driverFont);
        driverP.setAlignment(Element.ALIGN_CENTER);
        driverP.setSpacingBefore(8);
        headerCell.addElement(driverP);

        header.addCell(headerCell);
        document.add(header);
        document.add(new Paragraph(" ", new Font(Font.FontFamily.HELVETICA, 6)));

        // Revenue
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> revenues = (List<Map<String, Object>>) data.get("revenues");
        if (revenues != null && !revenues.isEmpty()) {
            addSection(document, "Revenue", GREEN, GREEN_BG, revenues, GREEN);
        }

        // Expenses
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> expenses = (List<Map<String, Object>>) data.get("expenses");
        if (expenses != null && !expenses.isEmpty()) {
            addSection(document, "Expenses", RED, RED_BG, expenses, RED);
        }

        // Summary
        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) data.get("summary");
        if (summary != null && !summary.isEmpty()) {
            addSummarySection(document, summary);
        }

        // Footer
        Paragraph footer = new Paragraph("Generated on " + LocalDate.now().format(DATE_FMT) + "  |  FareFlow", footerFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(15);
        document.add(footer);
    }

    private void addSection(Document document, String title, BaseColor titleColor, BaseColor bgColor,
                            List<Map<String, Object>> items, BaseColor amountColor) throws DocumentException {
        // Section header
        Font sFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, titleColor);
        Paragraph sectionTitle = new Paragraph(title, sFont);
        sectionTitle.setSpacingBefore(8);
        document.add(sectionTitle);

        // Divider
        PdfPTable divider = new PdfPTable(1);
        divider.setWidthPercentage(100);
        PdfPCell divCell = new PdfPCell();
        divCell.setFixedHeight(2);
        divCell.setBackgroundColor(bgColor);
        divCell.setBorder(Rectangle.NO_BORDER);
        divider.addCell(divCell);
        document.add(divider);

        // Items table
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{75, 25});
        table.setSpacingBefore(4);

        Font amtFont = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, amountColor);

        for (Map<String, Object> item : items) {
            PdfPCell labelCell = new PdfPCell(new Phrase(str(item, "label"), labelFont));
            labelCell.setBorder(Rectangle.BOTTOM);
            labelCell.setBorderColor(new BaseColor(230, 230, 230));
            labelCell.setPaddingLeft(15);
            labelCell.setPaddingTop(5);
            labelCell.setPaddingBottom(5);
            table.addCell(labelCell);

            PdfPCell amtCell = new PdfPCell(new Phrase(fmtAmount(item.get("amount")), amtFont));
            amtCell.setBorder(Rectangle.BOTTOM);
            amtCell.setBorderColor(new BaseColor(230, 230, 230));
            amtCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            amtCell.setPaddingTop(5);
            amtCell.setPaddingBottom(5);
            table.addCell(amtCell);
        }

        document.add(table);
    }

    private void addSummarySection(Document document, Map<String, Object> summary) throws DocumentException {
        Font sFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, BLUE);
        Paragraph sectionTitle = new Paragraph("Summary", sFont);
        sectionTitle.setSpacingBefore(12);
        document.add(sectionTitle);

        PdfPTable box = new PdfPTable(2);
        box.setWidthPercentage(100);
        box.setWidths(new float[]{65, 35});
        box.setSpacingBefore(4);

        if (summary.containsKey("totalRevenue")) {
            addSummaryRow(box, "Total Revenue", summary.get("totalRevenue"), GREEN, false);
        }
        if (summary.containsKey("totalExpenses")) {
            addSummaryRow(box, "Total Expenses", summary.get("totalExpenses"), RED, false);
        }
        if (summary.containsKey("netIncome")) {
            BigDecimal net = toBd(summary.get("netIncome"));
            BaseColor color = net.compareTo(BigDecimal.ZERO) >= 0 ? GREEN : RED;
            addSummaryRow(box, "Net Income", summary.get("netIncome"), color, true);
        }
        if (summary.containsKey("previousBalance") && toBd(summary.get("previousBalance")).compareTo(BigDecimal.ZERO) != 0) {
            addSummaryRow(box, "Previous Balance", summary.get("previousBalance"), BaseColor.DARK_GRAY, false);
        }
        if (summary.containsKey("paymentsMade") && toBd(summary.get("paymentsMade")).compareTo(BigDecimal.ZERO) != 0) {
            addSummaryRow(box, "Payments Made", summary.get("paymentsMade"), BaseColor.DARK_GRAY, false);
        }
        if (summary.containsKey("outstanding")) {
            BigDecimal out = toBd(summary.get("outstanding"));
            BaseColor color = out.compareTo(BigDecimal.ZERO) > 0 ? RED : GREEN;
            addSummaryRow(box, "Outstanding Balance", summary.get("outstanding"), color, true);
        }

        // Wrap in a bordered box
        PdfPTable wrapper = new PdfPTable(1);
        wrapper.setWidthPercentage(100);
        PdfPCell wrapCell = new PdfPCell(box);
        wrapCell.setBackgroundColor(GRAY_BG);
        wrapCell.setBorderColor(new BaseColor(229, 231, 235));
        wrapCell.setPadding(10);
        wrapper.addCell(wrapCell);
        document.add(wrapper);
    }

    private void addSummaryRow(PdfPTable table, String label, Object amount, BaseColor amountColor, boolean bold) {
        Font lFont = bold ? summaryLabelFont : new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);
        Font aFont = bold ? new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, amountColor)
                         : new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, amountColor);

        PdfPCell labelCell = new PdfPCell(new Phrase(label, lFont));
        labelCell.setBorder(bold ? Rectangle.TOP : Rectangle.NO_BORDER);
        labelCell.setBorderColor(BLUE);
        labelCell.setPaddingTop(4);
        labelCell.setPaddingBottom(4);
        table.addCell(labelCell);

        PdfPCell amtCell = new PdfPCell(new Phrase(fmtAmount(amount), aFont));
        amtCell.setBorder(bold ? Rectangle.TOP : Rectangle.NO_BORDER);
        amtCell.setBorderColor(BLUE);
        amtCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        amtCell.setPaddingTop(4);
        amtCell.setPaddingBottom(4);
        table.addCell(amtCell);
    }

    private String fmtAmount(Object val) {
        BigDecimal bd = toBd(val);
        if (bd.compareTo(BigDecimal.ZERO) < 0) {
            return "-" + CURRENCY.format(bd.abs());
        }
        return CURRENCY.format(bd);
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
}
