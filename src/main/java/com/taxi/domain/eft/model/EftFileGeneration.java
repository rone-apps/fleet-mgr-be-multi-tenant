package com.taxi.domain.eft.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "eft_file_generation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EftFileGeneration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    @Column(name = "file_creation_number", nullable = false)
    private Integer fileCreationNumber;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "record_count", nullable = false)
    @Builder.Default
    private Integer recordCount = 0;

    @Column(name = "total_credit_amount", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalCreditAmount = BigDecimal.ZERO;

    @Column(name = "total_debit_amount", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalDebitAmount = BigDecimal.ZERO;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "GENERATED";

    @Column(name = "generated_by", length = 100)
    private String generatedBy;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @PrePersist
    protected void onCreate() {
        if (generatedAt == null) generatedAt = LocalDateTime.now();
    }
}
