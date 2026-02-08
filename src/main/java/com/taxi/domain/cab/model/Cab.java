package com.taxi.domain.cab.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.taxi.domain.driver.model.Driver;
import com.taxi.domain.shift.model.CabShift;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDate;

/**
 * Cab entity - represents a physical taxi vehicle
 */
@Entity
@Table(name = "cab",
       uniqueConstraints = @UniqueConstraint(columnNames = {"cab_number"}),
       indexes = {
           @Index(name = "idx_cab_owner_driver", columnList = "owner_driver_id")
       })
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = "id")
public class Cab {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cab_number", nullable = false, unique = true, length = 20)
    private String cabNumber;  // Business identifier (e.g., "CAB-001")

    @Column(name = "registration_number", nullable = false, length = 50)
    private String registrationNumber;

    @Column(name = "make", length = 50)
    private String make;  // Toyota, Honda, etc.

    @Column(name = "model", length = 50)
    private String model;  // Camry, Civic, etc.

    @Column(name = "year")
    private Integer year;

    @Column(name = "color", length = 30)
    private String color;

    // Current owner driver (nullable - cab might be company-owned)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_driver_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Driver ownerDriver;

    // ============================================================================
    // Shifts relationship
    // Every cab has exactly 2 shifts: DAY and NIGHT
    // Attributes and status are now tracked at the shift level, not the cab level
    // ============================================================================
    @OneToMany(mappedBy = "cab", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CabShift> shifts;

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
     * Business logic: Check if cab is company-owned (no owner driver)
     */
    public boolean isCompanyOwned() {
        return ownerDriver == null;
    }

    /**
     * Get shifts that are active on a specific date
     * Filters shifts based on their historical status
     *
     * @param date The date to check
     * @return List of shifts that were active on the given date
     */
    public List<CabShift> getActiveShiftsOn(LocalDate date) {
        if (shifts == null || shifts.isEmpty()) {
            return List.of();
        }
        return shifts.stream()
            .filter(s -> s.isActiveOn(date))
            .collect(Collectors.toList());
    }

    /**
     * Get all shifts that are currently active
     *
     * @return List of currently active shifts
     */
    public List<CabShift> getCurrentActiveShifts() {
        if (shifts == null || shifts.isEmpty()) {
            return List.of();
        }
        return shifts.stream()
            .filter(CabShift::isCurrentlyActive)
            .collect(Collectors.toList());
    }
}
