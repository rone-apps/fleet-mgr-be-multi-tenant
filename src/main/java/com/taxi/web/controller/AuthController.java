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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.newrelic.api.agent.NewRelic;

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
     * Register new user with detailed logging for New Relic
     */
    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signupRequest, HttpServletRequest request) {
        String clientIp = getClientIp(request);
        long signupStartTime = System.currentTimeMillis();

        try {
            log.info("=== SIGNUP ATTEMPT START ===");
            log.info("Username: {}", signupRequest.getUsername());
            log.info("Email: {}", signupRequest.getEmail());
            log.info("Role: {}", signupRequest.getRole());
            log.info("Client IP: {}", clientIp);
            log.info("Timestamp: {}", java.time.LocalDateTime.now());

            // Check if username exists
            if (userRepository.existsByUsername(signupRequest.getUsername())) {
                log.warn("Signup failed - Username already taken: {}", signupRequest.getUsername());
                NewRelic.addCustomParameter("signup.username", signupRequest.getUsername());
                NewRelic.addCustomParameter("signup.status", "FAILED");
                NewRelic.addCustomParameter("signup.reason", "Username already taken");
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Error: Username is already taken!"));
            }

            // Check if email exists
            if (signupRequest.getEmail() != null && userRepository.existsByEmail(signupRequest.getEmail())) {
                log.warn("Signup failed - Email already in use: {}", signupRequest.getEmail());
                NewRelic.addCustomParameter("signup.email", signupRequest.getEmail());
                NewRelic.addCustomParameter("signup.status", "FAILED");
                NewRelic.addCustomParameter("signup.reason", "Email already in use");
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

            long signupDuration = System.currentTimeMillis() - signupStartTime;

            log.info("=== SIGNUP SUCCESS ===");
            log.info("User ID: {}", user.getId());
            log.info("Username: {}", user.getUsername());
            log.info("Email: {}", user.getEmail());
            log.info("Role: {}", user.getRole());
            log.info("Client IP: {}", clientIp);
            log.info("Duration: {} ms", signupDuration);
            log.info("First Name: {}", user.getFirstName());
            log.info("Last Name: {}", user.getLastName());
            log.info("Timestamp: {}", java.time.LocalDateTime.now());

            // Send custom attributes to New Relic
            NewRelic.addCustomParameter("signup.userId", user.getId());
            NewRelic.addCustomParameter("signup.username", user.getUsername());
            NewRelic.addCustomParameter("signup.email", user.getEmail());
            NewRelic.addCustomParameter("signup.role", user.getRole().toString());
            NewRelic.addCustomParameter("signup.clientIp", clientIp);
            NewRelic.addCustomParameter("signup.duration_ms", signupDuration);
            NewRelic.addCustomParameter("signup.status", "SUCCESS");

            return ResponseEntity.ok(new MessageResponse(
                    "User registered successfully!",
                    user.getId(),
                    user.getUsername(),
                    user.getRole().toString()
            ));

        } catch (Exception e) {
            long signupDuration = System.currentTimeMillis() - signupStartTime;

            log.error("=== SIGNUP FAILED ===");
            log.error("Username: {}", signupRequest.getUsername());
            log.error("Email: {}", signupRequest.getEmail());
            log.error("Error: {}", e.getMessage());
            log.error("Client IP: {}", clientIp);
            log.error("Duration: {} ms", signupDuration);

            NewRelic.addCustomParameter("signup.username", signupRequest.getUsername());
            NewRelic.addCustomParameter("signup.email", signupRequest.getEmail());
            NewRelic.addCustomParameter("signup.error", e.getMessage());
            NewRelic.addCustomParameter("signup.status", "FAILED");

            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Signup failed. Please try again."));
        }
    }

    /**
     * Login user with detailed logging for New Relic
     */
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        String clientIp = getClientIp(request);
        long loginStartTime = System.currentTimeMillis();

        log.info("=== LOGIN ATTEMPT START ===");
        log.info("Username: {}", loginRequest.getUsername());
        log.info("Client IP: {}", clientIp);
        log.info("Timestamp: {}", java.time.LocalDateTime.now());

        try {
            // Authenticate user first (this validates username and password)
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Get user details AFTER successful authentication
            User user = userDetailsService.loadUserEntityByUsername(loginRequest.getUsername());

            // Generate JWT token with role
            String jwt = jwtUtils.generateTokenWithUserIdAndRole(user.getUsername(), user.getId(), user.getRole().toString());

            long loginDuration = System.currentTimeMillis() - loginStartTime;

            // Log successful login with all details
            log.warn("=== LOGIN SUCCESS ===");
            log.warn("Username: {}", user.getUsername());
            log.warn("User ID: {}", user.getId());
            log.info("Role: {}", user.getRole());
            log.info("Email: {}", user.getEmail());
            log.warn("Client IP: {}", clientIp);
            log.info("Duration: {} ms", loginDuration);
            log.warn("First Name: {}", user.getFirstName());
            log.info("Last Name: {}", user.getLastName());
            log.info("Is Active: {}", user.isActive());
            log.info("Timestamp: {}", java.time.LocalDateTime.now());

            // Send custom attributes to New Relic for monitoring
            NewRelic.addCustomParameter("login.username", user.getUsername());
            NewRelic.addCustomParameter("login.userId", user.getId());
            NewRelic.addCustomParameter("login.role", user.getRole().toString());
            NewRelic.addCustomParameter("login.email", user.getEmail());
            NewRelic.addCustomParameter("login.clientIp", clientIp);
            NewRelic.addCustomParameter("login.duration_ms", loginDuration);
            NewRelic.addCustomParameter("login.firstName", user.getFirstName());
            NewRelic.addCustomParameter("login.lastName", user.getLastName());
            NewRelic.addCustomParameter("login.status", "SUCCESS");

            // Log structured login event for easier New Relic querying
            log.warn("🔐 LOGIN_SUCCESS|username:{}|userId:{}|role:{}|email:{}|clientIp:{}|duration_ms:{}|firstName:{}|lastName:{}|timestamp:{}",
                    user.getUsername(), user.getId(), user.getRole().toString(), user.getEmail(),
                    clientIp, loginDuration, user.getFirstName(), user.getLastName(), java.time.LocalDateTime.now());

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

        } catch (Exception e) {
            long loginDuration = System.currentTimeMillis() - loginStartTime;

            // Log failed login attempt
            log.error("=== LOGIN FAILED ===");
            log.error("Username: {}", loginRequest.getUsername());
            log.error("Client IP: {}", clientIp);
            log.error("Error: {}", e.getMessage());
            log.error("Duration: {} ms", loginDuration);
            log.error("Timestamp: {}", java.time.LocalDateTime.now());

            // Send failed login to New Relic
            NewRelic.addCustomParameter("login.username", loginRequest.getUsername());
            NewRelic.addCustomParameter("login.clientIp", clientIp);
            NewRelic.addCustomParameter("login.error", e.getMessage());
            NewRelic.addCustomParameter("login.duration_ms", loginDuration);
            NewRelic.addCustomParameter("login.status", "FAILED");

            // Log structured failed login event for easier New Relic querying
            log.warn("🔐 LOGIN_FAILED|username:{}|clientIp:{}|error:{}|duration_ms:{}|timestamp:{}",
                    loginRequest.getUsername(), clientIp, e.getMessage(), loginDuration, java.time.LocalDateTime.now());

            return ResponseEntity.badRequest().body(
                    new MessageResponse("Invalid username or password")
            );
        }
    }

    /**
     * Extract client IP address from HTTP request
     * Handles X-Forwarded-For header for proxied requests
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
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
