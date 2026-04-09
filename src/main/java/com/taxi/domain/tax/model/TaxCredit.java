package com.taxi.domain.tax.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tax_credit",
       indexes = {
           @Index(name = "idx_credit_year_jurisdiction", columnList = "tax_year, jurisdiction"),
           @Index(name = "idx_credit_code", columnList = "credit_code")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class TaxCredit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tax_year", nullable = false)
    private Integer taxYear;

    @Column(name = "jurisdiction", nullable = false, length = 10)
    private String jurisdiction;  // FEDERAL or province code

    @Column(name = "credit_code", nullable = false, length = 30)
    private String creditCode;  // BPA, AGE_AMOUNT, DISABILITY, CAREGIVER, DONATION_RATE_LOW, DONATION_RATE_HIGH

    @Column(name = "credit_name", nullable = false, length = 100)
    private String creditName;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;  // The base amount of the credit

    @Column(name = "rate", precision = 6, scale = 4)
    private BigDecimal rate;  // The rate to apply (e.g., 0.15 for federal basic rate)

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
