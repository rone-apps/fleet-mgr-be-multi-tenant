package com.taxi.web.controller;

import com.taxi.domain.account.dto.AccountChargePaymentRequest;
import com.taxi.domain.account.model.Payment;
import com.taxi.domain.account.model.PaymentBatch;
import com.taxi.domain.account.model.StatementAuditLog;
import com.taxi.domain.account.model.StatementPayment;
import com.taxi.domain.account.repository.PaymentBatchRepository;
import com.taxi.domain.account.repository.StatementPaymentRepository;
import com.taxi.domain.account.service.PaymentService;
import com.taxi.domain.account.model.PaymentMethod;
import com.taxi.domain.account.repository.PaymentMethodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentBatchRepository paymentBatchRepository;
    private final StatementPaymentRepository statementPaymentRepository;
    private final PaymentMethodRepository paymentMethodRepository;

    /**
     * Add payment to draft batch
     */
    @PostMapping("/batches/{batchId}/payments")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<StatementPayment> addPaymentToBatch(
            @PathVariable Long batchId,
            @RequestParam Long statementId,
            @RequestParam BigDecimal amount,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate paymentDate,
            @RequestParam Long paymentMethodId,
            @RequestParam(required = false) String referenceNumber,
            @RequestParam(required = false) String notes,
            Authentication authentication) {

        log.info("addPaymentToBatch - batchId: {}, statementId: {}, amount: {}, paymentMethodId: {}, paymentDate: {}",
                 batchId, statementId, amount, paymentMethodId, paymentDate);

        Long userId = extractUserId(authentication);
        StatementPayment payment = paymentService.addPaymentToBatch(batchId, statementId, amount, paymentDate,
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
    @Transactional
    public ResponseEntity<PaymentBatch> postPaymentBatch(
            @PathVariable Long batchId,
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        paymentService.postPaymentBatch(batchId, userId);
        PaymentBatch batch = paymentBatchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found"));
        return ResponseEntity.ok(batch);
    }

    /**
     * Mark batch completed (all payments processed, statements transitioned to PAID)
     */
    @PutMapping("/batches/{batchId}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    @Transactional
    public ResponseEntity<PaymentBatch> markBatchCompleted(
            @PathVariable Long batchId,
            @RequestBody(required = false) java.util.Map<String, Object> paymentData,
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        paymentService.markBatchCompleted(batchId, userId, paymentData);
        PaymentBatch batch = paymentBatchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found"));
        return ResponseEntity.ok(batch);
    }

    /**
     * Get payment batch details
     */
    @GetMapping("/batches/{batchId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    @Transactional
    public ResponseEntity<PaymentBatch> getPaymentBatch(@PathVariable Long batchId) {
        PaymentBatch batch = paymentService.getPaymentBatch(batchId);
        return ResponseEntity.ok(batch);
    }

    /**
     * Get all statement payments for a batch
     */
    @GetMapping("/batches/{batchId}/statement-payments")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<StatementPayment>> getStatementPaymentsForBatch(@PathVariable Long batchId) {
        List<StatementPayment> payments = statementPaymentRepository.findByPaymentBatchId(batchId);
        return ResponseEntity.ok(payments);
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
     * List all payment batches
     */
    @GetMapping("/batches")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    @Transactional
    public ResponseEntity<List<PaymentBatch>> listPaymentBatches(
            @RequestParam(required = false) String status) {

        List<PaymentBatch> batches;
        if (status != null && !status.isEmpty()) {
            batches = paymentBatchRepository.findByStatus(status);
        } else {
            batches = paymentBatchRepository.findAll();
        }
        return ResponseEntity.ok(batches);
    }

    /**
     * Create a new payment batch (POST endpoint)
     */
    @PostMapping("/batches")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<PaymentBatch> createPaymentBatch(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate batchDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodTo,
            @RequestParam(required = false) String notes,
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        PaymentBatch batch = paymentService.createPaymentBatch(batchDate, periodFrom, periodTo, userId);
        return ResponseEntity.ok(batch);
    }

    /**
     * Update batch with statement IDs
     */
    @PutMapping("/batches/{batchId}/set-statements")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<PaymentBatch> setStatements(
            @PathVariable Long batchId,
            @RequestParam String statementIds) {

        log.info("Setting statement IDs for batch {}: {}", batchId, statementIds);
        PaymentBatch batch = paymentBatchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found"));

        batch.setStatementIds(statementIds);
        PaymentBatch updated = paymentBatchRepository.save(batch);
        return ResponseEntity.ok(updated);
    }

    /**
     * Recall a statement
     */
    @PutMapping("/statements/{statementId}/recall")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<Object> recallStatement(
            @PathVariable Long statementId,
            @RequestParam String reason,
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        try {
            Object result = paymentService.recallStatement(statementId, reason, userId);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Record payment for account customer invoice
     * Used to record payments for account/customer invoices (separate from driver/owner payments)
     * Accepts invoiceId to match frontend expectations
     */
    @PostMapping("/account-charges/{invoiceId}/record-payment")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    @Transactional
    public ResponseEntity<Payment> recordAccountChargePayment(
            @PathVariable Long invoiceId,
            @RequestParam BigDecimal amount,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate paymentDate,
            @RequestParam Long paymentMethodId,
            @RequestParam(required = false) String referenceNumber,
            @RequestParam(required = false) String notes,
            Authentication authentication) {

        log.info("Recording payment for invoice {}: amount={}, paymentMethodId={}, paymentDate={}",
                invoiceId, amount, paymentMethodId, paymentDate);

        Long userId = extractUserId(authentication);
        Payment payment = paymentService.recordPaymentForInvoice(invoiceId, amount, paymentDate,
                paymentMethodId, referenceNumber, notes, userId);
        return ResponseEntity.ok(payment);
    }

    /**
     * Record batch payments for multiple account charges
     * Used to record payments for multiple account/customer invoices at once
     */
    @PostMapping("/account-charges/batch-payment")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    @Transactional
    public ResponseEntity<List<Payment>> recordBatchAccountChargePayments(
            @RequestBody List<AccountChargePaymentRequest> paymentRequests,
            Authentication authentication) {

        log.info("Recording batch payments for {} account charges", paymentRequests.size());

        Long userId = extractUserId(authentication);
        List<Payment> payments = paymentService.recordBatchAccountChargePayments(paymentRequests, userId);
        return ResponseEntity.ok(payments);
    }

    /**
     * List all payment methods
     */
    @GetMapping("/payment-methods")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<PaymentMethod>> listPaymentMethods() {
        List<PaymentMethod> methods = paymentMethodRepository.findAll();
        return ResponseEntity.ok(methods);
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
