package com.taxi.web.controller;

import com.taxi.domain.account.model.Payment;
import com.taxi.domain.account.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final InvoiceService invoiceService;

    /**
     * Record payment for invoice
     */
    @PostMapping("/record")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<Payment> recordPayment(
            @RequestParam Long invoiceId,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate paymentDate,
            @RequestParam Payment.PaymentMethod paymentMethod,
            @RequestParam(required = false) String referenceNumber,
            @RequestParam(required = false) String notes,
            Authentication authentication) {
        
        // Get user ID from authentication (simplified - adjust based on your User model)
        Long userId = null; // You'll need to extract this from authentication
        
        Payment payment = invoiceService.recordPayment(
                invoiceId, amount, paymentDate, paymentMethod, 
                referenceNumber, notes, userId);
        
        return ResponseEntity.ok(payment);
    }

    /**
     * Get payments for invoice
     */
    @GetMapping("/invoice/{invoiceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<Payment>> getInvoicePayments(@PathVariable Long invoiceId) {
        List<Payment> payments = invoiceService.getPaymentsByInvoice(invoiceId);
        return ResponseEntity.ok(payments);
    }

    /**
     * Get payments for customer
     */
    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<Payment>> getCustomerPayments(@PathVariable Long customerId) {
        List<Payment> payments = invoiceService.getPaymentsByCustomer(customerId);
        return ResponseEntity.ok(payments);
    }

    /**
     * Get payment by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<Payment> getPayment(@PathVariable Long id) {
        // Note: You'll need to add getPaymentById method to InvoiceService
        // For now, this is a placeholder
        return ResponseEntity.notFound().build();
    }
}
