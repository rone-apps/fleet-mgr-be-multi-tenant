package com.taxi.domain.cab.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * CabAttributeValue - Temporal tracking of cab attributes
 * Records when a cab had/has a specific attribute with date range support
 * Maintains complete history of all attribute assignments
 *
 * Pattern: startDate/endDate with NULL endDate = currently active
 * Following CabOwnerHistory temporal tracking pattern
 */
@Entity
@Table(name = "cab_attribute_value",
       indexes = {
           @Index(name = "idx_attr_val_cab", columnList = "cab_id"),
           @Index(name = "idx_attr_val_type", columnList = "attribute_type_id"),
           @Index(name = "idx_attr_val_dates", columnList = "start_date, end_date"),
           @Index(name = "idx_attr_val_cab_type", columnList = "cab_id, attribute_type_id"),
           @Index(name = "idx_attr_val_current", columnList = "cab_id, end_date")
       })
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"cab", "attributeType"})
@EqualsAndHashCode(of = "id")
public class CabAttributeValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cab_id", nullable = false)
    private Cab cab;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "attribute_type_id", nullable = false)
    private CabAttributeType attributeType;

    // Optional value (e.g., license number, transponder ID)
    @Column(name = "attribute_value", length = 255)
    private String attributeValue;

    // Temporal tracking with startDate/endDate pattern
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    // NULL endDate = currently active
    // Following CabOwnerHistory pattern for temporal tracking
    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "updated_by", length = 50)
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
     * Check if this attribute assignment is currently active
     * Following CabOwnerHistory pattern
     */
    public boolean isCurrent() {
        return endDate == null;
    }

    /**
     * Check if this attribute is active on a specific date
     * Following RecurringExpense.isEffectiveOn() pattern
     */
    public boolean isActiveOn(LocalDate date) {
        if (date.isBefore(startDate)) {
            return false;
        }
        return endDate == null || !date.isAfter(endDate);
    }

    /**
     * Check if this attribute overlaps with a date range
     * Used for validation to prevent duplicate active attributes
     */
    public boolean overlaps(LocalDate rangeStart, LocalDate rangeEnd) {
        // This ends before range starts
        if (endDate != null && endDate.isBefore(rangeStart)) {
            return false;
        }
        // This starts after range ends
        if (rangeEnd != null && startDate.isAfter(rangeEnd)) {
            return false;
        }
        return true;
    }
}
