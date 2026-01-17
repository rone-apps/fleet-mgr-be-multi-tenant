package com.taxi.domain.shift.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.taxi.domain.cab.model.Cab;
import com.taxi.domain.driver.model.Driver;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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
@ToString(exclude = {"cab", "currentOwner"})
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

    public enum ShiftStatus {
        ACTIVE,
        INACTIVE
    }
}
