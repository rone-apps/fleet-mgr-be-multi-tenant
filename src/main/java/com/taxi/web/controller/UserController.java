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
     * Get all users (Admin only)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        log.info("Fetching all users");
        List<User> users = userRepository.findAll();
        return ResponseEntity.ok(users);
    }

    /**
     * Get user by ID (Admin only)
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        log.info("Fetching user by ID: {}", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));
        
        return ResponseEntity.ok(user);
    }

    /**
     * Create new user with driver linking support
     * POST /api/users
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("Creating new user: {}", request.getUsername());

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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        
        log.info("Updating user ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));

        // Update email if changed and not duplicate
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Email is already in use"));
            }
            user.setEmail(request.getEmail());
        }

        // Update other fields
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setRole(request.getRole());

        // Update password if provided
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        user = userRepository.save(user);
        log.info("User updated successfully: {}", user.getUsername());

        return ResponseEntity.ok(new UserResponse(user));
    }

    /**
     * Toggle user active status
     * PUT /api/users/{id}/toggle-active
     */
    @PutMapping("/{id}/toggle-active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleActive(@PathVariable Long id) {
        log.info("Toggling active status for user ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));

        user.setActive(!user.isActive());
        user = userRepository.save(user);

        log.info("User {} status changed to: {}", user.getUsername(), user.isActive() ? "ACTIVE" : "INACTIVE");

        return ResponseEntity.ok(new UserResponse(user));
    }

    /**
     * Link driver to user (for existing users)
     * PUT /api/users/{userId}/link-driver/{driverId}
     */
    @PutMapping("/{userId}/link-driver/{driverId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> linkDriverToUser(
            @PathVariable Long userId,
            @PathVariable Long driverId) {
        
        log.info("Linking driver {} to user {}", driverId, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found with ID: " + driverId));

        // Check if driver is already linked to another user
        if (userRepository.existsByDriver(driver)) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("This driver is already linked to another user account"));
        }

        user.setDriver(driver);
        userRepository.save(user);

        log.info("Successfully linked driver {} to user {}", driver.getDriverNumber(), user.getUsername());

        return ResponseEntity.ok(new UserResponse(user));
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