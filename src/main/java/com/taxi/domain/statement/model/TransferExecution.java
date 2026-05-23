package com.taxi.domain.statement.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a specific calculated transfer execution for a statement period
 * Flows through approval workflow: PENDING → APPROVED → APPLIED → FINALIZED
 *
 * Once FINALIZED, this record is immutable and preserves historical integrity
 */
@Entity
@Table(name = "statement_transfer_execution")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "execution_number", unique = true, nullable = false, length = 50)
    private String executionNumber; // Format: EXEC-2026-05-18-abc123

    // Link to configuration
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_config_id", nullable = false)
    private StatementBalanceTransfer transferConfig;

    @Column(name = "config_transfer_number", length = 50)
    private String configTransferNumber; // Denormalized for display

    // Specific period
    @Column(name = "period_from", nullable = false)
    private LocalDate periodFrom;

    @Column(name = "period_to", nullable = false)
    private LocalDate periodTo;

    // Calculated details
    @Column(name = "calculated_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal calculatedAmount;

    @Column(name = "source_balance_snapshot", precision = 10, scale = 2)
    private BigDecimal sourceBalanceSnapshot; // Balance when calculated

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_statement_id_snapshot")
    private Statement sourceStatementSnapshot; // Which statement balance came from

    @Column(name = "calculation_date", nullable = false)
    private LocalDateTime calculationDate;

    @Column(name = "calculated_by")
    private Long calculatedBy;

    @Column(name = "calculation_notes", columnDefinition = "TEXT")
    private String calculationNotes;

    // Person details (denormalized for immutability)
    @Column(name = "source_person_id", nullable = false)
    private Long sourcePersonId;

    @Column(name = "source_person_name")
    private String sourcePersonName;

    @Column(name = "target_person_id", nullable = false)
    private Long targetPersonId;

    @Column(name = "target_person_name")
    private String targetPersonName;

    // Status workflow
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ExecutionStatus status = ExecutionStatus.PENDING;

    // Approval tracking
    @Column(name = "approved_date")
    private LocalDateTime approvedDate;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approval_notes", columnDefinition = "TEXT")
    private String approvalNotes;

    // Rejection tracking
    @Column(name = "rejected_date")
    private LocalDateTime rejectedDate;

    @Column(name = "rejected_by")
    private Long rejectedBy;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    // Application tracking (when statement generated)
    @Column(name = "applied_date")
    private LocalDateTime appliedDate;

    @Column(name = "applied_by")
    private Long appliedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_statement_id")
    private Statement sourceStatement; // Statement where this appears as expense

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_statement_id")
    private Statement targetStatement; // Statement where this appears as revenue

    // Finalization tracking
    @Column(name = "finalized_date")
    private LocalDateTime finalizedDate;

    // Audit
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Business Methods

    /**
     * Approve this execution
     * @param userId User who approved
     * @param notes Optional approval notes
     */
    public void approve(Long userId, String notes) {
        if (this.status != ExecutionStatus.PENDING && this.status != ExecutionStatus.REJECTED) {
            throw new IllegalStateException(
                String.format("Can only approve PENDING or REJECTED executions. Current status: %s", this.status)
            );
        }
        this.status = ExecutionStatus.APPROVED;
        this.approvedDate = LocalDateTime.now();
        this.approvedBy = userId;
        this.approvalNotes = notes;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Reject this execution
     * @param userId User who rejected
     * @param reason Rejection reason
     */
    public void reject(Long userId, String reason) {
        if (this.status != ExecutionStatus.PENDING) {
            throw new IllegalStateException(
                String.format("Can only reject PENDING executions. Current status: %s", this.status)
            );
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }
        this.status = ExecutionStatus.REJECTED;
        this.rejectedDate = LocalDateTime.now();
        this.rejectedBy = userId;
        this.rejectionReason = reason;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Mark this execution as applied to statements
     * @param source Source statement (where this appears as expense)
     * @param target Target statement (where this appears as revenue)
     * @param userId User who applied
     */
    public void applyToStatements(Statement source, Statement target, Long userId) {
        if (this.status != ExecutionStatus.APPROVED) {
            throw new IllegalStateException(
                String.format("Can only apply APPROVED executions. Current status: %s", this.status)
            );
        }
        this.status = ExecutionStatus.APPLIED;
        this.appliedDate = LocalDateTime.now();
        this.appliedBy = userId;
        if (source != null) {
            this.sourceStatement = source;
        }
        if (target != null) {
            this.targetStatement = target;
        }
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Mark this execution as finalized (immutable)
     */
    public void finalize() {
        if (this.status != ExecutionStatus.APPLIED) {
            throw new IllegalStateException(
                String.format("Can only finalize APPLIED executions. Current status: %s", this.status)
            );
        }
        this.status = ExecutionStatus.FINALIZED;
        this.finalizedDate = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Check if this execution can be modified
     * @return true if status is PENDING or REJECTED, false if APPROVED, APPLIED, or FINALIZED
     */
    public boolean canModify() {
        return this.status == ExecutionStatus.PENDING || this.status == ExecutionStatus.REJECTED;
    }

    /**
     * Check if this execution is immutable (finalized)
     * @return true if status is FINALIZED
     */
    public boolean isFinalized() {
        return this.status == ExecutionStatus.FINALIZED;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.calculationDate == null) {
            this.calculationDate = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
