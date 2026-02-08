package com.taxi.domain.shift.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.taxi.domain.cab.model.Cab;
import com.taxi.domain.cab.model.CabType;
import com.taxi.domain.cab.model.ShareType;
import com.taxi.domain.driver.model.Driver;
import com.taxi.domain.profile.model.ShiftProfile;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * CabShift - represents one shift (Morning or Evening) for a specific cab
 * Business Rule: Every cab has exactly 2 shifts
 * Business Rule: Every shift must have an owner
 */
@Entity
@Table(name = "cab_shift", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"cab_id", "shift_type"}),
       indexes = {
           @Index(name = "idx_shift_owner", columnList = "current_owner_id"),
           @Index(name = "idx_shift_status", columnList = "status")
       })
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"cab", "currentOwner", "currentProfile", "profileAssignments"})
@EqualsAndHashCode(of = "id")
public class CabShift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cab_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Cab cab;

    @Enumerated(EnumType.STRING)
    @Column(name = "shift_type", nullable = false, length = 20)
    private ShiftType shiftType;

    // Editable shift times (can vary per cab)
    @Column(name = "start_time", nullable = false, length = 10)
    private String startTime;  // e.g., "06:00", "07:00"

    @Column(name = "end_time", nullable = false, length = 10)
    private String endTime;    // e.g., "18:00", "19:00"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_owner_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Driver currentOwner;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ShiftStatus status = ShiftStatus.ACTIVE;

    @Column(name = "notes", length = 500)
    private String notes;

    // ============================================================================
    // Attributes moved from Cab level to Shift level (Part of Phase 1 refactoring)
    // These attributes can now be different for DAY and NIGHT shifts of the same cab
    // ============================================================================

    @Enumerated(EnumType.STRING)
    @Column(name = "cab_type", length = 20)
    private CabType cabType;  // SEDAN, HANDICAP_VAN

    @Enumerated(EnumType.STRING)
    @Column(name = "share_type", length = 20)
    private ShareType shareType;  // VOTING_SHARE, NON_VOTING_SHARE

    @Column(name = "has_airport_license")
    @Builder.Default
    private Boolean hasAirportLicense = false;

    @Column(name = "airport_license_number", length = 50)
    private String airportLicenseNumber;

    @Column(name = "airport_license_expiry")
    private LocalDate airportLicenseExpiry;

    // ============================================================================
    // Shift Profile relationship (Denormalized for performance)
    // Points to the current active profile assignment
    // Use ShiftProfileAssignment for full history
    // ============================================================================

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "current_profile_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private ShiftProfile currentProfile;

    @OneToMany(mappedBy = "shift", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("startDate DESC")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private java.util.List<com.taxi.domain.profile.model.ShiftProfileAssignment> profileAssignments;

    // ============================================================================
    // Status history relationship
    // Tracks all historical status changes (active/inactive) for this shift
    // ============================================================================

    @OneToMany(mappedBy = "shift", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("effectiveFrom DESC")
    private List<ShiftStatusHistory> statusHistory;

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
     * Business logic: Transfer ownership to new owner
     */
    public void transferOwnership(Driver newOwner) {
        if (newOwner == null) {
            throw new IllegalArgumentException("New owner cannot be null");
        }
        this.currentOwner = newOwner;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Business logic: Deactivate shift
     */
    public void deactivate() {
        this.status = ShiftStatus.INACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Business logic: Activate shift
     */
    public void activate() {
        this.status = ShiftStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    // ============================================================================
    // Shift Status History Helper Methods
    // These methods provide convenient access to historical status information
    // ============================================================================

    /**
     * Check if shift is active on a specific date
     * Uses the status history to determine if shift was active at that point in time
     *
     * @param date The date to check
     * @return true if shift was active on that date, false otherwise
     */
    public boolean isActiveOn(LocalDate date) {
        if (statusHistory == null || statusHistory.isEmpty()) {
            return false;
        }
        return statusHistory.stream()
            .anyMatch(h -> h.isActiveOn(date));
    }

    /**
     * Get the current status record (where effective_to is NULL)
     *
     * @return The current status record, or null if no status history exists
     */
    public ShiftStatusHistory getCurrentStatus() {
        if (statusHistory == null || statusHistory.isEmpty()) {
            return null;
        }
        return statusHistory.stream()
            .filter(h -> h.getEffectiveTo() == null)
            .findFirst()
            .orElse(null);
    }

    /**
     * Check if shift is currently active
     * Convenience method that checks the current status record
     *
     * @return true if the current status is active, false otherwise
     */
    public boolean isCurrentlyActive() {
        ShiftStatusHistory current = getCurrentStatus();
        return current != null && Boolean.TRUE.equals(current.getIsActive());
    }

    /**
     * Business logic: Check if airport license is expired
     *
     * @return true if has license and it's expired, false otherwise
     */
    public boolean isAirportLicenseExpired() {
        return Boolean.TRUE.equals(hasAirportLicense)
            && airportLicenseExpiry != null
            && airportLicenseExpiry.isBefore(LocalDate.now());
    }

    public enum ShiftStatus {
        ACTIVE,
        INACTIVE
    }
}
