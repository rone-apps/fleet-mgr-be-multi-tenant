package com.taxi.web.controller;

import com.taxi.domain.cab.model.Cab;
import com.taxi.domain.cab.model.CabType;
import com.taxi.domain.cab.service.CabService;
import com.taxi.web.dto.cab.CabDTO;
import com.taxi.web.dto.cab.CreateCabRequest;
import com.taxi.web.dto.cab.UpdateCabRequest;
import com.taxi.web.dto.cab.CabOwnerHistoryDTO;
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
 * REST Controller for Cab operations
 */
@RestController
@RequestMapping("/cabs")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class CabController {

    private final CabService cabService;

    /**
     * Get all cabs
     * GET /api/cabs
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DISPATCHER')")
    public ResponseEntity<List<CabDTO>> getAllCabs() {
        log.info("GET /api/cabs - Get all cabs");
        List<CabDTO> cabs = cabService.getAllCabs();
        return ResponseEntity.ok(cabs);
    }

    /**
     * Get active cabs only
     * GET /api/cabs/active
     */
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DISPATCHER')")
    public ResponseEntity<List<CabDTO>> getActiveCabs() {
        log.info("GET /api/cabs/active - Get active cabs");
        List<CabDTO> cabs = cabService.getActiveCabs();
        return ResponseEntity.ok(cabs);
    }

    /**
     * Get cabs by status
     * GET /api/cabs/status/{status}
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> getCabsByStatus(@PathVariable String status) {
        log.info("GET /api/cabs/status/{} - Get cabs by status", status);
        try {
            Cab.CabStatus cabStatus = Cab.CabStatus.valueOf(status.toUpperCase());
            List<CabDTO> cabs = cabService.getCabsByStatus(cabStatus);
            return ResponseEntity.ok(cabs);
        } catch (IllegalArgumentException e) {
            log.error("Invalid status: {}", status);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid status: " + status);
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get cabs by type
     * GET /api/cabs/type/{type}
     */
    @GetMapping("/type/{type}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DISPATCHER')")
    public ResponseEntity<?> getCabsByType(@PathVariable String type) {
        log.info("GET /api/cabs/type/{} - Get cabs by type", type);
        try {
            CabType cabType = CabType.valueOf(type.toUpperCase());
            List<CabDTO> cabs = cabService.getCabsByType(cabType);
            return ResponseEntity.ok(cabs);
        } catch (IllegalArgumentException e) {
            log.error("Invalid cab type: {}", type);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid cab type: " + type);
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get cabs with airport license
     * GET /api/cabs/airport-licensed
     */
    @GetMapping("/airport-licensed")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DISPATCHER')")
    public ResponseEntity<List<CabDTO>> getAirportLicensedCabs() {
        log.info("GET /api/cabs/airport-licensed - Get cabs with airport license");
        List<CabDTO> cabs = cabService.getCabsWithAirportLicense();
        return ResponseEntity.ok(cabs);
    }

    /**
     * Get cab by ID
     * GET /api/cabs/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DISPATCHER')")
    public ResponseEntity<?> getCabById(@PathVariable Long id) {
        log.info("GET /api/cabs/{} - Get cab by ID", id);
        try {
            CabDTO cab = cabService.getCabById(id);
            return ResponseEntity.ok(cab);
        } catch (RuntimeException e) {
            log.error("Cab not found: {}", id);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * Get cab by cab number
     * GET /api/cabs/number/{cabNumber}
     */
    @GetMapping("/number/{cabNumber}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DISPATCHER')")
    public ResponseEntity<?> getCabByCabNumber(@PathVariable String cabNumber) {
        log.info("GET /api/cabs/number/{} - Get cab by cab number", cabNumber);
        try {
            CabDTO cab = cabService.getCabByCabNumber(cabNumber);
            return ResponseEntity.ok(cab);
        } catch (RuntimeException e) {
            log.error("Cab not found: {}", cabNumber);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * Create a new cab
     * POST /api/cabs
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> createCab(@Valid @RequestBody CreateCabRequest request) {
        log.info("POST /api/cabs - Create cab with registration: {}", request.getRegistrationNumber());
        try {
            CabDTO cab = cabService.createCab(request);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Cab created successfully");
            response.put("cab", cab);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            log.error("Failed to create cab", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Update an existing cab
     * PUT /api/cabs/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> updateCab(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCabRequest request) {
        log.info("PUT /api/cabs/{} - Update cab", id);
        try {
            CabDTO cab = cabService.updateCab(id, request);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Cab updated successfully");
            response.put("cab", cab);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Failed to update cab: {}", id, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Set cab to maintenance
     * PUT /api/cabs/{id}/maintenance
     */
    @PutMapping("/{id}/maintenance")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> setMaintenance(@PathVariable Long id) {
        log.info("PUT /api/cabs/{}/maintenance - Set cab to maintenance", id);
        try {
            CabDTO cab = cabService.setMaintenance(id);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Cab set to maintenance");
            response.put("cab", cab);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Failed to set maintenance: {}", id, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Activate cab
     * PUT /api/cabs/{id}/activate
     */
    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> activate(@PathVariable Long id) {
        log.info("PUT /api/cabs/{}/activate - Activate cab", id);
        try {
            CabDTO cab = cabService.activate(id);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Cab activated successfully");
            response.put("cab", cab);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Failed to activate cab: {}", id, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Retire cab
     * PUT /api/cabs/{id}/retire
     */
    @PutMapping("/{id}/retire")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> retire(@PathVariable Long id) {
        log.info("PUT /api/cabs/{}/retire - Retire cab", id);
        try {
            CabDTO cab = cabService.retire(id);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Cab retired successfully");
            response.put("cab", cab);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Failed to retire cab: {}", id, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get ownership history for a cab
     * GET /api/cabs/{id}/owner-history
     */
    @GetMapping("/{id}/owner-history")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DISPATCHER')")
    public ResponseEntity<List<CabOwnerHistoryDTO>> getOwnerHistory(@PathVariable Long id) {
        log.info("GET /api/cabs/{}/owner-history - Get owner history", id);
        List<CabOwnerHistoryDTO> history = cabService.getOwnerHistory(id);
        return ResponseEntity.ok(history);
    }

    /**
     * Delete a cab
     * DELETE /api/cabs/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> deleteCab(@PathVariable Long id) {
        log.info("DELETE /api/cabs/{} - Delete cab", id);
        try {
            cabService.deleteCab(id);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Cab deleted successfully");

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Failed to delete cab: {}", id, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
