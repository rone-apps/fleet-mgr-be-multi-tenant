package com.taxi.domain.statement.model;

import com.taxi.domain.driver.model.Driver;
import lombok.*;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Statement Balance Transfer Configuration
 *
 * Allows drivers/owners to transfer their statement balances to other drivers/owners.
 * Transfers appear as line items in statements:
 * - Outgoing transfer (source) → OneTimeExpense with category BALANCE_TRANSFER_OUT
 * - Incoming transfer (target) → OtherRevenue with type BALANCE_TRANSFER_IN
 *
 * This design maintains statement immutability and provides complete transparency.
 */
@Entity
@Table(name = "statement_balance_transfer")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatementBalanceTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transfer_number", nullable = false, unique = true, length = 50)
    private String transferNumber;

    // Source (from whom balance is transferred)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_person_id", nullable = false)
    private Driver sourcePerson;

    @Column(name = "source_person_type", nullable = false, length = 20)
    private String sourcePersonType; // DRIVER or OWNER

    @Column(name = "source_person_name", length = 255)
    private String sourcePersonName;

    // Target (to whom balance is transferred)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_person_id", nullable = false)
    private Driver targetPerson;

    @Column(name = "target_person_type", nullable = false, length = 20)
    private String targetPersonType; // DRIVER or OWNER

    @Column(name = "target_person_name", length = 255)
    private String targetPersonName;

    // Configuration
    @Enumerated(EnumType.STRING)
    @Column(name = "transfer_type", nullable = false, length = 20)
    private TransferType transferType;

    @Enumerated(EnumType.STRING)
    @Column(name = "balance_direction", nullable = false, length = 20)
    private BalanceDirection balanceDirection;

    // Amounts
    @Column(name = "transfer_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal transferAmount;

    @Column(name = "transferred_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal transferredAmount = BigDecimal.ZERO;

    @Column(name = "remaining_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal remainingAmount;

    // Date range
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "statement_period_from")
    private LocalDate statementPeriodFrom;

    @Column(name = "statement_period_to")
    private LocalDate statementPeriodTo;

    // Status
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TransferStatus status = TransferStatus.ACTIVE;

    // Metadata
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "reason", length = 500)
    private String reason;

    // Audit fields
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancelled_by")
    private Long cancelledBy;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    /**
     * Check if this transfer is applicable for the given statement period
     */
    public boolean isApplicableForPeriod(LocalDate periodFrom, LocalDate periodTo) {
        if (status != TransferStatus.ACTIVE) {
            return false;
        }

        if (transferType == TransferType.ONE_TIME) {
            // ONE_TIME: must match exact statement period
            return statementPeriodFrom != null && statementPeriodTo != null
                    && statementPeriodFrom.equals(periodFrom)
                    && statementPeriodTo.equals(periodTo);
        } else {
            // RECURRING: check if period overlaps with transfer date range
            boolean startsBeforePeriodEnds = startDate.isBefore(periodTo) || startDate.equals(periodTo);
            boolean endsAfterPeriodStarts = endDate == null || endDate.isAfter(periodFrom) || endDate.equals(periodFrom);
            return startsBeforePeriodEnds && endsAfterPeriodStarts;
        }
    }

    /**
     * Apply transfer and update amounts
     * @param amount Amount being transferred in this application
     */
    public void applyTransfer(BigDecimal amount) {
        this.transferredAmount = this.transferredAmount.add(amount);
        this.remainingAmount = this.transferAmount.subtract(this.transferredAmount);
        this.updatedAt = LocalDateTime.now();

        // Mark as completed if fully transferred
        if (this.remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            this.status = TransferStatus.COMPLETED;
        }
    }

    /**
     * Cancel this transfer
     */
    public void cancel(Long userId, String reason) {
        this.status = TransferStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancelledBy = userId;
        this.cancellationReason = reason;
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = userId;
    }

    /**
     * Suspend this transfer
     */
    public void suspend() {
        if (this.status == TransferStatus.ACTIVE) {
            this.status = TransferStatus.SUSPENDED;
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * Resume this transfer from suspended state
     */
    public void resume() {
        if (this.status == TransferStatus.SUSPENDED) {
            this.status = TransferStatus.ACTIVE;
            this.updatedAt = LocalDateTime.now();
        }
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (transferNumber == null) {
            transferNumber = "XFER-" + LocalDate.now() + "-" + UUID.randomUUID().toString().substring(0, 8);
        }
        if (remainingAmount == null) {
            remainingAmount = transferAmount;
        }
        if (transferredAmount == null) {
            transferredAmount = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
