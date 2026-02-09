package com.taxi.domain.cab.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * AttributeCost - Pricing for custom attributes assigned to shifts
 *
 * Tracks historical pricing for attributes with temporal support.
 * Allows different costs for the same attribute at different time periods.
 *
 * Example:
 * - Attribute: TRANSPONDER
 * - From Jan 1, 2026: $30/month
 * - From Mar 1, 2026: $35/month
 */
@Entity
@Table(name = "attribute_cost",
       indexes = {
           @Index(name = "idx_attribute_cost_attribute_type", columnList = "attribute_type_id"),
           @Index(name = "idx_attribute_cost_effective_from", columnList = "effective_from"),
           @Index(name = "idx_attribute_cost_effective_to", columnList = "effective_to"),
           @Index(name = "idx_attribute_cost_date_range", columnList = "effective_from, effective_to")
       },
       uniqueConstraints = {
           @UniqueConstraint(name = "uq_attribute_cost_date_range", columnNames = {"attribute_type_id", "effective_from"})
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"attributeType"})
@EqualsAndHashCode(of = "id")
public class AttributeCost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Reference to the attribute type this cost applies to
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "attribute_type_id", nullable = false)
    private CabAttributeType attributeType;

    /**
     * The cost amount
     * Example: 30.00 for $30
     */
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * How to bill for this attribute
     * MONTHLY: Charge once per month
     * DAILY: Charge per day
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "billing_unit", nullable = false, length = 20)
    private BillingUnit billingUnit;

    /**
     * Date when this cost becomes effective
     * Once set, cannot be changed (immutable)
     */
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    /**
     * Date when this cost stops being effective
     * NULL means cost is ongoing/active
     */
    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    /**
     * Audit fields
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "updated_by", length = 255)
    private String updatedBy;

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
     * Check if this cost is active on a specific date
     */
    public boolean isActiveOn(LocalDate date) {
        return !date.isBefore(effectiveFrom) &&
               (effectiveTo == null || !date.isAfter(effectiveTo));
    }

    /**
     * Check if this cost is currently active
     */
    public boolean isCurrentlyActive() {
        return isActiveOn(LocalDate.now());
    }

    /**
     * Billing unit enumeration
     */
    public enum BillingUnit {
        MONTHLY("Monthly"),
        DAILY("Daily");

        private final String displayName;

        BillingUnit(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
