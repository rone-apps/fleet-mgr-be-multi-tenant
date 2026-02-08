package com.taxi.web.controller;

import com.taxi.domain.shift.service.ShiftStatusService;
import com.taxi.web.dto.shift.ActivateShiftRequest;
import com.taxi.web.dto.shift.DeactivateShiftRequest;
import com.taxi.web.dto.shift.ShiftStatusHistoryDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller for Shift Status Management
 *
 * Manages shift active/inactive status with full historical tracking.
 * Each endpoint creates audit trail entries for compliance and reporting.
 *
 * Endpoints:
 * - POST /api/shift-status/{shiftId}/activate - Activate a shift
 * - POST /api/shift-status/{shiftId}/deactivate - Deactivate a shift
 * - GET /api/shift-status/{shiftId}/history - Get status history
 * - GET /api/shift-status/{shiftId}/active-on - Check status on date
 */
@RestController
@RequestMapping("/shift-status")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class ShiftStatusController {

    private final ShiftStatusService shiftStatusService;

    /**
     * Activate a shift with effective date
     *
     * Creates a new status history record and closes the previous one.
     * Requires ADMIN or MANAGER role.
     *
     * POST /api/shift-status/{shiftId}/activate
     *
     * Request body:
     * {
     *   "effectiveFrom": "2026-02-06",
     *   "reason": "Returning from maintenance"
     * }
     */
    @PostMapping("/{shiftId}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> activateShift(
            @PathVariable Long shiftId,
            @Valid @RequestBody ActivateShiftRequest request,
            Authentication authentication) {

        log.info("POST /api/shift-status/{}/activate - Activate shift, effective from: {}",
            shiftId, request.getEffectiveFrom());

        try {
            String changedBy = authentication != null ? authentication.getName() : "SYSTEM";

            shiftStatusService.activateShift(
                shiftId,
                request.getEffectiveFrom(),
                changedBy,
                request.getReason()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Shift activated successfully");
            response.put("shiftId", shiftId);
            response.put("effectiveFrom", request.getEffectiveFrom());
            response.put("changedBy", changedBy);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Failed to activate shift {}: {}", shiftId, e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);

        } catch (Exception e) {
            log.error("Error activating shift {}", shiftId, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to activate shift: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Deactivate a shift with effective date
     *
     * Creates a new status history record and closes the previous one.
     * Requires ADMIN or MANAGER role.
     *
     * POST /api/shift-status/{shiftId}/deactivate
     *
     * Request body:
     * {
     *   "effectiveFrom": "2026-02-06",
     *   "reason": "Sending for maintenance"
     * }
     */
    @PostMapping("/{shiftId}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> deactivateShift(
            @PathVariable Long shiftId,
            @Valid @RequestBody DeactivateShiftRequest request,
            Authentication authentication) {

        log.info("POST /api/shift-status/{}/deactivate - Deactivate shift, effective from: {}",
            shiftId, request.getEffectiveFrom());

        try {
            String changedBy = authentication != null ? authentication.getName() : "SYSTEM";

            shiftStatusService.deactivateShift(
                shiftId,
                request.getEffectiveFrom(),
                changedBy,
                request.getReason()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Shift deactivated successfully");
            response.put("shiftId", shiftId);
            response.put("effectiveFrom", request.getEffectiveFrom());
            response.put("changedBy", changedBy);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Failed to deactivate shift {}: {}", shiftId, e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);

        } catch (Exception e) {
            log.error("Error deactivating shift {}", shiftId, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to deactivate shift: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get complete status history for a shift
     *
     * Returns all status records for the shift, ordered by most recent first.
     * Includes both historical and current status records.
     *
     * GET /api/shift-status/{shiftId}/history
     *
     * Response: List of ShiftStatusHistoryDTO objects
     */
    @GetMapping("/{shiftId}/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DISPATCHER')")
    public ResponseEntity<?> getStatusHistory(@PathVariable Long shiftId) {

        log.info("GET /api/shift-status/{}/history - Get status history", shiftId);

        try {
            List<ShiftStatusHistoryDTO> history = shiftStatusService.getStatusHistory(shiftId)
                .stream()
                .map(ShiftStatusHistoryDTO::fromEntity)
                .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("shiftId", shiftId);
            response.put("totalRecords", history.size());
            response.put("history", history);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error retrieving status history for shift {}", shiftId, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to retrieve status history: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Check if a shift was active on a specific date
     *
     * Useful for historical reporting and verification.
     * Query parameter: date in YYYY-MM-DD format
     *
     * GET /api/shift-status/{shiftId}/active-on?date=2026-02-06
     *
     * Response:
     * {
     *   "shiftId": 123,
     *   "date": "2026-02-06",
     *   "isActive": true
     * }
     */
    @GetMapping("/{shiftId}/active-on")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DISPATCHER')")
    public ResponseEntity<?> wasActiveOn(
            @PathVariable Long shiftId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("GET /api/shift-status/{}/active-on?date={} - Check shift status on date",
            shiftId, date);

        try {
            boolean isActive = shiftStatusService.wasShiftActiveOn(shiftId, date);

            Map<String, Object> response = new HashMap<>();
            response.put("shiftId", shiftId);
            response.put("date", date);
            response.put("isActive", isActive);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error checking shift status for shift {} on date {}", shiftId, date, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to check shift status: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get current status of a shift
     *
     * Returns the current active/inactive status (where effective_to is NULL).
     *
     * GET /api/shift-status/{shiftId}/current
     *
     * Response: ShiftStatusHistoryDTO or null if no status found
     */
    @GetMapping("/{shiftId}/current")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DISPATCHER')")
    public ResponseEntity<?> getCurrentStatus(@PathVariable Long shiftId) {

        log.info("GET /api/shift-status/{}/current - Get current shift status", shiftId);

        try {
            var currentStatus = shiftStatusService.getCurrentStatus(shiftId)
                .map(ShiftStatusHistoryDTO::fromEntity);

            Map<String, Object> response = new HashMap<>();
            response.put("shiftId", shiftId);

            if (currentStatus.isPresent()) {
                response.put("status", currentStatus.get());
                response.put("found", true);
            } else {
                response.put("status", null);
                response.put("found", false);
                response.put("message", "No status found for shift");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error retrieving current status for shift {}", shiftId, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to retrieve current status: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Check if shift is currently active
     *
     * Convenience endpoint for quick status checks.
     *
     * GET /api/shift-status/{shiftId}/is-active
     *
     * Response:
     * {
     *   "shiftId": 123,
     *   "isCurrentlyActive": true
     * }
     */
    @GetMapping("/{shiftId}/is-active")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DISPATCHER')")
    public ResponseEntity<?> isCurrentlyActive(@PathVariable Long shiftId) {

        log.info("GET /api/shift-status/{}/is-active - Check if shift is currently active", shiftId);

        try {
            boolean isActive = shiftStatusService.isCurrentlyActive(shiftId);

            Map<String, Object> response = new HashMap<>();
            response.put("shiftId", shiftId);
            response.put("isCurrentlyActive", isActive);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error checking current active status for shift {}", shiftId, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to check active status: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
