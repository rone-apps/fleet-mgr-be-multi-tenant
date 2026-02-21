package com.taxi.domain.account.model;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "statement_payment")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatementPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_number", nullable = false, unique = true, length = 50)
    private String paymentNumber;

    @Column(name = "statement_id", nullable = false)
    private Long statementId;

    @Column(name = "person_id", nullable = false)
    private Long personId;

    @Column(name = "person_type", nullable = false, length = 20)
    private String personType; // DRIVER or OWNER

    @Column(name = "person_name", length = 255)
    private String personName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_batch_id")
    @JsonIgnore
    private PaymentBatch paymentBatch;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "payment_method_id", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING"; // PENDING, COMPLETED, REVERSED

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "posted_at")
    private LocalDateTime postedAt;

    @Column(name = "posted_by")
    private Long postedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        // Generate payment number if not set
        if (paymentNumber == null) {
            paymentNumber = "STPAY-" + LocalDate.now() + "-" + UUID.randomUUID().toString().substring(0, 8);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void markCompleted(Long completedBy) {
        this.status = "COMPLETED";
        this.postedAt = LocalDateTime.now();
        this.postedBy = completedBy;
    }

    public void reverse() {
        this.status = "REVERSED";
    }
}
