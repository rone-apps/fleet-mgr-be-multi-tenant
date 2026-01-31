package com.taxi.web.controller;

import com.taxi.domain.driver.model.Driver;
import com.taxi.domain.driver.repository.DriverRepository;
import com.taxi.domain.user.model.User;
import com.taxi.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * User Management Controller
 * Handles CRUD operations for users with driver linking support
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class UserController {

    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Get all users (Admin and Super Admin only)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers(@AuthenticationPrincipal UserDetails userDetails) {
        log.info("Fetching all users");

        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Current user not found"));

        List<User> users;
        if (currentUser.isSuperAdmin()) {
            users = userRepository.findAll(); // Super admins see all
        } else {
            users = userRepository.findAllExcludingSuperAdmins(); // Admins don't see super admins
        }

        List<UserResponse> responses = users.stream()
                .map(UserResponse::new)
                .toList();
        return ResponseEntity.ok(responses);
    }

    /**
     * Get user by ID (Admin and Super Admin only)
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> getUserById(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Fetching user by ID: {}", id);

        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));

        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Current user not found"));

        if (!currentUser.canSeeUser(targetUser)) {
            log.warn("User {} attempted to access restricted user {}", currentUser.getUsername(), id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Access denied"));
        }

        return ResponseEntity.ok(new UserResponse(targetUser));
    }

    /**
     * Create new user with driver linking support
     * POST /api/users
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest request,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Creating new user: {}", request.getUsername());

        // Get current user
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Current user not found"));

        // Only SUPER_ADMIN can create SUPER_ADMIN users
        if (request.getRole() == User.UserRole.SUPER_ADMIN && !currentUser.isSuperAdmin()) {
            log.warn("Regular admin {} attempted to create super admin user", currentUser.getUsername());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Only super administrators can create super admin accounts"));
        }

        // Check if username exists
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Username is already taken"));
        }

        // Check if email exists
        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Email is already in use"));
        }

        // ✅ Handle driver linking for DRIVER role
        Driver driver = null;
        if (request.getRole() == User.UserRole.DRIVER) {
            if (request.getDriverId() == null) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Driver ID is required for DRIVER role"));
            }

            // Find the driver
            driver = driverRepository.findById(request.getDriverId())
                    .orElseThrow(() -> new RuntimeException("Driver not found with ID: " + request.getDriverId()));

            // Check if driver is already linked to another user
            if (userRepository.existsByDriver(driver)) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("This driver is already linked to another user account"));
            }

            log.info("Linking user to existing driver: {} ({})",
                    driver.getDriverNumber(), driver.getFirstName() + " " + driver.getLastName());
        }

        // Create user
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .role(request.getRole() != null ? request.getRole() : User.UserRole.DRIVER)
                .driver(driver)  // ✅ Link to driver
                .isActive(true)
                .build();

        user = userRepository.save(user);
        log.info("User created successfully with ID: {}, Role: {}", user.getId(), user.getRole());

        return ResponseEntity.status(HttpStatus.CREATED).body(new UserResponse(user));
    }

    /**
     * Update user
     * PUT /api/users/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Updating user ID: {}", id);

        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));

        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Current user not found"));

        // Prevent regular admins from modifying super admins
        if (targetUser.isSuperAdmin() && !currentUser.isSuperAdmin()) {
            log.warn("Regular admin {} attempted to modify super admin {}",
                    currentUser.getUsername(), targetUser.getUsername());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Cannot modify super administrator accounts"));
        }

        // Prevent promoting users to SUPER_ADMIN unless current user is SUPER_ADMIN
        if (request.getRole() == User.UserRole.SUPER_ADMIN && !currentUser.isSuperAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Only super administrators can assign super admin role"));
        }

        // Update email if changed and not duplicate
        if (request.getEmail() != null && !request.getEmail().equals(targetUser.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Email is already in use"));
            }
            targetUser.setEmail(request.getEmail());
        }

        // Update other fields
        targetUser.setFirstName(request.getFirstName());
        targetUser.setLastName(request.getLastName());
        targetUser.setPhone(request.getPhone());
        targetUser.setRole(request.getRole());

        // Update password if provided
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            targetUser.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        targetUser = userRepository.save(targetUser);
        log.info("User updated successfully: {}", targetUser.getUsername());

        return ResponseEntity.ok(new UserResponse(targetUser));
    }

    /**
     * Toggle user active status
     * PUT /api/users/{id}/toggle-active
     */
    @PutMapping("/{id}/toggle-active")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> toggleActive(@PathVariable Long id,
                                         @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Toggling active status for user ID: {}", id);

        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));

        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Current user not found"));

        // Prevent regular admins from toggling super admin status
        if (targetUser.isSuperAdmin() && !currentUser.isSuperAdmin()) {
            log.warn("Regular admin {} attempted to toggle super admin {}",
                    currentUser.getUsername(), targetUser.getUsername());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Cannot modify super administrator accounts"));
        }

        targetUser.setActive(!targetUser.isActive());
        targetUser = userRepository.save(targetUser);

        log.info("User {} status changed to: {}", targetUser.getUsername(), targetUser.isActive() ? "ACTIVE" : "INACTIVE");

        return ResponseEntity.ok(new UserResponse(targetUser));
    }

    /**
     * Link driver to user (for existing users)
     * PUT /api/users/{userId}/link-driver/{driverId}
     */
    @PutMapping("/{userId}/link-driver/{driverId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> linkDriverToUser(
            @PathVariable Long userId,
            @PathVariable Long driverId,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Linking driver {} to user {}", driverId, userId);

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Current user not found"));

        // Prevent regular admins from modifying super admins
        if (targetUser.isSuperAdmin() && !currentUser.isSuperAdmin()) {
            log.warn("Regular admin {} attempted to modify super admin {}",
                    currentUser.getUsername(), targetUser.getUsername());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Cannot modify super administrator accounts"));
        }

        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found with ID: " + driverId));

        // Check if driver is already linked to another user
        if (userRepository.existsByDriver(driver)) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("This driver is already linked to another user account"));
        }

        targetUser.setDriver(driver);
        userRepository.save(targetUser);

        log.info("Successfully linked driver {} to user {}", driver.getDriverNumber(), targetUser.getUsername());

        return ResponseEntity.ok(new UserResponse(targetUser));
    }

    // ===== DTOs =====

    /**
     * Request DTO for creating a user
     */
    public static class CreateUserRequest {
        @NotBlank
        @Size(min = 3, max = 50)
        private String username;

        @NotBlank
        @Size(min = 6, max = 100)
        private String password;

        @Email
        private String email;

        @NotBlank
        private String firstName;

        @NotBlank
        private String lastName;

        private String phone;

        private User.UserRole role;

        // ✅ NEW: Driver ID for linking
        private Long driverId;

        // Getters and Setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public User.UserRole getRole() { return role; }
        public void setRole(User.UserRole role) { this.role = role; }

        public Long getDriverId() { return driverId; }
        public void setDriverId(Long driverId) { this.driverId = driverId; }
    }

    /**
     * Request DTO for updating a user
     */
    public static class UpdateUserRequest {
        @Email
        private String email;

        @NotBlank
        private String firstName;

        @NotBlank
        private String lastName;

        private String phone;

        private User.UserRole role;

        // Password is optional for updates
        private String password;

        // Getters and Setters
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public User.UserRole getRole() { return role; }
        public void setRole(User.UserRole role) { this.role = role; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    /**
     * Response DTO for user
     */
    public static class UserResponse {
        private Long id;
        private String username;
        private String email;
        private String firstName;
        private String lastName;
        private String phone;
        private String role;
        private boolean isActive;
        private DriverInfo driver;

        public UserResponse(User user) {
            this.id = user.getId();
            this.username = user.getUsername();
            this.email = user.getEmail();
            this.firstName = user.getFirstName();
            this.lastName = user.getLastName();
            this.phone = user.getPhone();
            this.role = user.getRole().toString();
            this.isActive = user.isActive();
            
            if (user.getDriver() != null) {
                this.driver = new DriverInfo(user.getDriver());
            }
        }

        // Getters
        public Long getId() { return id; }
        public String getUsername() { return username; }
        public String getEmail() { return email; }
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public String getPhone() { return phone; }
        public String getRole() { return role; }
        public boolean isActive() { return isActive; }
        public DriverInfo getDriver() { return driver; }
    }

    /**
     * Nested DTO for driver info in user response
     */
    public static class DriverInfo {
        private Long id;
        private String driverNumber;
        private String firstName;
        private String lastName;

        public DriverInfo(Driver driver) {
            this.id = driver.getId();
            this.driverNumber = driver.getDriverNumber();
            this.firstName = driver.getFirstName();
            this.lastName = driver.getLastName();
        }

        // Getters
        public Long getId() { return id; }
        public String getDriverNumber() { return driverNumber; }
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
    }

    /**
     * Error response DTO
     */
    public static class ErrorResponse {
        private String message;

        public ErrorResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}