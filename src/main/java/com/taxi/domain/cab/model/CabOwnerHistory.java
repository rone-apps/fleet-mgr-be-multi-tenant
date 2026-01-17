package com.taxi.domain.cab.model;

import com.taxi.domain.driver.model.Driver;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Tracks ownership history of cabs
 * When a cab is assigned to an owner (driver marked as owner), we record it here
 */
@Entity
@Table(name = "cab_owner_history",
       indexes = {
           @Index(name = "idx_cab_owner_cab_id", columnList = "cab_id"),
           @Index(name = "idx_cab_owner_driver_id", columnList = "owner_driver_id"),
           @Index(name = "idx_cab_owner_start_date", columnList = "start_date")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"cab", "ownerDriver"})
@EqualsAndHashCode(of = "id")
public class CabOwnerHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cab_id", nullable = false)
    private Cab cab;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_driver_id", nullable = false)
    private Driver ownerDriver;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;  // null means current owner

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
     * Check if this is a current ownership (no end date)
     */
    public boolean isCurrent() {
        return endDate == null;
    }
}
