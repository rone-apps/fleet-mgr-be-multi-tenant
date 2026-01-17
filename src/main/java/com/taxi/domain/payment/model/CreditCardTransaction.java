package com.taxi.domain.payment.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Credit Card Transaction entity (UPDATED to use business identifiers)
 * Uses cab_number and driver_number instead of foreign key IDs
 */
@Entity
@Table(name = "credit_card_transaction",
        uniqueConstraints = {
                // Primary uniqueness: terminal + auth code + amount + datetime
                @UniqueConstraint(
                        name = "uk_cc_transaction_primary",
                        columnNames = {"terminal_id", "authorization_code", "amount", "transaction_date", "transaction_time"}
                ),
                // Backup uniqueness: transaction_id from processor
                @UniqueConstraint(
                        name = "uk_cc_transaction_id",
                        columnNames = {"transaction_id"}
                ),
                // Additional safeguard: merchant + terminal + cab + amount + datetime
                @UniqueConstraint(
                        name = "uk_cc_transaction_secondary",
                        columnNames = {"merchant_id", "terminal_id", "cab_number", "amount", "transaction_date", "transaction_time"}
                )
        },
        indexes = {
                @Index(name = "idx_cc_transaction_date", columnList = "transaction_date"),
                @Index(name = "idx_cc_settlement_date", columnList = "settlement_date"),
                @Index(name = "idx_cc_driver_number", columnList = "driver_number"),
                @Index(name = "idx_cc_cab_number", columnList = "cab_number"),
                @Index(name = "idx_cc_status", columnList = "transaction_status"),
                @Index(name = "idx_cc_settled", columnList = "is_settled"),
                @Index(name = "idx_cc_merchant_terminal", columnList = "merchant_id, terminal_id"),
                @Index(name = "idx_cc_upload_batch", columnList = "upload_batch_id"),
                @Index(name = "idx_cc_auth_code", columnList = "authorization_code"),
                @Index(name = "idx_cc_job_id", columnList = "job_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = "id")
public class CreditCardTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Transaction Identification
    @Column(name = "transaction_id", nullable = false, length = 100)
    private String transactionId;

    @Column(name = "authorization_code", nullable = false, length = 50)
    private String authorizationCode;

    @Column(name = "merchant_id", nullable = false, length = 50)
    private String merchantId;

    @Column(name = "terminal_id", nullable = false, length = 50)
    private String terminalId;

    // Transaction Timing
    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "transaction_time", nullable = false)
    private LocalTime transactionTime;

    @Column(name = "settlement_date")
    private LocalDate settlementDate;

    // Card Details (Masked for PCI compliance)
    @Column(name = "card_type", length = 20)
    private String cardType; // VISA, MASTERCARD, AMEX, DISCOVER

    @Column(name = "card_last_four", length = 4)
    private String cardLastFour;

    @Column(name = "card_brand", length = 30)
    private String cardBrand;

    // Business Context (Using business identifiers instead of foreign key IDs)
    @Column(name = "cab_number", length = 20)
    private String cabNumber; // e.g., "CAB-001"

    @Column(name = "driver_number", length = 20)
    private String driverNumber; // e.g., "DRV-001" - may be null

    @Column(name = "job_id", length = 50)
    private String jobId;

    // Financial Details
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "tip_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal tipAmount = BigDecimal.ZERO;

    @Column(name = "processing_fee", precision = 10, scale = 2)
    private BigDecimal processingFee;

    // Transaction Status
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_status", nullable = false, length = 20)
    @Builder.Default
    private TransactionStatus transactionStatus = TransactionStatus.PENDING;

    @Column(name = "is_settled", nullable = false)
    @Builder.Default
    private Boolean isSettled = false;

    @Column(name = "is_refunded", nullable = false)
    @Builder.Default
    private Boolean isRefunded = false;

    @Column(name = "refund_amount", precision = 10, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "refund_date")
    private LocalDate refundDate;

    // Additional Transaction Details
    @Column(name = "batch_number", length = 50)
    private String batchNumber;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(name = "customer_name", length = 100)
    private String customerName;

    @Column(name = "receipt_number", length = 50)
    private String receiptNumber;

    // Upload/Import Tracking
    @Column(name = "upload_batch_id", length = 50)
    private String uploadBatchId;

    @Column(name = "upload_filename", length = 255)
    private String uploadFilename;

    @Column(name = "upload_date")
    private LocalDateTime uploadDate;

    // Metadata
    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Get combined transaction datetime
     */
    public LocalDateTime getTransactionDateTime() {
        if (transactionDate != null && transactionTime != null) {
            return LocalDateTime.of(transactionDate, transactionTime);
        }
        return null;
    }

    /**
     * Calculate total amount (amount + tip)
     */
    public BigDecimal getTotalAmount() {
        BigDecimal total = amount != null ? amount : BigDecimal.ZERO;
        BigDecimal tip = tipAmount != null ? tipAmount : BigDecimal.ZERO;
        return total.add(tip);
    }

    /**
     * Calculate net amount (total - processing fee)
     */
    public BigDecimal getNetAmount() {
        BigDecimal total = getTotalAmount();
        BigDecimal fee = processingFee != null ? processingFee : BigDecimal.ZERO;
        return total.subtract(fee);
    }

    /**
     * Check if transaction is complete (settled and not refunded)
     */
    public boolean isComplete() {
        return Boolean.TRUE.equals(isSettled) && Boolean.FALSE.equals(isRefunded);
    }

    /**
     * Transaction status enum
     */
    public enum TransactionStatus {
        PENDING,    // Transaction authorized but not settled
        SETTLED,    // Funds settled
        DECLINED,   // Transaction declined
        REFUNDED,   // Transaction refunded
        DISPUTED    // Under dispute/chargeback
    }
}