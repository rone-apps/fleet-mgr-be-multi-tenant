package com.taxi.web.controller;

import com.taxi.domain.account.model.Payment;
import com.taxi.domain.account.model.PaymentBatch;
import com.taxi.domain.account.model.StatementAuditLog;
import com.taxi.domain.account.service.PaymentService;
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
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Create a new payment batch (for bulk payment entry)
     */
    @PostMapping("/batches/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<PaymentBatch> createPaymentBatch(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate batchDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd,
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        PaymentBatch batch = paymentService.createPaymentBatch(batchDate, periodStart, periodEnd, userId);
        return ResponseEntity.ok(batch);
    }

    /**
     * Add payment to draft batch
     */
    @PostMapping("/batches/{batchId}/payments")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<Payment> addPaymentToBatch(
            @PathVariable Long batchId,
            @RequestParam Long statementId,
            @RequestParam BigDecimal amount,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate paymentDate,
            @RequestParam Long paymentMethodId,
            @RequestParam(required = false) String referenceNumber,
            @RequestParam(required = false) String notes,
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        Payment payment = paymentService.addPaymentToBatch(batchId, statementId, amount, paymentDate,
                paymentMethodId, referenceNumber, notes, userId);
        return ResponseEntity.ok(payment);
    }

    /**
     * Update payment in draft batch
     */
    @PutMapping("/payments/{paymentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<Payment> updatePayment(
            @PathVariable Long paymentId,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String referenceNumber,
            @RequestParam(required = false) String notes,
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        Payment payment = paymentService.updatePayment(paymentId, amount, referenceNumber, notes, userId);
        return ResponseEntity.ok(payment);
    }

    /**
     * Post payment batch (transition from DRAFT to POSTED)
     */
    @PutMapping("/batches/{batchId}/post")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<Void> postPaymentBatch(
            @PathVariable Long batchId,
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        paymentService.postPaymentBatch(batchId, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Mark batch completed (all payments processed, statements transitioned to PAID)
     */
    @PutMapping("/batches/{batchId}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<Void> markBatchCompleted(
            @PathVariable Long batchId,
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        paymentService.markBatchCompleted(batchId, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Get payment batch details
     */
    @GetMapping("/batches/{batchId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<PaymentBatch> getPaymentBatch(@PathVariable Long batchId) {
        PaymentBatch batch = paymentService.getPaymentBatch(batchId);
        return ResponseEntity.ok(batch);
    }

    /**
     * Get all payments for a statement
     */
    @GetMapping("/statements/{statementId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<Payment>> getStatementPayments(@PathVariable Long statementId) {
        List<Payment> payments = paymentService.getStatementPayments(statementId);
        return ResponseEntity.ok(payments);
    }

    /**
     * Get statement audit history
     */
    @GetMapping("/statements/{statementId}/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<StatementAuditLog>> getStatementHistory(@PathVariable Long statementId) {
        List<StatementAuditLog> history = paymentService.getStatementHistory(statementId);
        return ResponseEntity.ok(history);
    }

    /**
     * Post a statement (DRAFT -> POSTED)
     */
    @PutMapping("/statements/{statementId}/post")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<Void> postStatement(
            @PathVariable Long statementId,
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        paymentService.postStatement(statementId, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Lock a statement (POSTED -> LOCKED)
     */
    @PutMapping("/statements/{statementId}/lock")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<Void> lockStatement(
            @PathVariable Long statementId,
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        paymentService.lockStatement(statementId, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Helper method to extract user ID from authentication
     */
    private Long extractUserId(Authentication authentication) {
        // TODO: Implement based on your User model and authentication setup
        // For now, returning a placeholder
        return 1L;
    }
}
