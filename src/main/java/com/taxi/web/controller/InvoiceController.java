package com.taxi.web.controller;

import com.taxi.domain.account.dto.InvoiceSummaryDTO;
import com.taxi.domain.account.dto.InvoiceDetailsDTO;
import com.taxi.domain.account.model.Invoice;
import com.taxi.domain.account.service.InvoiceService;
import com.taxi.domain.account.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final com.taxi.domain.account.service.EmailService emailService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<InvoiceSummaryDTO>> getAllInvoices() {
        List<InvoiceSummaryDTO> invoices = invoiceService.getAllInvoiceSummaries();
        return ResponseEntity.ok(invoices);
    }

    /**
     * Generate invoice for customer
     */
    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<Invoice> generateInvoice(
            @RequestParam Long customerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd,
            @RequestParam(required = false) BigDecimal taxRate,
            @RequestParam(required = false) String terms) {
        
        Invoice invoice = invoiceService.generateInvoice(
                customerId, periodStart, periodEnd, taxRate, terms);
        return ResponseEntity.ok(invoice);
    }

    /**
     * Get invoice by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DISPATCHER')")
    public ResponseEntity<InvoiceDetailsDTO> getInvoice(@PathVariable Long id) {
        InvoiceDetailsDTO invoice = invoiceService.getInvoiceDetailsById(id);
        return ResponseEntity.ok(invoice);
    }

    /**
     * Get invoice by invoice number
     */
    @GetMapping("/number/{invoiceNumber}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<InvoiceDetailsDTO> getInvoiceByNumber(@PathVariable String invoiceNumber) {
        InvoiceDetailsDTO invoice = invoiceService.getInvoiceDetailsByNumber(invoiceNumber);
        return ResponseEntity.ok(invoice);
    }

    /**
     * Get all invoices for customer
     */
    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DISPATCHER')")
    public ResponseEntity<List<InvoiceSummaryDTO>> getCustomerInvoices(@PathVariable Long customerId) {
        List<InvoiceSummaryDTO> invoices = invoiceService.getInvoiceSummariesByCustomer(customerId);
        return ResponseEntity.ok(invoices);
    }

    /**
     * Get all invoices for account
     */
    @GetMapping("/account/{accountId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<InvoiceSummaryDTO>> getAccountInvoices(@PathVariable String accountId) {
        List<InvoiceSummaryDTO> invoices = invoiceService.getInvoiceSummariesByAccount(accountId);
        return ResponseEntity.ok(invoices);
    }

    /**
     * Get unpaid invoices for customer
     */
    @GetMapping("/customer/{customerId}/unpaid")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<InvoiceSummaryDTO>> getUnpaidInvoices(@PathVariable Long customerId) {
        List<InvoiceSummaryDTO> invoices = invoiceService.getUnpaidInvoiceSummariesByCustomer(customerId);
        return ResponseEntity.ok(invoices);
    }

    /**
     * Get unpaid invoices for account
     */
    @GetMapping("/account/{accountId}/unpaid")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<InvoiceSummaryDTO>> getUnpaidInvoicesByAccount(@PathVariable String accountId) {
        List<InvoiceSummaryDTO> invoices = invoiceService.getUnpaidInvoiceSummariesByAccount(accountId);
        return ResponseEntity.ok(invoices);
    }

    /**
     * Get all overdue invoices
     */
    @GetMapping("/overdue")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<InvoiceSummaryDTO>> getOverdueInvoices() {
        List<InvoiceSummaryDTO> invoices = invoiceService.getOverdueInvoiceSummaries();
        return ResponseEntity.ok(invoices);
    }

    /**
     * Send invoice (mark as sent)
     */
    @PutMapping("/{id}/send")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<InvoiceSummaryDTO> sendInvoice(@PathVariable Long id) {
        InvoiceSummaryDTO invoice = invoiceService.sendInvoice(id);
        return ResponseEntity.ok(invoice);
    }

    /**
     * Cancel invoice
     */
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<InvoiceSummaryDTO> cancelInvoice(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        InvoiceSummaryDTO invoice = invoiceService.cancelInvoice(id, reason);
        return ResponseEntity.ok(invoice);
    }

    /**
     * Update invoice status
     */
    @PutMapping("/{id}/update-status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<Invoice> updateStatus(@PathVariable Long id) {
        Invoice invoice = invoiceService.updateInvoiceStatus(id);
        return ResponseEntity.ok(invoice);
    }

    /**
     * Get outstanding balance for customer
     */
    @GetMapping("/customer/{customerId}/balance")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<BigDecimal> getOutstandingBalance(@PathVariable Long customerId) {
        BigDecimal balance = invoiceService.calculateOutstandingBalance(customerId);
        return ResponseEntity.ok(balance);
    }

    /**
     * Get outstanding balance for account
     */
    @GetMapping("/account/{accountId}/balance")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<BigDecimal> getOutstandingBalanceByAccount(@PathVariable String accountId) {
        BigDecimal balance = invoiceService.calculateOutstandingBalanceByAccount(accountId);
        return ResponseEntity.ok(balance);
    }

    /**
     * Get invoice summary for customer
     */
    @GetMapping("/customer/{customerId}/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<Map<String, Object>> getInvoiceSummary(@PathVariable Long customerId) {
        Map<String, Object> summary = invoiceService.getInvoiceSummary(customerId);
        return ResponseEntity.ok(summary);
    }

    /**
     * Download invoice as PDF
     */
    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DISPATCHER')")
    public ResponseEntity<byte[]> downloadInvoicePDF(@PathVariable Long id) {
        byte[] pdfContent = invoiceService.generateInvoicePDF(id);

        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=invoice_" + id + ".pdf")
                .body(pdfContent);
    }

    /**
     * Send invoice via email to customer
     */
    @PostMapping("/{id}/send-email")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<Map<String, String>> sendInvoiceViaEmail(
            @PathVariable Long id,
            @RequestParam String recipientEmail) {
        try {
            invoiceService.sendInvoiceViaEmail(id, recipientEmail);
            return ResponseEntity.ok(Map.of(
                    "success", "true",
                    "message", "Invoice sent successfully to " + recipientEmail
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", "false",
                    "message", "Failed to send invoice: " + e.getMessage()
            ));
        }
    }

    /**
     * Get email send history for invoice
     */
    @GetMapping("/{id}/email-history")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<Map<String, Object>> getEmailHistory(@PathVariable Long id) {
        Invoice invoice = invoiceService.getInvoiceById(id);
        return ResponseEntity.ok(Map.of(
                "lastEmailSentAt", invoice.getLastEmailSentAt(),
                "lastEmailSentTo", invoice.getLastEmailSentTo(),
                "emailSendCount", invoice.getEmailSendCount()
        ));
    }

    /**
     * Test email configuration
     * This endpoint helps debug email configuration without needing an invoice
     */
    @PostMapping("/test/send-email")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<Map<String, String>> testEmailConfiguration(
            @RequestParam String recipientEmail) {
        try {
            emailService.sendTestEmail(recipientEmail);
            return ResponseEntity.ok(Map.of(
                    "success", "true",
                    "message", "Test email sent successfully to " + recipientEmail + ". Check your inbox and spam folder."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", "false",
                    "message", "Failed to send test email: " + e.getMessage()
            ));
        }
    }
}
