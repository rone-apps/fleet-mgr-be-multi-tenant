package com.taxi.domain.tax.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cpp_ei_rate")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class CppEiRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tax_year", nullable = false, unique = true)
    private Integer taxYear;

    @Column(name = "cpp_employee_rate", nullable = false, precision = 6, scale = 4)
    private BigDecimal cppEmployeeRate;  // e.g., 0.0595 for 5.95%

    @Column(name = "cpp_max_pensionable", nullable = false, precision = 12, scale = 2)
    private BigDecimal cppMaxPensionable;  // e.g., 68500.00

    @Column(name = "cpp_basic_exemption", nullable = false, precision = 12, scale = 2)
    private BigDecimal cppBasicExemption;  // e.g., 3500.00

    @Column(name = "ei_employee_rate", nullable = false, precision = 6, scale = 4)
    private BigDecimal eiEmployeeRate;  // e.g., 0.0166 for 1.66%

    @Column(name = "ei_max_insurable", nullable = false, precision = 12, scale = 2)
    private BigDecimal eiMaxInsurable;  // e.g., 63200.00

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
