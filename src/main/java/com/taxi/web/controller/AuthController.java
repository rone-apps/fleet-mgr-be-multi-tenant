package com.taxi.web.controller;

import com.taxi.domain.driver.model.Driver;
import com.taxi.domain.driver.repository.DriverRepository;
import com.taxi.domain.user.model.User;
import com.taxi.domain.user.repository.UserRepository;
import com.taxi.security.CustomUserDetailsService;
import com.taxi.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Authentication Controller
 * Handles user registration, login, and profile management
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final CustomUserDetailsService userDetailsService;

    /**
     * Register new user
     */
    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signupRequest) {
        log.info("Registration request for username: {}", signupRequest.getUsername());

        // Check if username exists
        if (userRepository.existsByUsername(signupRequest.getUsername())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Username is already taken!"));
        }

        // Check if email exists
        if (signupRequest.getEmail() != null && userRepository.existsByEmail(signupRequest.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Email is already in use!"));
        }

        // Create driver entity if user is a driver
        Driver driver = null;
        if (signupRequest.getRole() == User.UserRole.DRIVER) {
            // Generate driver number (e.g., DRV-001, DRV-002, etc.)
            String driverNumber = generateDriverNumber();
            
            driver = Driver.builder()
                    .driverNumber(driverNumber)
                    .firstName(signupRequest.getFirstName())
                    .lastName(signupRequest.getLastName())
                    .phone(signupRequest.getPhone())
                    .email(signupRequest.getEmail())
                    .licenseNumber(signupRequest.getLicenseNumber())
                    .status(Driver.DriverStatus.ACTIVE)
                    .build();
            driver = driverRepository.save(driver);
            log.info("Created driver entity with ID: {} and driver number: {}", driver.getId(), driver.getDriverNumber());
        }

        // Create user with encrypted password
        User user = User.builder()
                .username(signupRequest.getUsername())
                .password(passwordEncoder.encode(signupRequest.getPassword()))  // Encrypt password
                .email(signupRequest.getEmail())
                .firstName(signupRequest.getFirstName())
                .lastName(signupRequest.getLastName())
                .phone(signupRequest.getPhone())
                .role(signupRequest.getRole() != null ? signupRequest.getRole() : User.UserRole.DRIVER)
                .driver(driver)
                .isActive(true)
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully with ID: {}, Role: {}", user.getId(), user.getRole());

        return ResponseEntity.ok(new MessageResponse(
                "User registered successfully!",
                user.getId(),
                user.getUsername(),
                user.getRole().toString()
        ));
    }

    /**
     * Login user
     */
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Login attempt for username: {}", loginRequest.getUsername());

        // Get user details
        User user = userDetailsService.loadUserEntityByUsername(loginRequest.getUsername());

        // Authenticate user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Generate JWT token with role
        String jwt = jwtUtils.generateTokenWithUserIdAndRole(user.getUsername(), user.getId(), user.getRole().toString());

        log.info("User logged in successfully: {}, Role: {}", user.getUsername(), user.getRole());

        return ResponseEntity.ok(new JwtResponse(
                jwt,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().toString(),
                user.getDriver() != null ? user.getDriver().getId() : null
        ));
    }

    /**
     * Get current user info
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            log.warn("getCurrentUser called without authentication");
            return ResponseEntity.status(401).body(new MessageResponse("Unauthorized: No valid authentication"));
        }

        String username = userDetails.getUsername();
        User user = userDetailsService.loadUserEntityByUsername(username);

        log.info("Retrieved current user: {}, Role: {}", username, user.getRole());

        return ResponseEntity.ok(new UserInfoResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getRole().toString(),
                user.getDriver() != null ? user.getDriver().getId() : null,
                user.isActive()
        ));
    }

    // DTO Classes

    /**
     * Signup Request DTO
     */
    public static class SignupRequest {
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

        private String licenseNumber;

        private User.UserRole role;

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

        public String getLicenseNumber() { return licenseNumber; }
        public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }

        public User.UserRole getRole() { return role; }
        public void setRole(User.UserRole role) { this.role = role; }
    }

    /**
     * Login Request DTO
     */
    public static class LoginRequest {
        @NotBlank
        private String username;

        @NotBlank
        private String password;

        // Getters and Setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    /**
     * JWT Response DTO
     */
    public static class JwtResponse {
        private String token;
        private String type = "Bearer";
        private Long userId;
        private String username;
        private String email;
        private String firstName;
        private String lastName;
        private String role;
        private Long driverId;

        public JwtResponse(String token, Long userId, String username, String email, 
                          String firstName, String lastName, String role, Long driverId) {
            this.token = token;
            this.userId = userId;
            this.username = username;
            this.email = email;
            this.firstName = firstName;
            this.lastName = lastName;
            this.role = role;
            this.driverId = driverId;
        }

        // Getters
        public String getToken() { return token; }
        public String getType() { return type; }
        public Long getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getEmail() { return email; }
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public String getRole() { return role; }
        public Long getDriverId() { return driverId; }
    }

    /**
     * User Info Response DTO
     */
    public static class UserInfoResponse {
        private Long userId;
        private String username;
        private String email;
        private String firstName;
        private String lastName;
        private String phone;
        private String role;
        private Long driverId;
        private boolean isActive;

        public UserInfoResponse(Long userId, String username, String email, String firstName, 
                               String lastName, String phone, String role, Long driverId, boolean isActive) {
            this.userId = userId;
            this.username = username;
            this.email = email;
            this.firstName = firstName;
            this.lastName = lastName;
            this.phone = phone;
            this.role = role;
            this.driverId = driverId;
            this.isActive = isActive;
        }

        // Getters
        public Long getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getEmail() { return email; }
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public String getPhone() { return phone; }
        public String getRole() { return role; }
        public Long getDriverId() { return driverId; }
        public boolean isActive() { return isActive; }
    }

    /**
     * Message Response DTO
     */
    public static class MessageResponse {
        private String message;
        private Long userId;
        private String username;
        private String role;

        public MessageResponse(String message) {
            this.message = message;
        }

        public MessageResponse(String message, Long userId, String username, String role) {
            this.message = message;
            this.userId = userId;
            this.username = username;
            this.role = role;
        }

        // Getters
        public String getMessage() { return message; }
        public Long getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getRole() { return role; }
    }

    /**
     * Generate unique driver number
     * Format: DRV-001, DRV-002, etc.
     */
    private String generateDriverNumber() {
        // Get count of existing drivers
        long count = driverRepository.count();
        
        // Generate number with padding (DRV-001, DRV-002, etc.)
        String driverNumber;
        do {
            count++;
            driverNumber = String.format("DRV-%03d", count);
        } while (driverRepository.findByDriverNumber(driverNumber).isPresent());
        
        return driverNumber;
    }
}
