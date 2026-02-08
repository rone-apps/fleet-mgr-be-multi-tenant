package com.taxi.domain.profile.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.taxi.domain.cab.model.CabType;
import com.taxi.domain.cab.model.ShareType;
import com.taxi.domain.shift.model.ShiftType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ShiftProfile - Reusable bundle of shift attributes for categorization and matching
 *
 * Bundles static attributes (cab type, share type, airport license, shift type)
 * with custom dynamic attributes to create reusable profiles for:
 * 1. Categorizing shifts into operational groups
 * 2. Auto-matching expenses to applicable shifts via profile-based rules
 *
 * Example profiles:
 * - PREMIUM_SEDAN_VOTING: Sedan + Voting + Airport License
 * - HANDICAP_VAN_NON_VOTING: Handicap Van + Non-Voting
 * - DAY_SHIFT_SEDAN: Any Sedan during Day hours
 *
 * Static attributes are nullable: NULL means "any value acceptable"
 */
@Entity
@Table(name = "shift_profile",
       uniqueConstraints = @UniqueConstraint(columnNames = {"profile_code"}),
       indexes = {
           @Index(name = "idx_profile_code", columnList = "profile_code"),
           @Index(name = "idx_profile_active", columnList = "is_active"),
           @Index(name = "idx_profile_category", columnList = "category"),
           @Index(name = "idx_profile_cab_type", columnList = "cab_type"),
           @Index(name = "idx_profile_share_type", columnList = "share_type"),
           @Index(name = "idx_profile_shift_type", columnList = "shift_type"),
           @Index(name = "idx_profile_system", columnList = "is_system_profile")
       })
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"profileAttributes"})
@EqualsAndHashCode(of = "id")
public class ShiftProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "profile_code", nullable = false, unique = true, length = 50)
    private String profileCode;  // e.g., "PREMIUM_SEDAN_VOTING"

    @Column(name = "profile_name", nullable = false, length = 100)
    private String profileName;  // e.g., "Premium Sedan - Voting Share"

    @Column(name = "description", length = 500)
    private String description;

    // ============================================================================
    // Static Attributes (nullable = "any")
    // ============================================================================

    @Enumerated(EnumType.STRING)
    @Column(name = "cab_type", length = 20)
    private CabType cabType;  // NULL = accept any cab type

    @Enumerated(EnumType.STRING)
    @Column(name = "share_type", length = 20)
    private ShareType shareType;  // NULL = accept any share type

    @Column(name = "has_airport_license")
    private Boolean hasAirportLicense;  // NULL = don't care, TRUE = must have, FALSE = must not have

    @Enumerated(EnumType.STRING)
    @Column(name = "shift_type", length = 20)
    private ShiftType shiftType;  // NULL = accept any shift type

    // ============================================================================
    // Metadata
    // ============================================================================

    @Column(name = "category", length = 50)
    private String category;  // e.g., "STANDARD", "PREMIUM", "SPECIAL", "TIME_BASED"

    @Column(name = "color_code", length = 10)
    private String colorCode;  // e.g., "#3E5244" for UI display

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    // ============================================================================
    // Status
    // ============================================================================

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_system_profile")
    @Builder.Default
    private Boolean isSystemProfile = false;  // System profiles cannot be deleted

    @Column(name = "usage_count")
    @Builder.Default
    private Integer usageCount = 0;  // Number of shifts using this profile

    // ============================================================================
    // Dynamic Attributes
    // ============================================================================

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id ASC")
    private List<ShiftProfileAttribute> profileAttributes;

    // ============================================================================
    // Audit
    // ============================================================================

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
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

    // ============================================================================
    // Business Methods
    // ============================================================================

    /**
     * Activate profile for use in shift assignments
     */
    public void activate() {
        this.isActive = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Deactivate profile, preventing new assignments
     */
    public void deactivate() {
        this.isActive = false;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Increment usage count when a shift is assigned to this profile
     */
    public void incrementUsage() {
        if (this.usageCount == null) {
            this.usageCount = 0;
        }
        this.usageCount++;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Decrement usage count when a shift is removed from this profile
     */
    public void decrementUsage() {
        if (this.usageCount != null && this.usageCount > 0) {
            this.usageCount--;
        }
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Check if profile can be deleted
     * System profiles and profiles in use cannot be deleted
     */
    public boolean canBeDeleted() {
        return !Boolean.TRUE.equals(isSystemProfile)
                && (usageCount == null || usageCount == 0);
    }

    /**
     * Check if profile matches shift criteria
     * Returns true if shift's attributes match this profile's requirements
     */
    public boolean matchesShift(CabType shiftCabType, ShareType shiftShareType,
                                Boolean shiftHasAirportLicense, ShiftType shiftShiftType) {
        // Check cab type (NULL profile attribute = accept any)
        if (cabType != null && !cabType.equals(shiftCabType)) {
            return false;
        }

        // Check share type (NULL profile attribute = accept any)
        if (shareType != null && !shareType.equals(shiftShareType)) {
            return false;
        }

        // Check airport license (NULL profile attribute = don't care)
        if (hasAirportLicense != null && !hasAirportLicense.equals(shiftHasAirportLicense)) {
            return false;
        }

        // Check shift type (NULL profile attribute = accept any)
        if (shiftType != null && !shiftType.equals(shiftShiftType)) {
            return false;
        }

        return true;
    }
}
