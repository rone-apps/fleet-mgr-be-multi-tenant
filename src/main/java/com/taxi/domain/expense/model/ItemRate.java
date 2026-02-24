package com.taxi.domain.expense.model;

import com.taxi.domain.profile.model.ItemRateChargedTo;
import com.taxi.domain.profile.model.ItemRateUnitType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ItemRate - Global per-unit expense rates (base/default rates)
 *
 * Defines system-wide rates for activity-based expenses (mileage, airport trips, etc.)
 * These are the default rates that can be overridden per owner or shift via ItemRateOverride.
 *
 * Example:
 * - "Per Mile Insurance" - $0.10 per mile, charged to DRIVER
 * - "Airport Trip Fee" - $5.00 per airport trip, charged to OWNER
 */
@Entity
@Table(name = "item_rate",
       indexes = {
           @Index(name = "idx_item_rate_active", columnList = "is_active"),
           @Index(name = "idx_item_rate_unit_type", columnList = "unit_type"),
           @Index(name = "idx_item_rate_effective_date", columnList = "effective_from, effective_to")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = "id")
public class ItemRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;  // e.g., "Per Mile Insurance", "Airport Trip Fee"

    @Enumerated(EnumType.STRING)
    @Column(name = "unit_type", nullable = false, length = 30)
    private ItemRateUnitType unitType;  // MILEAGE, AIRPORT_TRIP

    @Column(name = "rate", nullable = false, precision = 10, scale = 4)
    private BigDecimal rate;  // e.g., 0.10 for $0.10 per unit

    @Enumerated(EnumType.STRING)
    @Column(name = "charged_to", nullable = false, length = 10)
    private ItemRateChargedTo chargedTo;  // DRIVER or OWNER

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;  // NULL = no end date (still active)

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "notes", length = 1000)
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

    /**
     * Check if this rate is active on a given date
     */
    public boolean isActiveOnDate(LocalDate date) {
        if (!isActive) return false;
        if (date.isBefore(effectiveFrom)) return false;
        if (effectiveTo != null && date.isAfter(effectiveTo)) return false;
        return true;
    }
}
