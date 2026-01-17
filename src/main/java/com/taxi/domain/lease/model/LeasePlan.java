package com.taxi.domain.lease.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Aggregate Root for managing lease rate plans
 * Different plans can be active for different time periods
 * This allows historical rate tracking for accurate reporting
 * 
 * IMPORTANT BUSINESS RULES:
 * 1. Plans cannot be deleted once created (audit trail)
 * 2. Rates cannot be edited once set (historical accuracy)
 * 3. To change rates: create new plan and close current one
 * 4. Only one plan can be active at a time (no overlaps)
 * 5. Plans auto-deactivate when end date is reached
 */
@Entity
@Table(name = "lease_plan", indexes = {
        @Index(name = "idx_plan_dates", columnList = "effective_from, effective_to"),
        @Index(name = "idx_plan_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "leaseRates")
@EqualsAndHashCode(of = "id")
public class LeasePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_name", nullable = false, length = 100)
    private String planName;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "notes", length = 500)
    private String notes;

    @JsonManagedReference
    @OneToMany(mappedBy = "leasePlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LeaseRate> leaseRates = new ArrayList<>();

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
        
        // Auto-deactivate if end date has passed
        if (effectiveTo != null && effectiveTo.isBefore(LocalDate.now()) && isActive) {
            isActive = false;
        }
    }

    /**
     * Business logic: Add a lease rate to this plan
     */
    public void addLeaseRate(LeaseRate leaseRate) {
        leaseRates.add(leaseRate);
        leaseRate.setLeasePlan(this);
    }

    /**
     * Business logic: Remove a lease rate from this plan
     */
    public void removeLeaseRate(LeaseRate leaseRate) {
        leaseRates.remove(leaseRate);
        leaseRate.setLeasePlan(null);
    }

    /**
     * Business logic: Check if plan is active for a given date
     */
    public boolean isActiveOn(LocalDate date) {
        if (!isActive) {
            return false;
        }
        boolean afterStart = !date.isBefore(effectiveFrom);
        boolean beforeEnd = effectiveTo == null || !date.isAfter(effectiveTo);
        return afterStart && beforeEnd;
    }

    /**
     * Business logic: Deactivate this plan with end date
     */
    public void deactivate(LocalDate endDate) {
        this.isActive = false;
        this.effectiveTo = endDate;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Business logic: Check if this plan overlaps with another plan's dates
     */
    public boolean overlapsWith(LocalDate otherStart, LocalDate otherEnd) {
        // If this plan has no end date, check if other starts before this ends (never)
        if (this.effectiveTo == null) {
            return !otherStart.isBefore(this.effectiveFrom);
        }
        
        // If other has no end date
        if (otherEnd == null) {
            return !this.effectiveTo.isBefore(otherStart);
        }
        
        // Both have end dates - check for overlap
        return !(otherEnd.isBefore(this.effectiveFrom) || otherStart.isAfter(this.effectiveTo));
    }
}
