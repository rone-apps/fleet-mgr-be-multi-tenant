package com.taxi.domain.tax.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "driver_tax_profile",
       uniqueConstraints = @UniqueConstraint(columnNames = {"driver_id", "tax_year"}),
       indexes = @Index(name = "idx_driver_tax_profile", columnList = "driver_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class DriverTaxProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "driver_id", nullable = false)
    private Long driverId;

    @Column(name = "tax_year", nullable = false)
    private Integer taxYear;

    @Column(name = "province", nullable = false, length = 5)
    private String province;  // AB, BC, ON, QC, MB, SK, NS, NB, PE, NL, NT, NU, YT

    @Column(name = "language", nullable = false, length = 2)
    @Builder.Default
    private String language = "EN";  // EN or FR

    @Column(name = "marital_status", nullable = false, length = 20)
    @Builder.Default
    private String maritalStatus = "SINGLE";  // SINGLE, MARRIED, COMMON_LAW, DIVORCED, WIDOWED, SEPARATED

    @Column(name = "num_dependents", nullable = false)
    @Builder.Default
    private Integer numDependents = 0;

    @Column(name = "birth_year")
    private Integer birthYear;  // for 65+ age credit calculation

    @Column(name = "has_disability", nullable = false)
    @Builder.Default
    private Boolean hasDisability = false;

    @Column(name = "spouse_disability", nullable = false)
    @Builder.Default
    private Boolean spouseDisability = false;

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
