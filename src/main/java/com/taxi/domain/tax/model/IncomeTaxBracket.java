package com.taxi.domain.tax.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "income_tax_bracket",
       indexes = {
           @Index(name = "idx_bracket_year_jurisdiction", columnList = "tax_year, jurisdiction"),
           @Index(name = "idx_bracket_order", columnList = "bracket_order")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class IncomeTaxBracket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tax_year", nullable = false)
    private Integer taxYear;

    @Column(name = "jurisdiction", nullable = false, length = 10)
    private String jurisdiction;  // FEDERAL or province code (AB, BC, ON, QC, etc.)

    @Column(name = "bracket_order", nullable = false)
    private Integer bracketOrder;

    @Column(name = "min_income", nullable = false, precision = 12, scale = 2)
    private BigDecimal minIncome;

    @Column(name = "max_income", precision = 12, scale = 2)
    private BigDecimal maxIncome;  // NULL = unlimited

    @Column(name = "rate", nullable = false, precision = 6, scale = 4)
    private BigDecimal rate;  // e.g., 0.1500 for 15%

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
