package com.taxi.domain.cab.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.taxi.domain.driver.model.Driver;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Cab entity - represents a physical taxi vehicle
 */
@Entity
@Table(name = "cab", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"cab_number"}),
       indexes = {
           @Index(name = "idx_cab_status", columnList = "status"),
           @Index(name = "idx_cab_type", columnList = "cab_type"),
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

    @Enumerated(EnumType.STRING)
    @Column(name = "cab_type", nullable = false, length = 20)
    private CabType cabType;

    @Enumerated(EnumType.STRING)
    @Column(name = "share_type", length = 20)
    private ShareType shareType;

    @Enumerated(EnumType.STRING)
    @Column(name = "cab_shift_type", length = 20)
    private CabShiftType cabShiftType;

    @Column(name = "has_airport_license")
    @Builder.Default
    private Boolean hasAirportLicense = false;

    @Column(name = "airport_license_number", length = 50)
    private String airportLicenseNumber;

    @Column(name = "airport_license_expiry")
    private LocalDate airportLicenseExpiry;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private CabStatus status = CabStatus.ACTIVE;

    // Current owner driver (nullable - cab might be company-owned)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_driver_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Driver ownerDriver;

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
     * Business logic: Check if airport license is expired
     */
    public boolean isAirportLicenseExpired() {
        return Boolean.TRUE.equals(hasAirportLicense) && 
               airportLicenseExpiry != null && 
               airportLicenseExpiry.isBefore(LocalDate.now());
    }

    /**
     * Business logic: Check if cab is company-owned (no owner driver)
     */
    public boolean isCompanyOwned() {
        return ownerDriver == null;
    }

    /**
     * Business logic: Activate cab
     */
    public void activate() {
        this.status = CabStatus.ACTIVE;
    }

    /**
     * Business logic: Put cab in maintenance
     */
    public void setMaintenance() {
        this.status = CabStatus.MAINTENANCE;
    }

    /**
     * Business logic: Retire cab
     */
    public void retire() {
        this.status = CabStatus.RETIRED;
    }

    public enum CabStatus {
        ACTIVE,
        MAINTENANCE,
        RETIRED
    }
}
