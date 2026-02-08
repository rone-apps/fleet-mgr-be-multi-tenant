package com.taxi.web.controller;

import com.taxi.domain.shift.service.CabShiftAttributeService;
import com.taxi.web.dto.shift.ShiftAttributesDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for Shift Attribute Management
 *
 * Manages shift-level attributes (cab type, share type, airport license).
 * Each shift (DAY/NIGHT) can have independent attributes.
 *
 * Endpoints:
 * - GET /api/cab-shifts/{shiftId}/attributes - Get shift attributes
 * - PUT /api/cab-shifts/{shiftId}/attributes - Update shift attributes
 */
@RestController
@RequestMapping("/cab-shifts")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class CabShiftAttributeController {

    private final CabShiftAttributeService attributeService;

    /**
     * Get all attributes for a shift
     *
     * Returns current attribute values for the specified shift.
     * Attributes are now managed at shift level, allowing different values
     * for DAY and NIGHT shifts of the same cab.
     *
     * GET /api/cab-shifts/{shiftId}/attributes
     *
     * Response:
     * {
     *   "cabType": "SEDAN",
     *   "shareType": "VOTING_SHARE",
     *   "hasAirportLicense": true,
     *   "airportLicenseNumber": "ALT-123456",
     *   "airportLicenseExpiry": "2027-12-31"
     * }
     */
    @GetMapping("/{shiftId}/attributes")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DISPATCHER')")
    public ResponseEntity<?> getAttributes(@PathVariable Long shiftId) {

        log.info("GET /api/cab-shifts/{}/attributes - Get shift attributes", shiftId);

        try {
            ShiftAttributesDTO attributes = attributeService.getShiftAttributes(shiftId);

            Map<String, Object> response = new HashMap<>();
            response.put("shiftId", shiftId);
            response.put("attributes", attributes);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Shift not found: {}", shiftId);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);

        } catch (Exception e) {
            log.error("Error retrieving attributes for shift {}", shiftId, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to retrieve shift attributes: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Update all attributes for a shift
     *
     * Updates all shift attributes in a single request.
     * Attributes that are null will be left as-is (not cleared).
     *
     * PUT /api/cab-shifts/{shiftId}/attributes
     *
     * Request body:
     * {
     *   "cabType": "SEDAN",
     *   "shareType": "VOTING_SHARE",
     *   "hasAirportLicense": true,
     *   "airportLicenseNumber": "ALT-123456",
     *   "airportLicenseExpiry": "2027-12-31"
     * }
     *
     * Response: Updated ShiftAttributesDTO
     */
    @PutMapping("/{shiftId}/attributes")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DISPATCHER')")
    public ResponseEntity<?> updateAttributes(
            @PathVariable Long shiftId,
            @Valid @RequestBody ShiftAttributesDTO attributes) {

        log.info("PUT /api/cab-shifts/{}/attributes - Update shift attributes", shiftId);

        try {
            attributeService.updateShiftAttributes(
                shiftId,
                attributes.getCabType(),
                attributes.getShareType(),
                attributes.getHasAirportLicense(),
                attributes.getAirportLicenseNumber(),
                attributes.getAirportLicenseExpiry()
            );

            // Retrieve updated attributes
            ShiftAttributesDTO updated = attributeService.getShiftAttributes(shiftId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Shift attributes updated successfully");
            response.put("shiftId", shiftId);
            response.put("attributes", updated);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Failed to update shift attributes - {}: {}", shiftId, e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);

        } catch (Exception e) {
            log.error("Error updating attributes for shift {}", shiftId, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to update shift attributes: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Update only the cab type for a shift
     *
     * PUT /api/cab-shifts/{shiftId}/cab-type?type=SEDAN
     *
     * Query parameter:
     * - type: Cab type (SEDAN, HANDICAP_VAN)
     */
    @PutMapping("/{shiftId}/cab-type")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DISPATCHER')")
    public ResponseEntity<?> updateCabType(
            @PathVariable Long shiftId,
            @RequestParam String type) {

        log.info("PUT /api/cab-shifts/{}/cab-type - Update cab type to {}", shiftId, type);

        try {
            var cabType = com.taxi.domain.cab.model.CabType.valueOf(type.toUpperCase());
            attributeService.updateCabType(shiftId, cabType);

            ShiftAttributesDTO updated = attributeService.getShiftAttributes(shiftId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Cab type updated successfully");
            response.put("shiftId", shiftId);
            response.put("cabType", cabType);
            response.put("attributes", updated);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Failed to update cab type for shift {}: {}", shiftId, e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);

        } catch (Exception e) {
            log.error("Error updating cab type for shift {}", shiftId, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to update cab type: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Update only the share type for a shift
     *
     * PUT /api/cab-shifts/{shiftId}/share-type?type=VOTING_SHARE
     *
     * Query parameter:
     * - type: Share type (VOTING_SHARE, NON_VOTING_SHARE, or empty/null to clear)
     */
    @PutMapping("/{shiftId}/share-type")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DISPATCHER')")
    public ResponseEntity<?> updateShareType(
            @PathVariable Long shiftId,
            @RequestParam(required = false) String type) {

        log.info("PUT /api/cab-shifts/{}/share-type - Update share type to {}", shiftId, type);

        try {
            com.taxi.domain.cab.model.ShareType shareType = null;
            if (type != null && !type.isEmpty()) {
                shareType = com.taxi.domain.cab.model.ShareType.valueOf(type.toUpperCase());
            }

            attributeService.updateShareType(shiftId, shareType);

            ShiftAttributesDTO updated = attributeService.getShiftAttributes(shiftId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Share type updated successfully");
            response.put("shiftId", shiftId);
            response.put("shareType", shareType);
            response.put("attributes", updated);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Failed to update share type for shift {}: {}", shiftId, e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);

        } catch (Exception e) {
            log.error("Error updating share type for shift {}", shiftId, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to update share type: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Check if shift has airport license
     *
     * GET /api/cab-shifts/{shiftId}/has-airport-license
     *
     * Response:
     * {
     *   "shiftId": 123,
     *   "hasAirportLicense": true
     * }
     */
    @GetMapping("/{shiftId}/has-airport-license")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DISPATCHER')")
    public ResponseEntity<?> hasAirportLicense(@PathVariable Long shiftId) {

        log.info("GET /api/cab-shifts/{}/has-airport-license", shiftId);

        try {
            boolean hasLicense = attributeService.hasAirportLicense(shiftId);

            Map<String, Object> response = new HashMap<>();
            response.put("shiftId", shiftId);
            response.put("hasAirportLicense", hasLicense);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Shift not found: {}", shiftId);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);

        } catch (Exception e) {
            log.error("Error checking airport license for shift {}", shiftId, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to check airport license: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Check if shift's airport license is expired
     *
     * GET /api/cab-shifts/{shiftId}/airport-license-expired
     *
     * Response:
     * {
     *   "shiftId": 123,
     *   "isExpired": false,
     *   "licenseNumber": "ALT-123456",
     *   "expiryDate": "2027-12-31"
     * }
     */
    @GetMapping("/{shiftId}/airport-license-expired")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DISPATCHER')")
    public ResponseEntity<?> isAirportLicenseExpired(@PathVariable Long shiftId) {

        log.info("GET /api/cab-shifts/{}/airport-license-expired", shiftId);

        try {
            boolean isExpired = attributeService.isAirportLicenseExpired(shiftId);
            String licenseNumber = attributeService.getAirportLicenseNumber(shiftId);
            var expiryDate = attributeService.getAirportLicenseExpiry(shiftId);

            Map<String, Object> response = new HashMap<>();
            response.put("shiftId", shiftId);
            response.put("isExpired", isExpired);
            response.put("licenseNumber", licenseNumber);
            response.put("expiryDate", expiryDate);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Shift not found: {}", shiftId);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);

        } catch (Exception e) {
            log.error("Error checking airport license expiry for shift {}", shiftId, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to check airport license expiry: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
