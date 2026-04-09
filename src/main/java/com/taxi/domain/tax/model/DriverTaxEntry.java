package com.taxi.domain.tax.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "driver_tax_entry",
       indexes = {
           @Index(name = "idx_driver_tax_year", columnList = "driver_id, tax_year"),
           @Index(name = "idx_entry_type", columnList = "entry_type")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class DriverTaxEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "driver_id", nullable = false)
    private Long driverId;

    @Column(name = "driver_name", length = 255)
    private String driverName;

    @Column(name = "tax_year", nullable = false)
    private Integer taxYear;

    @Column(name = "entry_type", nullable = false, length = 30)
    private String entryType;  // T_SLIP, RRSP, DONATION, OTHER_DEDUCTION

    @Column(name = "slip_type", length = 20)
    private String slipType;  // T4, T4A, T4A-OAS, T5, T3, T4E, RL-1, RL-3, etc.

    @Column(name = "box_label", length = 100)
    private String boxLabel;  // e.g., "Box 14 - Employment Income"

    @Column(name = "issuer_name", length = 255)
    private String issuerName;  // Employer, bank, charity name, etc.

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
