package com.taxi.domain.statement.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "statements")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Statement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long personId;                  // driver or owner ID
    private String personType;              // "DRIVER" or "OWNER"
    private String personName;

    private LocalDate periodFrom;
    private LocalDate periodTo;
    private LocalDateTime generatedDate;

    private BigDecimal totalRevenues;
    private BigDecimal totalRecurringExpenses;
    private BigDecimal totalOneTimeExpenses;
    private BigDecimal totalExpenses;
    private BigDecimal previousBalance;     // carried from prior statement
    private BigDecimal paidAmount;          // entered by admin
    private BigDecimal netDue;

    @Enumerated(EnumType.STRING)
    private StatementStatus status;         // DRAFT, FINALIZED

    @Column(columnDefinition = "LONGTEXT")
    private String lineItemsJson;           // serialized List<StatementLineItem> (stored as JSON)

    // audit
    private LocalDateTime createdAt;
    private String createdBy;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
