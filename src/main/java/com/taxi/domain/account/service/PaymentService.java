package com.taxi.domain.account.service;

import com.taxi.domain.account.model.*;
import com.taxi.domain.account.repository.*;
import com.taxi.domain.account.model.AccountCredit;
import com.taxi.domain.statement.model.Statement;
import com.taxi.domain.statement.repository.StatementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final StatementPaymentRepository statementPaymentRepository;
    private final PaymentBatchRepository paymentBatchRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final StatementAuditLogRepository auditLogRepository;
    private final InvoiceRepository invoiceRepository;
    private final StatementRepository statementRepository;
    private final AccountChargeRepository accountChargeRepository;
    private final AccountCreditRepository accountCreditRepository;

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
        log.info("Creating payment batch - batchDate: {}, periodStart: {}, periodEnd: {}, userId: {}",
                 batchDate, periodStart, periodEnd, userId);

        String batchNumber = "BATCH-" + LocalDate.now() + "-" + UUID.randomUUID().toString().substring(0, 8);

        PaymentBatch batch = PaymentBatch.builder()
                .batchNumber(batchNumber)
                .batchDate(batchDate)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .status("DRAFT")
                .createdBy(userId)
                .build();

        log.info("Payment batch created: {}, period: {} to {}", batch.getBatchNumber(), batch.getPeriodStart(), batch.getPeriodEnd());
        return paymentBatchRepository.save(batch);
    }

    /**
     * Add statement payment to batch (for driver/owner payments)
     */
    @Transactional
    public StatementPayment addPaymentToBatch(Long batchId, Long statementId, BigDecimal amount,
                                               LocalDate paymentDate, Long paymentMethodId,
                                               String referenceNumber, String notes, Long userId) {

        PaymentBatch batch = paymentBatchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found"));

        if (!"DRAFT".equals(batch.getStatus())) {
            throw new IllegalStateException("Can only add payments to DRAFT batches");
        }

        // Validate statement exists
        Statement statement = statementRepository.findById(statementId)
                .orElseThrow(() -> new IllegalArgumentException("Statement not found: " + statementId));

        // Validate payment method exists
        PaymentMethod paymentMethod = paymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> new IllegalArgumentException("Payment method not found: " + paymentMethodId));

        // Create StatementPayment record for driver/owner payment
        StatementPayment payment = new StatementPayment();
        payment.setPaymentBatch(batch);
        payment.setStatementId(statementId);
        payment.setPersonId(statement.getPersonId());
        payment.setPersonType(statement.getPersonType());
        payment.setPersonName(statement.getPersonName());
        payment.setPaymentDate(paymentDate);
        payment.setAmount(amount);
        payment.setPaymentMethod(paymentMethod);
        payment.setReferenceNumber(referenceNumber);
        payment.setNotes(notes);
        payment.setStatus("PENDING");
        payment.setCreatedBy(userId);
        payment.setCreatedAt(LocalDateTime.now());

        // Save the payment record
        StatementPayment savedPayment = statementPaymentRepository.save(payment);

        log.info("Statement payment created for {} {} in batch {}: {} {} {}",
                 statement.getPersonType(), statement.getPersonName(), batchId,
                 savedPayment.getPaymentNumber(), amount, paymentMethod.getMethodCode());

        return savedPayment;
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

        // For Statement-based payments, batch may not have Payment records yet
        // This is acceptable as Payment records will be created separately
        if (batch.getPayments().isEmpty()) {
            log.info("Batch {} has no Payment records yet - this is expected for Statement-based batches", batchId);
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
     * Mark all statement payments in batch as completed
     * Transitions statements to PAID and updates paid_amount
     */
    @Transactional
    public void markBatchCompleted(Long batchId, Long userId, Map<String, Object> paymentData) {
        PaymentBatch batch = paymentBatchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found"));

        if (!"POSTED".equals(batch.getStatus())) {
            throw new IllegalStateException("Only POSTED batches can be marked completed");
        }

        // Get all statement payments for this batch
        List<StatementPayment> statementPayments = statementPaymentRepository.findByPaymentBatchId(batchId);

        // Update statement payment records to COMPLETED
        for (StatementPayment payment : statementPayments) {
            payment.setStatus("COMPLETED");
            payment.setPostedAt(LocalDateTime.now());
            payment.setPostedBy(userId);
            statementPaymentRepository.save(payment);
        }

        // Extract payment amounts by statement ID from request body
        Map<Long, BigDecimal> paymentsByStatement = new HashMap<>();
        if (paymentData != null && paymentData.containsKey("paymentsByStatement")) {
            Map<String, Object> payments_map = (Map<String, Object>) paymentData.get("paymentsByStatement");
            for (Map.Entry<String, Object> entry : payments_map.entrySet()) {
                try {
                    Long statementId = Long.parseLong(entry.getKey());
                    BigDecimal amount = new BigDecimal(entry.getValue().toString());
                    paymentsByStatement.put(statementId, amount);
                } catch (NumberFormatException e) {
                    log.warn("Invalid statement ID in payment data: {}", entry.getKey());
                }
            }
        }

        // Update statements using the statement IDs stored in the batch
        if (batch.getStatementIds() != null && !batch.getStatementIds().isEmpty()) {
            String[] statementIdArray = batch.getStatementIds().split(",");

            for (String statementIdStr : statementIdArray) {
                try {
                    Long statementId = Long.parseLong(statementIdStr.trim());
                    Statement statement = statementRepository.findById(statementId)
                            .orElseThrow(() -> new IllegalArgumentException("Statement not found: " + statementId));

                    // Get paid amount from the frontend data
                    BigDecimal paidAmount = paymentsByStatement.getOrDefault(statementId, BigDecimal.ZERO);

                    if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
                        statement.setPaidAmount(paidAmount);
                    }

                    // Mark statement as paid
                    statement.setStatus(com.taxi.domain.statement.model.StatementStatus.PAID);
                    statementRepository.save(statement);

                    log.info("Statement {} marked as PAID with paid amount {}", statementId, paidAmount);
                } catch (NumberFormatException e) {
                    log.warn("Invalid statement ID in batch: {}", statementIdStr);
                }
            }
        }

        batch.markProcessed(userId);
        paymentBatchRepository.save(batch);

        log.info("Batch {} completed. {} statement payments processed", batchId, statementPayments.size());
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
     * Record payment for account customer invoice
     * Creates a Payment record and marks associated AccountCharges as paid
     */
    @Transactional
    public Payment recordPaymentForInvoice(Long invoiceId, BigDecimal amount, LocalDate paymentDate,
                                         Long paymentMethodId, String referenceNumber, String notes, Long userId) {
        // Get the invoice
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));

        log.info("===== PAYMENT RECORDING START =====");
        log.info("Invoice ID: {}, Number: {}, Current Status: {}", invoiceId, invoice.getInvoiceNumber(), invoice.getStatus());
        log.info("Invoice before payment - Total: {}, AmountPaid: {}, BalanceDue: {}",
                invoice.getTotalAmount(), invoice.getAmountPaid(), invoice.getBalanceDue());

        // Check if invoice is already fully paid
        if (invoice.getStatus() == Invoice.InvoiceStatus.PAID) {
            throw new IllegalStateException("Invoice " + invoice.getInvoiceNumber() +
                    " is already fully paid. No additional payments can be recorded.");
        }

        // Check if payment amount exceeds remaining balance
        BigDecimal balanceDue = invoice.getBalanceDue();
        if (amount.compareTo(balanceDue) > 0) {
            log.warn("Overpayment detected for invoice {}. Amount: {}, Balance due: {}. " +
                    "Excess will be recorded as account credit.", invoiceId, amount, balanceDue);
        }

        // Get the payment method
        PaymentMethod paymentMethod = paymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> new IllegalArgumentException("Payment method not found: " + paymentMethodId));

        // Create payment record
        Payment payment = Payment.builder()
                .statement(invoice)
                .invoiceId(invoiceId)
                .customerId(invoice.getCustomer() != null ? invoice.getCustomer().getId() : null)
                .accountId(invoice.getAccountId())
                .amount(amount)
                .paymentDate(paymentDate)
                .paymentMethod(paymentMethod)
                .paymentMethodName(paymentMethod.getMethodName())
                .referenceNumber(referenceNumber)
                .notes(notes)
                .status("PENDING")
                .createdBy(userId)
                .build();

        payment = paymentRepository.save(payment);
        log.info("Recorded payment {} for invoice {} (amount: {}, method: {})",
                payment.getPaymentNumber(), invoiceId, amount, paymentMethod.getMethodName());

        // Distribute payment to charges (mark charges as paid based on payment amount)
        List<AccountCharge> charges = accountChargeRepository.findByInvoiceId(invoiceId);
        BigDecimal remainingPayment = amount;
        BigDecimal overpaymentAmount = BigDecimal.ZERO;

        // Sort charges by creation date to process in order
        charges.sort((c1, c2) -> c1.getId().compareTo(c2.getId()));

        for (AccountCharge charge : charges) {
            if (remainingPayment.compareTo(BigDecimal.ZERO) <= 0) {
                break; // No more payment to distribute
            }

            BigDecimal chargeAmount = charge.getTotalAmount();

            // Check if this charge is not already marked as paid
            if (!charge.isPaid()) {
                // If remaining payment covers this charge
                if (remainingPayment.compareTo(chargeAmount) >= 0) {
                    charge.markAsPaid(payment.getPaymentNumber());
                    remainingPayment = remainingPayment.subtract(chargeAmount);
                    log.info("Marked charge {} as paid. Remaining payment: {}", charge.getId(), remainingPayment);
                } else {
                    // Partial payment on this charge - don't mark as fully paid
                    log.info("Partial payment on charge {}. Amount: {}, Remaining: {}",
                            charge.getId(), charge.getTotalAmount(), remainingPayment);
                }
            }

            accountChargeRepository.save(charge);
        }

        // Update invoice amount_paid and balance_due
        invoice.setAmountPaid(invoice.getAmountPaid().add(amount));
        BigDecimal newBalanceDue = invoice.getTotalAmount().subtract(invoice.getAmountPaid());

        // Check for overpayment
        if (newBalanceDue.compareTo(BigDecimal.ZERO) < 0) {
            overpaymentAmount = newBalanceDue.abs();
            invoice.setBalanceDue(BigDecimal.ZERO);
            log.info("Overpayment detected: {}", overpaymentAmount);
        } else {
            invoice.setBalanceDue(newBalanceDue);
        }

        // Update invoice status based on balance
        log.info("Before updateStatus() - BalanceDue: {}, Status: {}", invoice.getBalanceDue(), invoice.getStatus());
        invoice.updateStatus();
        log.info("After updateStatus() - BalanceDue: {}, Status: {}", invoice.getBalanceDue(), invoice.getStatus());

        invoiceRepository.save(invoice);
        log.info("===== INVOICE SAVED =====");
        log.info("Updated invoice {} - Total: {}, AmountPaid: {}, BalanceDue: {}, Status: {}",
                invoiceId, invoice.getTotalAmount(), invoice.getAmountPaid(), invoice.getBalanceDue(), invoice.getStatus());
        log.info("===== PAYMENT RECORDING END =====");

        // Create account credit if there's overpayment
        if (overpaymentAmount.compareTo(BigDecimal.ZERO) > 0) {
            AccountCredit credit = AccountCredit.builder()
                    .accountId(invoice.getAccountId())
                    .customer(invoice.getCustomer())
                    .creditAmount(overpaymentAmount)
                    .remainingAmount(overpaymentAmount)
                    .usedAmount(BigDecimal.ZERO)
                    .sourceType("OVERPAYMENT")
                    .sourceReference(payment.getPaymentNumber())
                    .invoiceId(invoiceId)
                    .paymentId(payment.getId())
                    .description("Overpayment on invoice " + invoice.getInvoiceNumber())
                    .createdBy(userId)
                    .isActive(true)
                    .build();

            accountCreditRepository.save(credit);
            log.info("Created account credit {} for overpayment: {}", credit.getId(), overpaymentAmount);
        }

        return payment;
    }

    /**
     * Record payment for account customer charge
     * Creates a Payment record and marks the AccountCharge as paid
     */
    @Transactional
    public Payment recordAccountChargePayment(Long chargeId, BigDecimal amount, LocalDate paymentDate,
                                            Long paymentMethodId, String referenceNumber, String notes, Long userId) {
        // Get the account charge
        AccountCharge charge = accountChargeRepository.findById(chargeId)
                .orElseThrow(() -> new IllegalArgumentException("Account charge not found: " + chargeId));

        // Get the invoice associated with this charge
        Invoice invoice = invoiceRepository.findById(charge.getInvoiceId())
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found for charge: " + chargeId));

        // Get the payment method
        PaymentMethod paymentMethod = paymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> new IllegalArgumentException("Payment method not found: " + paymentMethodId));

        // Create payment record
        Payment payment = Payment.builder()
                .statement(invoice)
                .invoiceId(charge.getInvoiceId())
                .customerId(invoice.getCustomer() != null ? invoice.getCustomer().getId() : null)
                .accountId(invoice.getAccountId())
                .amount(amount)
                .paymentDate(paymentDate)
                .paymentMethod(paymentMethod)
                .paymentMethodName(paymentMethod.getMethodName())
                .referenceNumber(referenceNumber)
                .notes(notes)
                .status("PENDING")
                .createdBy(userId)
                .build();

        payment = paymentRepository.save(payment);
        log.info("Recorded payment {} for account charge {} (amount: {}, method: {})",
                payment.getPaymentNumber(), chargeId, amount, paymentMethod.getMethodName());

        // Mark charge as paid
        charge.markAsPaid(payment.getPaymentNumber());
        accountChargeRepository.save(charge);

        // Update invoice amount_paid and balance_due
        invoice.setAmountPaid(invoice.getAmountPaid().add(amount));
        invoice.setBalanceDue(invoice.getTotalAmount().subtract(invoice.getAmountPaid()));
        invoiceRepository.save(invoice);
        log.info("Updated invoice {} amount_paid to {}, balance_due to {}",
                charge.getInvoiceId(), invoice.getAmountPaid(), invoice.getBalanceDue());

        return payment;
    }

    /**
     * Record batch payments for multiple account customer charges
     * Creates multiple Payment records and marks all AccountCharges as paid
     */
    @Transactional
    public List<Payment> recordBatchAccountChargePayments(List<com.taxi.domain.account.dto.AccountChargePaymentRequest> paymentRequests, Long userId) {
        List<Payment> payments = new ArrayList<>();

        for (com.taxi.domain.account.dto.AccountChargePaymentRequest request : paymentRequests) {
            Payment payment = recordAccountChargePayment(
                    request.getChargeId(),
                    request.getAmount(),
                    request.getPaymentDate(),
                    request.getPaymentMethodId(),
                    request.getReferenceNumber(),
                    request.getNotes(),
                    userId
            );
            payments.add(payment);
        }

        log.info("Recorded {} batch payments for account charges", payments.size());
        return payments;
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
