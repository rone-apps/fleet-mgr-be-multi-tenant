package com.taxi.web.controller;

import com.taxi.domain.driver.model.Driver;
import com.taxi.domain.driver.service.DriverService;
import com.taxi.web.dto.driver.CreateDriverRequest;
import com.taxi.web.dto.driver.DriverDTO;
import com.taxi.web.dto.driver.UpdateDriverRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Driver operations
 */
@RestController
@RequestMapping("/drivers")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class DriverController {

    private final DriverService driverService;

    /**
     * Get all drivers
     * GET /api/drivers
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'MANAGER', 'DISPATCHER', 'DRIVER')")
    public ResponseEntity<List<DriverDTO>> getAllDrivers() {
        log.info("GET /api/drivers - Get all drivers");
        List<DriverDTO> drivers = driverService.getAllDrivers();
        return ResponseEntity.ok(drivers);
    }

    /**
     * Get active drivers only
     * GET /api/drivers/active
     */
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'MANAGER', 'DISPATCHER')")
    public ResponseEntity<List<DriverDTO>> getActiveDrivers() {
        log.info("GET /api/drivers/active - Get active drivers");
        List<DriverDTO> drivers = driverService.getActiveDrivers();
        return ResponseEntity.ok(drivers);
    }

    /**
     * Get drivers by status
     * GET /api/drivers/status/{status}
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'MANAGER')")
    public ResponseEntity<List<DriverDTO>> getDriversByStatus(@PathVariable String status) {
        log.info("GET /api/drivers/status/{} - Get drivers by status", status);
        try {
            Driver.DriverStatus driverStatus = Driver.DriverStatus.valueOf(status.toUpperCase());
            List<DriverDTO> drivers = driverService.getDriversByStatus(driverStatus);
            return ResponseEntity.ok(drivers);
        } catch (IllegalArgumentException e) {
            log.error("Invalid status: {}", status);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Search drivers by name
     * GET /api/drivers/search?name={name}
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'MANAGER', 'DISPATCHER')")
    public ResponseEntity<List<DriverDTO>> searchDrivers(@RequestParam String name) {
        log.info("GET /api/drivers/search?name={} - Search drivers", name);
        List<DriverDTO> drivers = driverService.searchDriversByName(name);
        return ResponseEntity.ok(drivers);
    }

    /**
     * Get driver by ID
     * GET /api/drivers/{id}
     * ✅ UPDATED: Drivers can access their own info
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DISPATCHER', 'DRIVER')")
    public ResponseEntity<?> getDriverById(@PathVariable Long id) {
        log.info("GET /api/drivers/{} - Get driver by ID", id);
        try {
            DriverDTO driver = driverService.getDriverById(id);
            return ResponseEntity.ok(driver);
        } catch (RuntimeException e) {
            log.error("Driver not found: {}", id);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * Get driver by driver number
     * GET /api/drivers/number/{driverNumber}
     */
    @GetMapping("/number/{driverNumber}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DISPATCHER')")
    public ResponseEntity<?> getDriverByNumber(@PathVariable String driverNumber) {
        log.info("GET /api/drivers/number/{} - Get driver by number", driverNumber);
        try {
            DriverDTO driver = driverService.getDriverByDriverNumber(driverNumber);
            return ResponseEntity.ok(driver);
        } catch (RuntimeException e) {
            log.error("Driver not found: {}", driverNumber);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * Create a new driver
     * POST /api/drivers
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> createDriver(@Valid @RequestBody CreateDriverRequest request) {
        log.info("POST /api/drivers - Create driver: {} {}", request.getFirstName(), request.getLastName());
        try {
            DriverDTO driver = driverService.createDriver(request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Driver created successfully");
            response.put("driver", driver);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            log.error("Failed to create driver", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Update an existing driver
     * PUT /api/drivers/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> updateDriver(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDriverRequest request) {
        log.info("PUT /api/drivers/{} - Update driver", id);
        try {
            DriverDTO driver = driverService.updateDriver(id, request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Driver updated successfully");
            response.put("driver", driver);
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Failed to update driver: {}", id, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Delete a driver (soft delete)
     * DELETE /api/drivers/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> deleteDriver(@PathVariable Long id) {
        log.info("DELETE /api/drivers/{} - Delete driver", id);
        try {
            driverService.deleteDriver(id);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Driver terminated successfully");
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Failed to delete driver: {}", id, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Activate a driver
     * PUT /api/drivers/{id}/activate
     */
    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> activateDriver(@PathVariable Long id) {
        log.info("PUT /api/drivers/{}/activate - Activate driver", id);
        try {
            DriverDTO driver = driverService.activateDriver(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Driver activated successfully");
            response.put("driver", driver);
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Failed to activate driver: {}", id, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Suspend a driver
     * PUT /api/drivers/{id}/suspend
     */
    @PutMapping("/{id}/suspend")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> suspendDriver(@PathVariable Long id) {
        log.info("PUT /api/drivers/{}/suspend - Suspend driver", id);
        try {
            DriverDTO driver = driverService.suspendDriver(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Driver suspended successfully");
            response.put("driver", driver);
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Failed to suspend driver: {}", id, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}