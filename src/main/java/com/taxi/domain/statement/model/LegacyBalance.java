package com.taxi.domain.statement.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing manually imported legacy balance data.
 *
 * This table stores manual balance entries as an alternative to the automatic
 * balance-forward system (SmartFleets AI). Balances are loaded from CSV imports
 * and used when the "Use SmartFleets AI" toggle is disabled in driver reports.
 *
 * Convention: Negative balances mean driver owes money TO the company.
 */
@Entity
@Table(name = "legacy_balance_owed")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"notes"})
@EqualsAndHashCode(of = {"driverNumber", "effectiveDate"})
public class LegacyBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Driver business identifier (not FK to allow flexible CSV imports)
     */
    @Column(name = "driver_number", nullable = false, length = 50)
    private String driverNumber;

    /**
     * Driver full name for reference
     */
    @Column(name = "driver_name")
    private String driverName;

    /**
     * Flag indicating if this person is an owner (vs regular driver)
     */
    @Column(name = "is_owner", nullable = false)
    @Builder.Default
    private Boolean isOwner = false;

    /**
     * Amount owed (negative = driver owes company, positive = company owes driver)
     * Convention matches Statement.previousBalance
     */
    @Column(name = "balance_owed", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceOwed;

    /**
     * Date when this balance became effective (allows historical snapshots)
     */
    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    /**
     * Source of this data (e.g., "CSV_IMPORT", "MANUAL_ENTRY")
     */
    @Column(name = "source", length = 50)
    @Builder.Default
    private String source = "CSV_IMPORT";

    /**
     * Optional notes or context
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", updatable = false)
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
