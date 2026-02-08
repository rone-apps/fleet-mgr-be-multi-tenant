package com.taxi.web.controller;

import com.taxi.domain.cab.model.CabAttributeValue;
import com.taxi.domain.cab.service.CabAttributeValueService;
import com.taxi.web.dto.cab.attribute.AssignAttributeRequest;
import com.taxi.web.dto.cab.attribute.CabAttributeValueDTO;
import com.taxi.web.dto.cab.attribute.UpdateAttributeValueRequest;
import com.taxi.web.dto.cab.attribute.EndAttributeRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller for Shift Custom Attribute Value operations
 *
 * Manages temporal custom attributes assigned to shifts (DAY/NIGHT).
 * Follows the same pattern as CabAttributeValueController but for shift-level attributes.
 *
 * Endpoints:
 * - POST /api/cab-shifts/{shiftId}/custom-attributes - Assign custom attribute
 * - PUT /api/cab-shifts/{shiftId}/custom-attributes/{attributeValueId} - Update
 * - PUT /api/cab-shifts/{shiftId}/custom-attributes/{attributeValueId}/end - End assignment
 * - GET /api/cab-shifts/{shiftId}/custom-attributes/current - Get current
 * - GET /api/cab-shifts/{shiftId}/custom-attributes/on-date - Get by date
 * - GET /api/cab-shifts/{shiftId}/custom-attributes/history - Get history
 * - DELETE /api/cab-shifts/{shiftId}/custom-attributes/{attributeValueId} - Delete
 */
@RestController
@RequestMapping("/cab-shifts/{shiftId}/custom-attributes")
@RequiredArgsConstructor
@Slf4j
public class ShiftAttributeValueController {

    private final CabAttributeValueService attributeValueService;

    /**
     * Assign a custom attribute to a shift
     *
     * POST /api/cab-shifts/{shiftId}/custom-attributes
     *
     * Request body:
     * {
     *   "attributeTypeId": 1,
     *   "attributeValue": "ALT-123456",
     *   "startDate": "2026-02-06",
     *   "endDate": "2027-12-31",
     *   "notes": "Airport license for this shift"
     * }
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> assignAttribute(
            @PathVariable Long shiftId,
            @Valid @RequestBody AssignAttributeRequest request) {
        log.info("POST /api/cab-shifts/{}/custom-attributes - Assign attribute", shiftId);
        try {
            CabAttributeValue value = attributeValueService.assignAttributeToShift(
                shiftId,
                request.getAttributeTypeId(),
                request.getAttributeValue(),
                request.getStartDate(),
                request.getEndDate(),
                request.getNotes()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Attribute assigned successfully");
            response.put("attributeValue", CabAttributeValueDTO.fromEntity(value));

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            log.error("Failed to assign attribute to shift {}: {}", shiftId, e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Update a custom attribute assignment for a shift
     *
     * PUT /api/cab-shifts/{shiftId}/custom-attributes/{attributeValueId}
     *
     * Request body:
     * {
     *   "attributeValue": "ALT-789012",
     *   "startDate": "2026-02-06",
     *   "endDate": "2027-12-31",
     *   "notes": "Updated airport license"
     * }
     */
    @PutMapping("/{attributeValueId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> updateAttributeValue(
            @PathVariable Long shiftId,
            @PathVariable Long attributeValueId,
            @Valid @RequestBody UpdateAttributeValueRequest request) {
        log.info("PUT /api/cab-shifts/{}/custom-attributes/{}", shiftId, attributeValueId);
        try {
            CabAttributeValue value = attributeValueService.updateShiftAttributeValue(
                attributeValueId,
                request.getAttributeValue(),
                request.getStartDate(),
                request.getEndDate(),
                request.getNotes()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Attribute value updated successfully");
            response.put("attributeValue", CabAttributeValueDTO.fromEntity(value));

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Failed to update attribute value {} for shift {}: {}",
                attributeValueId, shiftId, e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * End a custom attribute assignment (set end date to effective date)
     *
     * PUT /api/cab-shifts/{shiftId}/custom-attributes/{attributeValueId}/end
     *
     * Request body:
     * {
     *   "endDate": "2027-02-06"
     * }
     */
    @PutMapping("/{attributeValueId}/end")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> endAttribute(
            @PathVariable Long shiftId,
            @PathVariable Long attributeValueId,
            @Valid @RequestBody EndAttributeRequest request) {
        log.info("PUT /api/cab-shifts/{}/custom-attributes/{}/end", shiftId, attributeValueId);
        try {
            attributeValueService.endShiftAttributeAssignment(attributeValueId, request.getEndDate());
            Map<String, String> response = new HashMap<>();
            response.put("message", "Attribute assignment ended successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Failed to end attribute {} for shift {}: {}",
                attributeValueId, shiftId, e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get current custom attributes for a shift (where endDate is NULL)
     *
     * GET /api/cab-shifts/{shiftId}/custom-attributes/current
     *
     * Response:
     * [
     *   {
     *     "id": 1,
     *     "attributeCode": "AIRPORT_LICENSE",
     *     "attributeName": "Airport License",
     *     "attributeValue": "ALT-123456",
     *     "startDate": "2026-02-06",
     *     "endDate": null,
     *     "isCurrent": true,
     *     "notes": "Active airport license"
     *   }
     * ]
     */
    @GetMapping("/current")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DISPATCHER')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<CabAttributeValueDTO>> getCurrentAttributes(
            @PathVariable Long shiftId) {
        log.info("GET /api/cab-shifts/{}/custom-attributes/current", shiftId);
        List<CabAttributeValueDTO> attributes = attributeValueService.getCurrentAttributesByShift(shiftId)
                .stream()
                .map(CabAttributeValueDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(attributes);
    }

    /**
     * Get custom attributes active on a specific date for a shift
     *
     * GET /api/cab-shifts/{shiftId}/custom-attributes/on-date?date=2026-05-01
     *
     * Query parameter:
     * - date: Date to query (ISO format: YYYY-MM-DD)
     */
    @GetMapping("/on-date")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DISPATCHER')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<CabAttributeValueDTO>> getAttributesOnDate(
            @PathVariable Long shiftId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.info("GET /api/cab-shifts/{}/custom-attributes/on-date?date={}", shiftId, date);
        List<CabAttributeValueDTO> attributes = attributeValueService.getAttributesOnDateByShift(shiftId, date)
                .stream()
                .map(CabAttributeValueDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(attributes);
    }

    /**
     * Get full custom attribute history for a shift
     *
     * GET /api/cab-shifts/{shiftId}/custom-attributes/history
     *
     * Response: List of all attribute assignments (current and historical)
     */
    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DISPATCHER')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<CabAttributeValueDTO>> getAttributeHistory(
            @PathVariable Long shiftId) {
        log.info("GET /api/cab-shifts/{}/custom-attributes/history", shiftId);
        List<CabAttributeValueDTO> history = attributeValueService.getAttributeHistoryByShift(shiftId)
                .stream()
                .map(CabAttributeValueDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(history);
    }

    /**
     * Delete a custom attribute assignment for a shift
     *
     * DELETE /api/cab-shifts/{shiftId}/custom-attributes/{attributeValueId}
     *
     * Note: Only SUPER_ADMIN can delete attributes
     */
    @DeleteMapping("/{attributeValueId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> deleteAttribute(
            @PathVariable Long shiftId,
            @PathVariable Long attributeValueId) {
        log.info("DELETE /api/cab-shifts/{}/custom-attributes/{}", shiftId, attributeValueId);
        try {
            attributeValueService.deleteShiftAttributeValue(attributeValueId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Attribute deleted successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Failed to delete attribute {} for shift {}: {}",
                attributeValueId, shiftId, e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
