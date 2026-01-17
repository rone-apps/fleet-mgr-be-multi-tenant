package com.taxi.domain.driver.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Driver entity - represents a person who can drive or own shifts
 */
@Entity
@Table(name = "driver", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"driver_number"}),
       indexes = {
           @Index(name = "idx_driver_status", columnList = "status"),
           @Index(name = "idx_driver_name", columnList = "last_name, first_name")
       })
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"password"})
@EqualsAndHashCode(of = "id")
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "driver_number", nullable = false, unique = true, length = 20)
    private String driverNumber;  // Business identifier (e.g., "DRV-001")

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "license_number", length = 50)
    private String licenseNumber;

    @Column(name = "license_expiry")
    private LocalDate licenseExpiry;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "address", length = 500)
    private String address;

    // Authentication fields
    @Column(name = "username", unique = true, length = 50)
    private String username;

    @Column(name = "password", length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private DriverStatus status = DriverStatus.ACTIVE;

    @Column(name = "is_admin")
    @Builder.Default
    private boolean isAdmin = false;

    @Column(name = "is_owner", nullable = false)
    @Builder.Default
    private Boolean isOwner = false;

    @Column(name = "joined_date")
    private LocalDate joinedDate;

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
        if (joinedDate == null) {
            joinedDate = LocalDate.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Business logic: Get full name
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Business logic: Check if license is expired
     */
    public boolean isLicenseExpired() {
        return licenseExpiry != null && licenseExpiry.isBefore(LocalDate.now());
    }

    /**
     * Business logic: Activate driver
     */
    public void activate() {
        this.status = DriverStatus.ACTIVE;
    }

    /**
     * Business logic: Suspend driver
     */
    public void suspend() {
        this.status = DriverStatus.SUSPENDED;
    }

    /**
     * Business logic: Terminate driver
     */
    public void terminate() {
        this.status = DriverStatus.TERMINATED;
    }

    public enum DriverStatus {
        ACTIVE,
        INACTIVE,
        SUSPENDED,
        TERMINATED
    }
}
