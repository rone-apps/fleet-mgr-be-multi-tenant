package com.taxi.domain.account.model;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "payment_batch")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_number", nullable = false, unique = true, length = 50)
    private String batchNumber;

    @Column(name = "batch_date", nullable = false)
    private LocalDate batchDate;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "payment_count", nullable = false)
    @Builder.Default
    private Integer paymentCount = 0;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "DRAFT"; // DRAFT, POSTED, PROCESSING, PROCESSED

    @Column(name = "posted_at")
    private LocalDateTime postedAt;

    @Column(name = "posted_by")
    private Long postedBy;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "processed_by")
    private Long processedBy;

    @Column(name = "actual_payments_made", precision = 10, scale = 2)
    private BigDecimal actualPaymentsMade;

    @Column(name = "reconciliation_notes", columnDefinition = "TEXT")
    private String reconciliationNotes;

    @OneToMany(mappedBy = "paymentBatch", cascade = CascadeType.ALL, orphanRemoval = false)
    @JsonIgnore
    @Builder.Default
    private List<Payment> payments = new ArrayList<>();

    @Column(name = "statement_ids", columnDefinition = "TEXT")
    private String statementIds; // Comma-separated statement IDs for this batch

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void addPayment(Payment payment) {
        payments.add(payment);
        payment.setPaymentBatch(this);
        recalculateTotals();
    }

    public void recalculateTotals() {
        this.paymentCount = payments.size();
        this.totalAmount = payments.stream()
            .map(Payment::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void post(Long userId) {
        this.status = "POSTED";
        this.postedAt = LocalDateTime.now();
        this.postedBy = userId;
    }

    public void markProcessed(Long userId) {
        this.status = "PROCESSED";
        this.processedAt = LocalDateTime.now();
        this.processedBy = userId;

        // Mark all payments as completed
        payments.forEach(p -> p.markCompleted(userId));
    }
}
