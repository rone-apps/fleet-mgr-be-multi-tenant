package com.taxi.web.controller;

import com.taxi.domain.shift.service.ShiftService;
import com.taxi.web.dto.shift.CabShiftDTO;
import com.taxi.web.dto.shift.CreateShiftRequest;
import com.taxi.web.dto.shift.ShiftOwnershipDTO;
import com.taxi.web.dto.shift.TransferShiftOwnershipRequest;
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
 * REST Controller for Shift operations
 */
@RestController
@RequestMapping("/shifts")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class ShiftController {

    private final ShiftService shiftService;

    /**
     * Get all shifts
     * GET /api/shifts
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DISPATCHER')")
    public ResponseEntity<List<CabShiftDTO>> getAllShifts() {
        log.info("GET /api/shifts - Get all shifts");
        List<CabShiftDTO> shifts = shiftService.getAllShifts();
        return ResponseEntity.ok(shifts);
    }

    /**
     * Get all shifts for a cab
     * GET /api/shifts/cab/{cabId}
     */
    @GetMapping("/cab/{cabId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DISPATCHER')")
    public ResponseEntity<List<CabShiftDTO>> getShiftsByCab(@PathVariable Long cabId) {
        log.info("GET /api/shifts/cab/{} - Get shifts for cab", cabId);
        List<CabShiftDTO> shifts = shiftService.getShiftsByCab(cabId);
        return ResponseEntity.ok(shifts);
    }

    /**
     * Get all shifts owned by a driver
     * GET /api/shifts/owner/{ownerId}
     */
    @GetMapping("/owner/{ownerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DISPATCHER')")
    public ResponseEntity<List<CabShiftDTO>> getShiftsByOwner(@PathVariable Long ownerId) {
        log.info("GET /api/shifts/owner/{} - Get shifts owned by driver", ownerId);
        List<CabShiftDTO> shifts = shiftService.getShiftsByOwner(ownerId);
        return ResponseEntity.ok(shifts);
    }

    /**
     * Get shift by ID
     * GET /api/shifts/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DISPATCHER')")
    public ResponseEntity<?> getShiftById(@PathVariable Long id) {
        log.info("GET /api/shifts/{} - Get shift by ID", id);
        try {
            CabShiftDTO shift = shiftService.getShiftById(id);
            return ResponseEntity.ok(shift);
        } catch (RuntimeException e) {
            log.error("Shift not found: {}", id);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * Create a new shift
     * POST /api/shifts
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> createShift(@Valid @RequestBody CreateShiftRequest request) {
        log.info("POST /api/shifts - Create shift for cab ID: {}", request.getCabId());
        try {
            CabShiftDTO shift = shiftService.createShift(request);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Shift created successfully");
            response.put("shift", shift);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            log.error("Failed to create shift", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Transfer shift ownership
     * PUT /api/shifts/{id}/transfer
     */
    @PutMapping("/{id}/transfer")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> transferOwnership(
            @PathVariable Long id,
            @Valid @RequestBody TransferShiftOwnershipRequest request) {
        log.info("PUT /api/shifts/{}/transfer - Transfer ownership", id);
        try {
            CabShiftDTO shift = shiftService.transferOwnership(id, request);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Ownership transferred successfully");
            response.put("shift", shift);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Failed to transfer ownership: {}", id, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get ownership history for a shift
     * GET /api/shifts/{id}/ownership-history
     */
    @GetMapping("/{id}/ownership-history")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DISPATCHER')")
    public ResponseEntity<List<ShiftOwnershipDTO>> getOwnershipHistory(@PathVariable Long id) {
        log.info("GET /api/shifts/{}/ownership-history - Get ownership history", id);
        List<ShiftOwnershipDTO> history = shiftService.getOwnershipHistory(id);
        return ResponseEntity.ok(history);
    }

    /**
     * Get all ownership history for a driver
     * GET /api/shifts/ownership-history/driver/{driverId}
     */
    @GetMapping("/ownership-history/driver/{driverId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DISPATCHER')")
    public ResponseEntity<List<ShiftOwnershipDTO>> getOwnershipHistoryByDriver(@PathVariable Long driverId) {
        log.info("GET /api/shifts/ownership-history/driver/{} - Get ownership history for driver", driverId);
        List<ShiftOwnershipDTO> history = shiftService.getOwnershipHistoryByDriver(driverId);
        return ResponseEntity.ok(history);
    }

    /**
     * Activate shift
     * PUT /api/shifts/{id}/activate
     */
    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> activateShift(@PathVariable Long id) {
        log.info("PUT /api/shifts/{}/activate - Activate shift", id);
        try {
            CabShiftDTO shift = shiftService.activateShift(id);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Shift activated successfully");
            response.put("shift", shift);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Failed to activate shift: {}", id, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Deactivate shift
     * PUT /api/shifts/{id}/deactivate
     */
    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> deactivateShift(@PathVariable Long id) {
        log.info("PUT /api/shifts/{}/deactivate - Deactivate shift", id);
        try {
            CabShiftDTO shift = shiftService.deactivateShift(id);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Shift deactivated successfully");
            response.put("shift", shift);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Failed to deactivate shift: {}", id, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
