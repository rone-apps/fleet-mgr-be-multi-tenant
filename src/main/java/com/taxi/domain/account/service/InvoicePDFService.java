package com.taxi.domain.account.service;

import com.itextpdf.text.*;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.taxi.domain.account.model.Invoice;
import com.taxi.domain.account.model.InvoiceLineItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class InvoicePDFService {

    private static final BaseColor HEADER_COLOR = new BaseColor(62, 82, 68); // Dark green
    private static final BaseColor ACCENT_COLOR = new BaseColor(76, 175, 80); // Light green
    private static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 24, Font.BOLD, BaseColor.WHITE);
    private static final Font HEADER_FONT = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.WHITE);
    private static final Font LABEL_FONT = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD);
    private static final Font NORMAL_FONT = new Font(Font.FontFamily.HELVETICA, 10);
    private static final Font SMALL_FONT = new Font(Font.FontFamily.HELVETICA, 9);

    /**
     * Generate PDF invoice as byte array with company name
     */
    public byte[] generateInvoicePDF(Invoice invoice, String companyName) {
        return generateInvoicePDFInternal(invoice, companyName);
    }

    /**
     * Generate PDF invoice as byte array (backward compatibility, defaults to Maclures Cabs)
     */
    public byte[] generateInvoicePDF(Invoice invoice) {
        return generateInvoicePDFInternal(invoice, "Maclures Cabs");
    }

    /**
     * Internal method to generate PDF
     */
    private byte[] generateInvoicePDFInternal(Invoice invoice, String companyName) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, baos);

            document.open();

            // Add content to PDF
            addHeader(document, invoice, companyName);
            document.add(new Paragraph(" "));
            addCustomerDetails(document, invoice);
            document.add(new Paragraph(" "));
            addInvoiceDetails(document, invoice);
            document.add(new Paragraph(" "));
            addLineItemsTable(document, invoice);
            document.add(new Paragraph(" "));
            addTotalsSection(document, invoice);
            document.add(new Paragraph(" "));
            if (invoice.getTerms() != null && !invoice.getTerms().isEmpty()) {
                addTermsSection(document, invoice);
                document.add(new Paragraph(" "));
            }
            addFooter(document);

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Failed to generate PDF invoice: {}", e.getMessage());
            throw new RuntimeException("Failed to generate PDF invoice: " + e.getMessage());
        }
    }

    /**
     * Add header with company name and invoice title
     */
    private void addHeader(Document document, Invoice invoice, String companyName) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setSpacingBefore(0);
        headerTable.setSpacingAfter(20);

        // Logo/Company name section
        PdfPCell companyCell = new PdfPCell();
        companyCell.setBackgroundColor(HEADER_COLOR);
        companyCell.setPadding(15);
        companyCell.setBorder(0);
        Paragraph companyPara = new Paragraph();
        companyPara.add(new Paragraph("🚕 FareFlow", TITLE_FONT));
        companyPara.add(new Paragraph(companyName != null ? companyName : "Maclures Cabs", new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, BaseColor.WHITE)));
        companyCell.addElement(companyPara);
        headerTable.addCell(companyCell);

        // Invoice title
        PdfPCell invoiceTitleCell = new PdfPCell();
        invoiceTitleCell.setBackgroundColor(HEADER_COLOR);
        invoiceTitleCell.setPadding(15);
        invoiceTitleCell.setBorder(0);
        invoiceTitleCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph titlePara = new Paragraph();
        titlePara.add(new Paragraph("INVOICE", TITLE_FONT));
        titlePara.add(new Paragraph(invoice.getInvoiceNumber(), new Font(Font.FontFamily.HELVETICA, 14, Font.NORMAL, BaseColor.WHITE)));
        invoiceTitleCell.addElement(titlePara);
        headerTable.addCell(invoiceTitleCell);

        document.add(headerTable);
    }

    /**
     * Add customer details section
     */
    private void addCustomerDetails(Document document, Invoice invoice) throws DocumentException {
        PdfPTable detailsTable = new PdfPTable(3);
        detailsTable.setWidthPercentage(100);
        detailsTable.setSpacingAfter(15);
        float[] columnWidths = {1.5f, 1.5f, 1f};
        detailsTable.setWidths(columnWidths);

        // Bill To
        PdfPCell billToHeader = new PdfPCell(new Paragraph("BILL TO:", LABEL_FONT));
        billToHeader.setBorder(0);
        billToHeader.setPaddingBottom(5);
        detailsTable.addCell(billToHeader);

        // Invoice Date
        PdfPCell invoiceDateHeader = new PdfPCell(new Paragraph("INVOICE DATE:", LABEL_FONT));
        invoiceDateHeader.setBorder(0);
        invoiceDateHeader.setPaddingBottom(5);
        detailsTable.addCell(invoiceDateHeader);

        // Invoice Number Header
        PdfPCell invoiceNumberHeader = new PdfPCell(new Paragraph("INVOICE #:", LABEL_FONT));
        invoiceNumberHeader.setBorder(0);
        invoiceNumberHeader.setPaddingBottom(5);
        detailsTable.addCell(invoiceNumberHeader);

        // Customer info
        Paragraph customerInfo = new Paragraph();
        customerInfo.add(invoice.getCustomer().getCompanyName() + "\n");
        customerInfo.add(invoice.getCustomer().getContactPerson() + "\n");
        customerInfo.add(invoice.getCustomer().getStreetAddress() + "\n");
        customerInfo.add(invoice.getCustomer().getCity() + ", " + invoice.getCustomer().getProvince() + " " + invoice.getCustomer().getPostalCode() + "\n");
        customerInfo.add(invoice.getCustomer().getCountry() + "\n");
        customerInfo.setFont(NORMAL_FONT);

        PdfPCell billToCell = new PdfPCell(customerInfo);
        billToCell.setBorder(0);
        detailsTable.addCell(billToCell);

        // Invoice date
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("MMM dd, yyyy");
        PdfPCell invoiceDateCell = new PdfPCell(new Paragraph(invoice.getInvoiceDate().format(dateFormat), NORMAL_FONT));
        invoiceDateCell.setBorder(0);
        detailsTable.addCell(invoiceDateCell);

        // Invoice number
        PdfPCell invoiceNumberCell = new PdfPCell(new Paragraph(invoice.getInvoiceNumber(), NORMAL_FONT));
        invoiceNumberCell.setBorder(0);
        detailsTable.addCell(invoiceNumberCell);

        document.add(detailsTable);
    }

    /**
     * Add invoice details (period, due date, etc)
     */
    private void addInvoiceDetails(Document document, Invoice invoice) throws DocumentException {
        PdfPTable detailsTable = new PdfPTable(4);
        detailsTable.setWidthPercentage(100);
        detailsTable.setSpacingAfter(15);

        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("MMM dd, yyyy");

        // Billing Period
        addDetailRow(detailsTable, "BILLING PERIOD",
                invoice.getBillingPeriodStart().format(dateFormat) + " - " +
                invoice.getBillingPeriodEnd().format(dateFormat));

        // Due Date
        addDetailRow(detailsTable, "DUE DATE", invoice.getDueDate().format(dateFormat));

        // Account ID
        addDetailRow(detailsTable, "ACCOUNT ID", invoice.getAccountId());

        // Status
        addDetailRow(detailsTable, "STATUS", invoice.getStatus().toString());

        document.add(detailsTable);
    }

    /**
     * Add detail row to table
     */
    private void addDetailRow(PdfPTable table, String label, String value) throws DocumentException {
        PdfPCell labelCell = new PdfPCell(new Paragraph(label, LABEL_FONT));
        labelCell.setBackgroundColor(new BaseColor(245, 245, 245));
        labelCell.setPadding(8);
        labelCell.setBorderColor(ACCENT_COLOR);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Paragraph(value, NORMAL_FONT));
        valueCell.setPadding(8);
        valueCell.setBorderColor(ACCENT_COLOR);
        table.addCell(valueCell);
    }

    /**
     * Add line items table
     */
    private void addLineItemsTable(Document document, Invoice invoice) throws DocumentException {
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setSpacingAfter(15);
        float[] columnWidths = {1f, 2f, 0.8f, 1f, 1f};
        table.setWidths(columnWidths);

        // Header row
        addTableHeader(table, new String[]{"Date", "Description", "Qty", "Amount", "Total"});

        // Line items
        DecimalFormat currencyFormat = new DecimalFormat("$#,##0.00");
        for (InvoiceLineItem item : invoice.getLineItems()) {
            PdfPCell dateCell = new PdfPCell(new Paragraph(item.getTripDate().toString(), NORMAL_FONT));
            dateCell.setPadding(8);
            table.addCell(dateCell);

            PdfPCell descCell = new PdfPCell(new Paragraph(item.getDescription(), NORMAL_FONT));
            descCell.setPadding(8);
            table.addCell(descCell);

            PdfPCell qtyCell = new PdfPCell(new Paragraph("1", NORMAL_FONT));
            qtyCell.setPadding(8);
            qtyCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(qtyCell);

            PdfPCell amountCell = new PdfPCell(new Paragraph(currencyFormat.format(item.getAmount()), NORMAL_FONT));
            amountCell.setPadding(8);
            amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(amountCell);

            PdfPCell totalCell = new PdfPCell(new Paragraph(currencyFormat.format(item.getAmount()), NORMAL_FONT));
            totalCell.setPadding(8);
            totalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(totalCell);
        }

        document.add(table);
    }

    /**
     * Add table header cells
     */
    private void addTableHeader(PdfPTable table, String[] headers) throws DocumentException {
        for (String header : headers) {
            PdfPCell headerCell = new PdfPCell(new Paragraph(header, HEADER_FONT));
            headerCell.setBackgroundColor(ACCENT_COLOR);
            headerCell.setPadding(10);
            headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(headerCell);
        }
    }

    /**
     * Add totals section
     */
    private void addTotalsSection(Document document, Invoice invoice) throws DocumentException {
        DecimalFormat currencyFormat = new DecimalFormat("$#,##0.00");

        PdfPTable totalsTable = new PdfPTable(2);
        totalsTable.setWidthPercentage(50);
        totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalsTable.setSpacingAfter(15);

        // Subtotal
        addTotalRow(totalsTable, "Subtotal:", currencyFormat.format(invoice.getSubtotal()), false);

        // Tax
        if (invoice.getTaxRate().compareTo(BigDecimal.ZERO) > 0) {
            addTotalRow(totalsTable, "Tax (" + invoice.getTaxRate().stripTrailingZeros().toPlainString() + "%):",
                    currencyFormat.format(invoice.getTaxAmount()), false);
        }

        // Total (highlighted)
        addTotalRow(totalsTable, "TOTAL:", currencyFormat.format(invoice.getTotalAmount()), true);

        // Amount paid if applicable
        if (invoice.getAmountPaid().compareTo(BigDecimal.ZERO) > 0) {
            addTotalRow(totalsTable, "Amount Paid:", currencyFormat.format(invoice.getAmountPaid()), false);
            addTotalRow(totalsTable, "BALANCE DUE:", currencyFormat.format(invoice.getBalanceDue()), true);
        } else {
            addTotalRow(totalsTable, "BALANCE DUE:", currencyFormat.format(invoice.getBalanceDue()), true);
        }

        document.add(totalsTable);
    }

    /**
     * Add total row
     */
    private void addTotalRow(PdfPTable table, String label, String value, boolean highlight) throws DocumentException {
        PdfPCell labelCell = new PdfPCell(new Paragraph(label, highlight ? new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD) : NORMAL_FONT));
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        labelCell.setPadding(8);
        if (highlight) {
            labelCell.setBackgroundColor(ACCENT_COLOR);
        }
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Paragraph(value, highlight ? new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD) : NORMAL_FONT));
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPadding(8);
        if (highlight) {
            valueCell.setBackgroundColor(ACCENT_COLOR);
        }
        table.addCell(valueCell);
    }

    /**
     * Add terms section
     */
    private void addTermsSection(Document document, Invoice invoice) throws DocumentException {
        Paragraph termsPara = new Paragraph("TERMS & CONDITIONS", LABEL_FONT);
        termsPara.setSpacingAfter(10);
        document.add(termsPara);

        Paragraph termsContent = new Paragraph(invoice.getTerms(), SMALL_FONT);
        termsContent.setSpacingAfter(15);
        document.add(termsContent);
    }

    /**
     * Add footer
     */
    private void addFooter(Document document) throws DocumentException {
        Paragraph footer = new Paragraph();
        footer.add(new Chunk("Thank you for your business!", SMALL_FONT));
        footer.add(new Chunk("\n\n", SMALL_FONT));
        footer.add(new Chunk("For questions about this invoice, please contact us.", SMALL_FONT));
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(30);
        document.add(footer);
    }
}
