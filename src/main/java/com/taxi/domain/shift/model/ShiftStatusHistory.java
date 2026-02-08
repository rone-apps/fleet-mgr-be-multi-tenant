package com.taxi.domain.shift.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ShiftStatusHistory - Audit trail for shift active/inactive status changes
 *
 * Tracks when shifts became active or inactive with full historical record.
 * Purpose: Enable historical queries like "was shift X active on date Y?"
 *
 * Key Design:
 * - effective_from: Date when this status became effective
 * - effective_to: Date when this status ended (NULL = current/ongoing status)
 * - Only one record per shift should have effective_to = NULL at any time
 * - Allows generating reports that respect shift status at any point in time
 */
@Entity
@Table(name = "shift_status_history",
       indexes = {
           @Index(name = "idx_shift_status_shift_id", columnList = "shift_id"),
           @Index(name = "idx_shift_status_dates", columnList = "effective_from, effective_to"),
           @Index(name = "idx_shift_status_active", columnList = "shift_id, is_active, effective_from, effective_to")
       })
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"shift"})
@EqualsAndHashCode(of = "id")
public class ShiftStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private CabShift shift;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "changed_by", length = 100)
    private String changedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Business logic: Check if shift is active on a specific date
     *
     * @param date The date to check
     * @return true if shift is active on that date, false otherwise
     */
    public boolean isActiveOn(LocalDate date) {
        return Boolean.TRUE.equals(isActive)
            && !date.isBefore(effectiveFrom)
            && (effectiveTo == null || !date.isAfter(effectiveTo));
    }

    /**
     * Business logic: Check if this is the current status
     *
     * @return true if effective_to is null (current status), false if historical
     */
    public boolean isCurrent() {
        return effectiveTo == null;
    }
}
