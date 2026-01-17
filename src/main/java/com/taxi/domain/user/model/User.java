package com.taxi.domain.user.model;

import com.taxi.domain.driver.model.Driver;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * User entity - Authentication and Authorization
 * Separate from Driver entity for better separation of concerns
 * 
 * Users can be:
 * - Drivers (linked to Driver entity)
 * - Office staff (accountants, dispatchers, managers)
 * - Admins
 */
@Entity
@Table(name = "user",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"username"}),
           @UniqueConstraint(columnNames = {"email"})
       },
       indexes = {
           @Index(name = "idx_user_username", columnList = "username"),
           @Index(name = "idx_user_email", columnList = "email"),
           @Index(name = "idx_user_role", columnList = "role")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"password"})
@EqualsAndHashCode(of = "id")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password", nullable = false, length = 255)
    private String password;  // BCrypt encrypted password

    @Column(name = "email", unique = true, length = 100)
    private String email;

    @Column(name = "first_name", length = 50)
    private String firstName;

    @Column(name = "last_name", length = 50)
    private String lastName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private UserRole role = UserRole.DRIVER;

    @Column(name = "is_active")
    @Builder.Default
    private boolean isActive = true;

    // Optional link to driver if this user is a driver
    @OneToOne
    @JoinColumn(name = "driver_id")
    private Driver driver;

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
     * User Roles for authorization
     */
    public enum UserRole {
        ADMIN("Administrator", "Full system access"),
        DRIVER("Driver", "Can operate shifts and view own data"),
        ACCOUNTANT("Accountant", "Manages expenses and financials"),
        DISPATCHER("Dispatcher", "Manages shift assignments"),
        MANAGER("Manager", "Oversees operations and reports"),
        VIEWER("Viewer", "Read-only access");

        private final String displayName;
        private final String description;

        UserRole(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Business logic: Check if user is admin
     */
    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    /**
     * Business logic: Check if user is a driver
     */
    public boolean isDriver() {
        return role == UserRole.DRIVER && driver != null;
    }

    /**
     * Business logic: Check if user has role
     */
    public boolean hasRole(UserRole role) {
        return this.role == role;
    }

    /**
     * Business logic: Get full name
     */
    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        }
        return username;
    }

    /**
     * Business logic: Activate user
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * Business logic: Deactivate user
     */
    public void deactivate() {
        this.isActive = false;
    }
}
