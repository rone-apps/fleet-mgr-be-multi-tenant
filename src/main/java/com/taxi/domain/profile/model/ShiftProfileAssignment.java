package com.taxi.domain.profile.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.taxi.domain.shift.model.CabShift;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ShiftProfileAssignment - Audit trail for shift profile assignments
 *
 * Tracks when a profile was assigned to a shift, including:
 * - start_date: When assignment became effective
 * - end_date: When assignment ended (NULL = currently active)
 * - reason: Why the assignment was made or changed
 * - assigned_by: User who made the assignment
 *
 * This creates a complete audit trail of all profile changes for a shift,
 * allowing you to see the full history of what profile was assigned at any given time.
 *
 * Example:
 * - 2024-01-01: STANDARD_SEDAN_VOTING assigned to shift
 * - 2024-06-15: PREMIUM_SEDAN_VOTING assigned (previous one ends)
 * - Current: PREMIUM_SEDAN_VOTING (end_date is NULL)
 */
@Entity
@Table(name = "shift_profile_assignment",
       uniqueConstraints = @UniqueConstraint(columnNames = {"shift_id", "end_date"}, name = "uk_shift_active_profile"),
       indexes = {
           @Index(name = "idx_assignment_shift", columnList = "shift_id"),
           @Index(name = "idx_assignment_profile", columnList = "profile_id"),
           @Index(name = "idx_assignment_dates", columnList = "start_date,end_date"),
           @Index(name = "idx_assignment_active", columnList = "shift_id,end_date")
       })
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"shift", "profile"})
@EqualsAndHashCode(of = "id")
public class ShiftProfileAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private CabShift shift;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "profile_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private ShiftProfile profile;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;  // NULL = currently active

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "assigned_by", length = 100)
    private String assignedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ============================================================================
    // Business Methods
    // ============================================================================

    /**
     * Check if this assignment is currently active
     */
    public boolean isActive() {
        return endDate == null;
    }

    /**
     * Check if this assignment was active on a specific date
     */
    public boolean wasActiveOn(LocalDate date) {
        return !startDate.isAfter(date) && (endDate == null || !endDate.isBefore(date));
    }

    /**
     * End this assignment (close it out)
     */
    public void endAssignment(LocalDate endingDate) {
        this.endDate = endingDate;
    }

    /**
     * End this assignment today
     */
    public void endAssignmentToday() {
        this.endDate = LocalDate.now();
    }
}
