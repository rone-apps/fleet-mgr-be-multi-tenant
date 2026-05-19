package com.taxi.domain.statement.model;

import lombok.*;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Statement Balance Transfer History
 *
 * Records each application of a balance transfer to a specific statement period.
 * Provides complete audit trail of when and how transfers were applied.
 * Supports reversal tracking for corrections.
 */
@Entity
@Table(name = "statement_balance_transfer_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatementBalanceTransferHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_id", nullable = false)
    private StatementBalanceTransfer transfer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_statement_id")
    private Statement sourceStatement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_statement_id")
    private Statement targetStatement;

    @Column(name = "transfer_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal transferAmount;

    @Column(name = "applied_period_from", nullable = false)
    private LocalDate appliedPeriodFrom;

    @Column(name = "applied_period_to", nullable = false)
    private LocalDate appliedPeriodTo;

    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt;

    @Column(name = "applied_by")
    private Long appliedBy;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // Reversal tracking
    @Column(name = "is_reversed")
    @Builder.Default
    private Boolean isReversed = false;

    @Column(name = "reversed_at")
    private LocalDateTime reversedAt;

    @Column(name = "reversed_by")
    private Long reversedBy;

    @Column(name = "reversal_reason", columnDefinition = "TEXT")
    private String reversalReason;

    /**
     * Reverse this transfer application
     */
    public void reverse(Long userId, String reason) {
        this.isReversed = true;
        this.reversedAt = LocalDateTime.now();
        this.reversedBy = userId;
        this.reversalReason = reason;
    }

    @PrePersist
    protected void onCreate() {
        if (appliedAt == null) {
            appliedAt = LocalDateTime.now();
        }
        if (isReversed == null) {
            isReversed = false;
        }
    }
}
