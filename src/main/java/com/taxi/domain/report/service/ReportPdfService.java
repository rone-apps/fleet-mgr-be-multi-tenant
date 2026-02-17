package com.taxi.domain.report.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPCell;
import com.taxi.web.dto.expense.OwnerReportDTO;
import com.taxi.web.dto.expense.StatementLineItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for generating professional PDF reports for drivers/owners
 */
@Service
@Slf4j
public class ReportPdfService {

    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("$#,##0.00");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    /**
     * Generate professional PDF report from OwnerReportDTO matching the modal detail view
     */
    public byte[] generateDriverReportPdf(OwnerReportDTO report) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.LETTER, 40, 40, 40, 40);
            PdfWriter.getInstance(document, baos);
            document.open();

            // Header (Owner name, period, status)
            addReportHeader(document, report);

            // Revenues Section organized by type
            if (report.getRevenues() != null && !report.getRevenues().isEmpty()) {
                // Lease Revenues
                java.util.List<OwnerReportDTO.RevenueLineItem> leaseRevenues = report.getRevenues().stream()
                        .filter(r -> "LEASE_INCOME".equals(r.getRevenueSubType()))
                        .toList();
                if (!leaseRevenues.isEmpty()) {
                    addLeaseRevenuesSection(document, leaseRevenues);
                    document.add(new Paragraph(" "));
                }

                // Account Charge Revenues
                java.util.List<OwnerReportDTO.RevenueLineItem> accountRevenues = report.getRevenues().stream()
                        .filter(r -> "ACCOUNT_REVENUE".equals(r.getRevenueSubType()))
                        .toList();
                if (!accountRevenues.isEmpty()) {
                    addAccountChargeRevenuesSection(document, accountRevenues);
                    document.add(new Paragraph(" "));
                }

                // Credit Card Revenues
                java.util.List<OwnerReportDTO.RevenueLineItem> creditCardRevenues = report.getRevenues().stream()
                        .filter(r -> "CREDIT_CARD_REVENUE".equals(r.getRevenueSubType()))
                        .toList();
                if (!creditCardRevenues.isEmpty()) {
                    addCreditCardRevenuesSection(document, creditCardRevenues);
                    document.add(new Paragraph(" "));
                }

                // Other Revenues
                java.util.List<OwnerReportDTO.RevenueLineItem> otherRevenues = report.getRevenues().stream()
                        .filter(r -> "OTHER_REVENUE".equals(r.getRevenueSubType()))
                        .toList();
                if (!otherRevenues.isEmpty()) {
                    addOtherRevenuesSection(document, otherRevenues);
                    document.add(new Paragraph(" "));
                }
            }

            // Recurring Expenses Section (if any)
            if (report.getRecurringExpenses() != null && !report.getRecurringExpenses().isEmpty()) {
                addRecurringExpensesSection(document, report);
                document.add(new Paragraph(" "));
            }

            // Lease Expenses Section (if any)
            java.util.List<StatementLineItem> leaseExpenses = report.getOneTimeExpenses().stream()
                    .filter(exp -> "LEASE_RENT".equals(exp.getApplicationType()))
                    .toList();
            if (!leaseExpenses.isEmpty()) {
                addLeaseExpensesSection(document, report, leaseExpenses);
                document.add(new Paragraph(" "));
            }

            // One-Time Expenses Section (non-lease, if any)
            java.util.List<StatementLineItem> otherOneTime = report.getOneTimeExpenses().stream()
                    .filter(exp -> !"LEASE_RENT".equals(exp.getApplicationType()))
                    .toList();
            if (!otherOneTime.isEmpty()) {
                addOneTimeExpensesSection(document, report, otherOneTime);
                document.add(new Paragraph(" "));
            }

            // Totals Footer (matching modal)
            addTotalsSectionFooter(document, report);

            // Footer
            document.add(new Paragraph(" "));
            addFooter(document);

            document.close();
            return baos.toByteArray();

        } catch (DocumentException e) {
            log.error("Error generating PDF report", e);
            throw new RuntimeException("Failed to generate PDF report: " + e.getMessage(), e);
        }
    }

    private void addReportHeader(Document document, OwnerReportDTO report) throws DocumentException {
        // Header background box with gradient effect
        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setWidthPercentage(100);
        headerTable.getDefaultCell().setBorder(PdfPCell.NO_BORDER);
        headerTable.getDefaultCell().setBackgroundColor(new BaseColor(44, 62, 80)); // Dark blue-gray
        headerTable.getDefaultCell().setPadding(15);

        // Title
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD, BaseColor.WHITE);
        Paragraph title = new Paragraph("📊 Financial Statement", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);

        // Company Name
        Font companyFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, BaseColor.WHITE);
        Paragraph company = new Paragraph("Maclures Cabs", companyFont);
        company.setAlignment(Element.ALIGN_CENTER);

        PdfPCell cell = new PdfPCell();
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setBackgroundColor(new BaseColor(44, 62, 80));
        cell.addElement(title);
        cell.addElement(company);
        cell.setPadding(0);
        headerTable.addCell(cell);
        document.add(headerTable);

        // Driver info section
        Font nameFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, new BaseColor(44, 62, 80));
        Paragraph name = new Paragraph(report.getOwnerName(), nameFont);
        name.setAlignment(Element.ALIGN_CENTER);
        name.setSpacingBefore(15);
        document.add(name);

        Font periodFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, new BaseColor(100, 100, 100));
        String period = report.getPeriodFrom().format(DATE_FORMAT) + " to " + report.getPeriodTo().format(DATE_FORMAT);
        Paragraph periodPara = new Paragraph(period, periodFont);
        periodPara.setAlignment(Element.ALIGN_CENTER);
        document.add(periodPara);

        String generatedDate = "Generated: " + java.time.LocalDate.now().format(DATE_FORMAT);
        Paragraph datePara = new Paragraph(generatedDate, periodFont);
        datePara.setAlignment(Element.ALIGN_CENTER);
        document.add(datePara);

        document.add(new Paragraph(" "));
    }

    private void addRecurringExpensesSection(Document document, OwnerReportDTO report) throws DocumentException {
        // Recurring Expenses - Dark Orange (#e65100)
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, new BaseColor(230, 81, 0));
        Paragraph section = new Paragraph("Recurring Expenses", sectionFont);
        section.setSpacingBefore(10);
        document.add(section);

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setSpacingBefore(5);
        table.setSpacingAfter(10);

        Font headerFont = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD);
        PdfPCell[] headerCells = createTableHeaderCells(new String[]{"Category", "Target", "Amount"}, headerFont, new BaseColor(230, 81, 0));
        for (PdfPCell cell : headerCells) {
            table.addCell(cell);
        }

        Font cellFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);
        for (StatementLineItem expense : report.getRecurringExpenses()) {
            addTableCell(table, expense.getCategoryName() != null ? expense.getCategoryName() : "", cellFont);
            addTableCell(table, expense.getEntityDescription() != null ? expense.getEntityDescription() : "", cellFont);
            addTableCell(table, CURRENCY_FORMAT.format(expense.getAmount()), cellFont, Element.ALIGN_RIGHT);
        }

        document.add(table);
    }

    private void addLeaseExpensesSection(Document document, OwnerReportDTO report, java.util.List<StatementLineItem> leaseExpenses) throws DocumentException {
        // Lease Expenses - Yellow-Orange (#f57f17)
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, new BaseColor(245, 127, 23));
        Paragraph section = new Paragraph("Lease Expenses", sectionFont);
        section.setSpacingBefore(10);
        document.add(section);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingBefore(5);
        table.setSpacingAfter(10);

        Font headerFont = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD);
        PdfPCell[] headerCells = createTableHeaderCells(new String[]{"Date", "Description", "Rate Breakdown", "Amount"}, headerFont, new BaseColor(245, 127, 23));
        for (PdfPCell cell : headerCells) {
            table.addCell(cell);
        }

        Font cellFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);
        for (StatementLineItem expense : leaseExpenses) {
            addTableCell(table, expense.getDate() != null ? expense.getDate().format(DATE_FORMAT) : "", cellFont);
            addTableCell(table, expense.getDescription() != null ? expense.getDescription() : "", cellFont);
            addTableCell(table, expense.getEntityDescription() != null ? expense.getEntityDescription() : "", cellFont);
            addTableCell(table, CURRENCY_FORMAT.format(expense.getAmount()), cellFont, Element.ALIGN_RIGHT);
        }

        document.add(table);
    }

    private void addOneTimeExpensesSection(Document document, OwnerReportDTO report, java.util.List<StatementLineItem> oneTimeExpenses) throws DocumentException {
        // One-Time Expenses - Pink (#c2185b)
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, new BaseColor(194, 24, 91));
        Paragraph section = new Paragraph("One-Time Expenses", sectionFont);
        section.setSpacingBefore(10);
        document.add(section);

        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setSpacingBefore(5);
        table.setSpacingAfter(10);

        Font headerFont = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD);
        PdfPCell[] headerCells = createTableHeaderCells(new String[]{"Date", "Category", "Description", "Cab #", "Shift Type", "Details", "Amount"}, headerFont, new BaseColor(194, 24, 91));
        for (PdfPCell cell : headerCells) {
            table.addCell(cell);
        }

        Font cellFont = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL);
        for (StatementLineItem expense : oneTimeExpenses) {
            addTableCell(table, expense.getDate() != null ? expense.getDate().format(DATE_FORMAT) : "", cellFont);
            addTableCell(table, expense.getCategoryName() != null ? expense.getCategoryName() : "", cellFont);
            addTableCell(table, expense.getDescription() != null ? expense.getDescription() : "", cellFont);
            addTableCell(table, expense.getCabNumber() != null ? expense.getCabNumber() : "", cellFont);
            addTableCell(table, expense.getShiftType() != null ? expense.getShiftType() : "", cellFont);
            addTableCell(table, expense.getChargeTarget() != null ? expense.getChargeTarget() : "", cellFont);
            addTableCell(table, CURRENCY_FORMAT.format(expense.getAmount()), cellFont, Element.ALIGN_RIGHT);
        }

        document.add(table);
    }

    private void addLeaseRevenuesSection(Document document, java.util.List<OwnerReportDTO.RevenueLineItem> leaseRevenues) throws DocumentException {
        // Lease Revenue - Green (#388e3c)
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, new BaseColor(56, 142, 60));
        Paragraph section = new Paragraph("Lease Revenue", sectionFont);
        section.setSpacingBefore(10);
        document.add(section);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingBefore(5);
        table.setSpacingAfter(10);

        Font headerFont = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD);
        PdfPCell[] headerCells = createTableHeaderCells(new String[]{"Date", "Category", "Description", "Amount"}, headerFont, new BaseColor(56, 142, 60));
        for (PdfPCell cell : headerCells) {
            table.addCell(cell);
        }

        Font cellFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);
        for (OwnerReportDTO.RevenueLineItem revenue : leaseRevenues) {
            addTableCell(table, revenue.getRevenueDate().format(DATE_FORMAT), cellFont);
            addTableCell(table, revenue.getCategoryName() != null ? revenue.getCategoryName() : "", cellFont);
            addTableCell(table, revenue.getDescription() != null ? revenue.getDescription() : "", cellFont);
            addTableCell(table, CURRENCY_FORMAT.format(revenue.getAmount()), cellFont, Element.ALIGN_RIGHT);
        }

        document.add(table);
    }

    private void addAccountChargeRevenuesSection(Document document, java.util.List<OwnerReportDTO.RevenueLineItem> accountRevenues) throws DocumentException {
        // Account Charges - Blue (#1976d2)
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, new BaseColor(25, 118, 210));
        Paragraph section = new Paragraph("Account Charges", sectionFont);
        section.setSpacingBefore(10);
        document.add(section);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingBefore(5);
        table.setSpacingAfter(10);

        Font headerFont = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD);
        PdfPCell[] headerCells = createTableHeaderCells(new String[]{"Date", "Category", "Description", "Amount"}, headerFont, new BaseColor(25, 118, 210));
        for (PdfPCell cell : headerCells) {
            table.addCell(cell);
        }

        Font cellFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);
        for (OwnerReportDTO.RevenueLineItem revenue : accountRevenues) {
            addTableCell(table, revenue.getRevenueDate().format(DATE_FORMAT), cellFont);
            addTableCell(table, revenue.getCategoryName() != null ? revenue.getCategoryName() : "", cellFont);
            addTableCell(table, revenue.getDescription() != null ? revenue.getDescription() : "", cellFont);
            addTableCell(table, CURRENCY_FORMAT.format(revenue.getAmount()), cellFont, Element.ALIGN_RIGHT);
        }

        document.add(table);
    }

    private void addCreditCardRevenuesSection(Document document, java.util.List<OwnerReportDTO.RevenueLineItem> creditCardRevenues) throws DocumentException {
        // Credit Card Revenue - Red (#d32f2f)
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, new BaseColor(211, 47, 47));
        Paragraph section = new Paragraph("Credit Card Revenue", sectionFont);
        section.setSpacingBefore(10);
        document.add(section);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingBefore(5);
        table.setSpacingAfter(10);

        Font headerFont = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD);
        PdfPCell[] headerCells = createTableHeaderCells(new String[]{"Date", "Category", "Description", "Amount"}, headerFont, new BaseColor(211, 47, 47));
        for (PdfPCell cell : headerCells) {
            table.addCell(cell);
        }

        Font cellFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);
        for (OwnerReportDTO.RevenueLineItem revenue : creditCardRevenues) {
            addTableCell(table, revenue.getRevenueDate().format(DATE_FORMAT), cellFont);
            addTableCell(table, revenue.getCategoryName() != null ? revenue.getCategoryName() : "", cellFont);
            addTableCell(table, revenue.getDescription() != null ? revenue.getDescription() : "", cellFont);
            addTableCell(table, CURRENCY_FORMAT.format(revenue.getAmount()), cellFont, Element.ALIGN_RIGHT);
        }

        document.add(table);
    }

    private void addOtherRevenuesSection(Document document, java.util.List<OwnerReportDTO.RevenueLineItem> otherRevenues) throws DocumentException {
        // Other Revenues - Orange (#f57c00)
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, new BaseColor(245, 127, 0));
        Paragraph section = new Paragraph("Other Revenues", sectionFont);
        section.setSpacingBefore(10);
        document.add(section);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingBefore(5);
        table.setSpacingAfter(10);

        Font headerFont = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD);
        PdfPCell[] headerCells = createTableHeaderCells(new String[]{"Date", "Category", "Description", "Amount"}, headerFont, new BaseColor(245, 127, 0));
        for (PdfPCell cell : headerCells) {
            table.addCell(cell);
        }

        Font cellFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);
        for (OwnerReportDTO.RevenueLineItem revenue : otherRevenues) {
            addTableCell(table, revenue.getRevenueDate().format(DATE_FORMAT), cellFont);
            addTableCell(table, revenue.getCategoryName() != null ? revenue.getCategoryName() : "", cellFont);
            addTableCell(table, revenue.getDescription() != null ? revenue.getDescription() : "", cellFont);
            addTableCell(table, CURRENCY_FORMAT.format(revenue.getAmount()), cellFont, Element.ALIGN_RIGHT);
        }

        document.add(table);
    }

    private void addTotalsSectionFooter(Document document, OwnerReportDTO report) throws DocumentException {
        document.add(new Paragraph(" "));

        // Professional totals section with better styling
        PdfPTable totalsOuter = new PdfPTable(1);
        totalsOuter.setWidthPercentage(100);
        totalsOuter.getDefaultCell().setBorder(PdfPCell.NO_BORDER);
        totalsOuter.getDefaultCell().setPadding(15);
        totalsOuter.getDefaultCell().setBackgroundColor(new BaseColor(245, 245, 245));

        PdfPTable totalsTable = new PdfPTable(2);
        totalsTable.setWidthPercentage(100);
        totalsTable.setSpacingBefore(5);

        Font labelFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, new BaseColor(80, 80, 80));
        Font valueFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, new BaseColor(80, 80, 80));

        // Summary rows
        addTableRow(totalsTable, "Total Revenues", CURRENCY_FORMAT.format(report.getTotalRevenues()), labelFont, valueFont);
        addTableRow(totalsTable, "Recurring Expenses", CURRENCY_FORMAT.format(report.getTotalRecurringExpenses()), labelFont, valueFont);
        addTableRow(totalsTable, "One-Time Expenses", CURRENCY_FORMAT.format(report.getTotalOneTimeExpenses()), labelFont, valueFont);

        // Divider
        PdfPCell dividerCell = new PdfPCell();
        dividerCell.setColspan(2);
        dividerCell.setBorder(PdfPCell.TOP);
        dividerCell.setBorderColor(new BaseColor(200, 200, 200));
        dividerCell.setPadding(8);
        totalsTable.addCell(dividerCell);

        // Net Due - highlighted prominently
        BigDecimal netDue = report.getNetDue() != null ? report.getNetDue() : BigDecimal.ZERO;
        BaseColor netDueColor = netDue.compareTo(BigDecimal.ZERO) > 0 ? new BaseColor(211, 47, 47) : new BaseColor(56, 142, 60);
        Font netDueFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.WHITE);

        PdfPCell labelCell = new PdfPCell(new Phrase("NET DUE", netDueFont));
        labelCell.setBorder(PdfPCell.NO_BORDER);
        labelCell.setBackgroundColor(netDueColor);
        labelCell.setPadding(10);
        labelCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        totalsTable.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(CURRENCY_FORMAT.format(Math.abs(netDue.doubleValue())), netDueFont));
        valueCell.setBorder(PdfPCell.NO_BORDER);
        valueCell.setBackgroundColor(netDueColor);
        valueCell.setPadding(10);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalsTable.addCell(valueCell);

        PdfPCell tableCell = new PdfPCell(totalsTable);
        tableCell.setBorder(PdfPCell.NO_BORDER);
        tableCell.setPadding(0);
        totalsOuter.addCell(tableCell);

        document.add(totalsOuter);
    }

    private void addFooter(Document document) throws DocumentException {
        document.add(new Paragraph(" "));

        // Divider line
        PdfPTable divider = new PdfPTable(1);
        divider.setWidthPercentage(100);
        divider.getDefaultCell().setBorder(PdfPCell.TOP);
        divider.getDefaultCell().setBorderColor(new BaseColor(200, 200, 200));
        divider.getDefaultCell().setFixedHeight(1f);
        divider.addCell("");
        document.add(divider);

        document.add(new Paragraph(" "));

        // Footer text
        Font footerFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, new BaseColor(120, 120, 120));
        Paragraph footer = new Paragraph("This is an automated statement from Maclures Cabs. For questions, please contact the office.", footerFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);

        Paragraph timestamp = new Paragraph("Statement generated on " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm a")), footerFont);
        timestamp.setAlignment(Element.ALIGN_CENTER);
        document.add(timestamp);
    }

    private void addTableHeader(PdfPTable table, String[] headers, Font font) {
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, font));
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            cell.setPadding(5);
            table.addCell(cell);
        }
    }

    private PdfPCell[] createTableHeaderCells(String[] headers, Font font, BaseColor color) {
        PdfPCell[] cells = new PdfPCell[headers.length];
        Font whiteFont = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, BaseColor.WHITE);
        for (int i = 0; i < headers.length; i++) {
            PdfPCell cell = new PdfPCell(new Phrase(headers[i], whiteFont));
            cell.setBackgroundColor(color);
            cell.setPadding(5);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cells[i] = cell;
        }
        return cells;
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
