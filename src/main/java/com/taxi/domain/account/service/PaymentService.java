package com.taxi.domain.account.service;

import com.taxi.domain.account.model.*;
import com.taxi.domain.account.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentBatchRepository paymentBatchRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final StatementAuditLogRepository auditLogRepository;
    private final InvoiceRepository invoiceRepository;

    /**
     * Post a statement (DRAFT -> POSTED)
     * Locks statement and generates PDF
     */
    @Transactional
    public void postStatement(Long statementId, Long userId) {
        Invoice statement = invoiceRepository.findById(statementId)
                .orElseThrow(() -> new IllegalArgumentException("Statement not found"));

        if (statement.getStatus() != Invoice.InvoiceStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT statements can be posted");
        }

        // Update statement
        statement.setStatus(Invoice.InvoiceStatus.POSTED);
        statement.setPostedAt(LocalDateTime.now());
        statement.setPostedBy(userId);

        // TODO: Generate immutable PDF and store path
        // statement.setPdfPath(generateStatementPDF(statement));
        // statement.setPdfGeneratedAt(LocalDateTime.now());

        invoiceRepository.save(statement);

        // Log audit
        createAuditLog(statement, "POSTED", "DRAFT", "POSTED", null, userId);
    }

    /**
     * Lock a statement (POSTED -> LOCKED)
     * Prepares for payment processing
     */
    @Transactional
    public void lockStatement(Long statementId, Long userId) {
        Invoice statement = invoiceRepository.findById(statementId)
                .orElseThrow(() -> new IllegalArgumentException("Statement not found"));

        if (statement.getStatus() != Invoice.InvoiceStatus.POSTED) {
            throw new IllegalStateException("Only POSTED statements can be locked");
        }

        statement.setStatus(Invoice.InvoiceStatus.LOCKED);
        statement.setLockedAt(LocalDateTime.now());
        statement.setLockedBy(userId);

        invoiceRepository.save(statement);

        createAuditLog(statement, "LOCKED", "POSTED", "LOCKED", null, userId);
    }

    /**
     * Recall a statement (POSTED/LOCKED -> DRAFT as new version)
     * Can only recall if no payments have been made
     */
    @Transactional
    public Invoice recallStatement(Long statementId, String reason, Long userId) {
        Invoice original = invoiceRepository.findById(statementId)
                .orElseThrow(() -> new IllegalArgumentException("Statement not found"));

        if (original.getStatus() != Invoice.InvoiceStatus.POSTED && original.getStatus() != Invoice.InvoiceStatus.LOCKED) {
            throw new IllegalStateException("Can only recall POSTED or LOCKED statements");
        }

        // Check if payments made
        List<Payment> payments = paymentRepository.findCompletedPaymentsByStatement(statementId);
        if (!payments.isEmpty()) {
            throw new IllegalStateException("Cannot recall after payments have been made");
        }

        // Create amendment (new version)
        Invoice amendment = Invoice.builder()
                .customer(original.getCustomer())
                .invoiceNumber(original.getInvoiceNumber() + "-v" + (original.getStatementVersion() + 1))
                .billingPeriodStart(original.getBillingPeriodStart())
                .billingPeriodEnd(original.getBillingPeriodEnd())
                .invoiceDate(LocalDate.now())
                .dueDate(original.getDueDate())
                .subtotal(original.getSubtotal())
                .taxRate(original.getTaxRate())
                .taxAmount(original.getTaxAmount())
                .totalAmount(original.getTotalAmount())
                .balanceDue(original.getBalanceDue())
                .status(Invoice.InvoiceStatus.DRAFT)
                .statementVersion(original.getStatementVersion() + 1)
                .parentStatementId(original.getId())
                .createdBy(userId)
                .build();

        invoiceRepository.save(amendment);

        // Archive original
        original.setStatus(Invoice.InvoiceStatus.ARCHIVED);
        invoiceRepository.save(original);

        createAuditLog(original, "RECALLED", original.getStatus().name(), "ARCHIVED", reason, userId);
        createAuditLog(amendment, "AMENDMENT_CREATED", null, "DRAFT",
                "Amendment created due to recall of statement " + statementId, userId);

        return amendment;
    }

    /**
     * Create a bulk payment batch
     */
    @Transactional
    public PaymentBatch createPaymentBatch(LocalDate batchDate, LocalDate periodStart, 
                                           LocalDate periodEnd, Long userId) {
        String batchNumber = "BATCH-" + LocalDate.now() + "-" + UUID.randomUUID().toString().substring(0, 8);

        PaymentBatch batch = PaymentBatch.builder()
                .batchNumber(batchNumber)
                .batchDate(batchDate)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .status("DRAFT")
                .createdBy(userId)
                .build();

        return paymentBatchRepository.save(batch);
    }

    /**
     * Add payment to batch (pre-posting)
     */
    @Transactional
    public Payment addPaymentToBatch(Long batchId, Long statementId, BigDecimal amount, 
                                      LocalDate paymentDate, Long paymentMethodId, 
                                      String referenceNumber, String notes, Long userId) {

        PaymentBatch batch = paymentBatchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found"));

        if (!"DRAFT".equals(batch.getStatus())) {
            throw new IllegalStateException("Can only add payments to DRAFT batches");
        }

        Invoice statement = invoiceRepository.findById(statementId)
                .orElseThrow(() -> new IllegalArgumentException("Statement not found"));

        if (!("POSTED".equals(statement.getStatus()) || "LOCKED".equals(statement.getStatus()))) {
            throw new IllegalStateException("Statement must be POSTED or LOCKED for payment");
        }

        PaymentMethod method = paymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> new IllegalArgumentException("Payment method not found"));

        Payment payment = Payment.builder()
                .statement(statement)
                .paymentBatch(batch)
                .amount(amount)
                .paymentDate(paymentDate)
                .paymentMethod(method)
                .referenceNumber(referenceNumber)
                .notes(notes)
                .status("PENDING")
                .createdBy(userId)
                .build();

        paymentRepository.save(payment);
        batch.addPayment(payment);
        paymentBatchRepository.save(batch);

        return payment;
    }

    /**
     * Edit payment in DRAFT batch
     */
    @Transactional
    public Payment updatePayment(Long paymentId, BigDecimal amount, String referenceNumber, 
                                 String notes, Long userId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        if (!"DRAFT".equals(payment.getPaymentBatch().getStatus())) {
            throw new IllegalStateException("Can only edit payments in DRAFT batches");
        }

        payment.setAmount(amount);
        payment.setReferenceNumber(referenceNumber);
        payment.setNotes(notes);
        payment.setUpdatedBy(userId);

        paymentRepository.save(payment);

        // Recalculate batch totals
        payment.getPaymentBatch().recalculateTotals();
        paymentBatchRepository.save(payment.getPaymentBatch());

        return payment;
    }

    /**
     * Post a payment batch (DRAFT -> POSTED)
     * Validates all payments and locks for processing
     */
    @Transactional
    public void postPaymentBatch(Long batchId, Long userId) {
        PaymentBatch batch = paymentBatchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found"));

        if (!"DRAFT".equals(batch.getStatus())) {
            throw new IllegalStateException("Only DRAFT batches can be posted");
        }

        if (batch.getPayments().isEmpty()) {
            throw new IllegalStateException("Cannot post empty batch");
        }

        // Validate all payments
        for (Payment payment : batch.getPayments()) {
            if (!"PENDING".equals(payment.getStatus())) {
                throw new IllegalStateException("All payments must be PENDING");
            }

            // Lock statement if not already locked
            Invoice statement = payment.getStatement();
            if (statement.getStatus() == Invoice.InvoiceStatus.POSTED) {
                statement.setStatus(Invoice.InvoiceStatus.LOCKED);
                statement.setLockedAt(LocalDateTime.now());
                statement.setLockedBy(userId);
                invoiceRepository.save(statement);
            }
        }

        batch.post(userId);
        paymentBatchRepository.save(batch);
    }

    /**
     * Mark all payments in batch as completed
     * Transitions statements to PAID
     */
    @Transactional
    public void markBatchCompleted(Long batchId, Long userId) {
        PaymentBatch batch = paymentBatchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found"));

        if (!"POSTED".equals(batch.getStatus())) {
            throw new IllegalStateException("Only POSTED batches can be marked completed");
        }

        for (Payment payment : batch.getPayments()) {
            payment.markCompleted(userId);
            paymentRepository.save(payment);

            Invoice statement = payment.getStatement();
            statement.setStatus(Invoice.InvoiceStatus.PAID);
            invoiceRepository.save(statement);

            createAuditLog(statement, "PAID", "LOCKED", "PAID", null, userId);
        }

        batch.markProcessed(userId);
        paymentBatchRepository.save(batch);
    }

    /**
     * Create adjustment statement for post-payment corrections
     */
    @Transactional
    public Invoice createAdjustmentStatement(Long originalStatementId, BigDecimal adjustmentAmount, 
                                            String reason, Long userId) {
        Invoice original = invoiceRepository.findById(originalStatementId)
                .orElseThrow(() -> new IllegalArgumentException("Statement not found"));

        if (original.getStatus() != Invoice.InvoiceStatus.PAID) {
            throw new IllegalStateException("Can only create adjustments for PAID statements");
        }

        // Create new statement for adjustment (next day period)
        LocalDate nextDay = original.getBillingPeriodEnd().plusDays(1);

        Invoice adjustment = Invoice.builder()
                .customer(original.getCustomer())
                .invoiceNumber(original.getInvoiceNumber() + "-ADJ")
                .billingPeriodStart(nextDay)
                .billingPeriodEnd(nextDay)
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now())
                .subtotal(adjustmentAmount)
                .taxRate(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .totalAmount(adjustmentAmount)
                .balanceDue(adjustmentAmount)
                .previousBalance(original.getNetDue()) // Carry forward original net due
                .status(Invoice.InvoiceStatus.DRAFT)
                .statementVersion(1)
                .parentStatementId(original.getId())
                .createdBy(userId)
                .build();

        invoiceRepository.save(adjustment);

        createAuditLog(adjustment, "ADJUSTMENT_CREATED", null, "DRAFT", 
                "Adjustment: " + reason, userId);

        return adjustment;
    }

    /**
     * Get all payments for a statement
     */
    public List<Payment> getStatementPayments(Long statementId) {
        return paymentRepository.findByStatementId(statementId);
    }

    /**
     * Get payment batch details
     */
    public PaymentBatch getPaymentBatch(Long batchId) {
        return paymentBatchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found"));
    }

    /**
     * Get statement audit history
     */
    public List<StatementAuditLog> getStatementHistory(Long statementId) {
        return auditLogRepository.findByStatementId(statementId);
    }

    /**
     * Helper: Create audit log entry
     */
    private void createAuditLog(Invoice statement, String changeType, String previousStatus, 
                               String newStatus, String reason, Long userId) {
        StatementAuditLog log = StatementAuditLog.builder()
                .statement(statement)
                .changeType(changeType)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .changeDescription(changeType + " by user " + userId)
                .changedBy(userId)
                .reason(reason)
                .build();

        auditLogRepository.save(log);
    }
}
