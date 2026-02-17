package com.taxi.web.controller;

import com.taxi.domain.profile.service.ShiftProfileService;
import com.taxi.web.dto.profile.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ShiftProfileController - REST API for shift profile management
 *
 * Endpoints:
 * - GET /api/shift-profiles - List all active profiles
 * - GET /api/shift-profiles/{id} - Get profile details
 * - POST /api/shift-profiles - Create profile (ADMIN, MANAGER)
 * - PUT /api/shift-profiles/{id} - Update profile (ADMIN, MANAGER)
 * - DELETE /api/shift-profiles/{id} - Delete profile (ADMIN, SUPER_ADMIN)
 * - PUT /api/shift-profiles/{id}/activate - Activate (ADMIN, MANAGER)
 * - PUT /api/shift-profiles/{id}/deactivate - Deactivate (ADMIN, MANAGER)
 * - POST /api/shift-profiles/assign - Assign to shift
 * - POST /api/shift-profiles/remove - Remove from shift
 * - GET /api/shift-profiles/suggest/{shiftId} - Get suggested profiles
 */
@RestController
@RequestMapping("/shift-profiles")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class ShiftProfileController {

    private final ShiftProfileService shiftProfileService;

    // ============================================================================
    // Profile CRUD Operations
    // ============================================================================

    /**
     * GET /api/shift-profiles - List all active profiles
     */
    @GetMapping
    public ResponseEntity<List<ShiftProfileDTO>> getAllProfiles() {
        log.info("GET /api/shift-profiles - List all active profiles");

        List<ShiftProfileDTO> profiles = shiftProfileService.getAllActiveProfiles().stream()
                .map(ShiftProfileDTO::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(profiles);
    }

    /**
     * GET /api/shift-profiles/{id} - Get single profile
     */
    @GetMapping("/{id}")
    public ResponseEntity<ShiftProfileDTO> getProfileById(@PathVariable Long id) {
        log.info("GET /api/shift-profiles/{} - Get profile details", id);

        ShiftProfileDTO profile = ShiftProfileDTO.fromEntity(
                shiftProfileService.getProfileById(id)
        );

        return ResponseEntity.ok(profile);
    }

    /**
     * POST /api/shift-profiles - Create new profile
     * Requires: ADMIN or MANAGER role
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ShiftProfileDTO> createProfile(
            @RequestBody CreateShiftProfileRequest request,
            Principal principal) {
        log.info("POST /api/shift-profiles - Create new profile: {}", request.getProfileCode());

        try {
            ShiftProfileDTO profile = ShiftProfileDTO.fromEntity(
                    shiftProfileService.createProfile(
                            request,
                            principal.getName()
                    )
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(profile);
        } catch (IllegalArgumentException e) {
            log.warn("Profile creation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * PUT /api/shift-profiles/{id} - Update profile
     * Requires: ADMIN or MANAGER role
     * System profiles cannot have profile_code changed but can be updated
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ShiftProfileDTO> updateProfile(
            @PathVariable Long id,
            @RequestBody UpdateShiftProfileRequest request,
            Principal principal) {
        log.info("PUT /api/shift-profiles/{} - Update profile", id);

        try {
            ShiftProfileDTO profile = ShiftProfileDTO.fromEntity(
                    shiftProfileService.updateProfile(
                            id,
                            request,
                            principal.getName()
                    )
            );
            return ResponseEntity.ok(profile);
        } catch (IllegalArgumentException e) {
            log.warn("Profile update failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * DELETE /api/shift-profiles/{id} - Delete profile
     * Requires: ADMIN or SUPER_ADMIN role
     * Can only delete custom profiles with no shifts assigned
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> deleteProfile(@PathVariable Long id) {
        log.info("DELETE /api/shift-profiles/{} - Delete profile", id);

        try {
            shiftProfileService.deleteProfile(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Profile deletion failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * PUT /api/shift-profiles/{id}/activate - Activate profile
     * Requires: ADMIN or MANAGER role
     */
    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ShiftProfileDTO> activateProfile(
            @PathVariable Long id,
            Principal principal) {
        log.info("PUT /api/shift-profiles/{}/activate - Activate profile", id);

        try {
            ShiftProfileDTO profile = ShiftProfileDTO.fromEntity(
                    shiftProfileService.activateProfile(id, principal.getName())
            );
            return ResponseEntity.ok(profile);
        } catch (IllegalArgumentException e) {
            log.warn("Profile activation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * PUT /api/shift-profiles/{id}/deactivate - Deactivate profile
     * Requires: ADMIN or MANAGER role
     */
    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ShiftProfileDTO> deactivateProfile(
            @PathVariable Long id,
            Principal principal) {
        log.info("PUT /api/shift-profiles/{}/deactivate - Deactivate profile", id);

        try {
            ShiftProfileDTO profile = ShiftProfileDTO.fromEntity(
                    shiftProfileService.deactivateProfile(id, principal.getName())
            );
            return ResponseEntity.ok(profile);
        } catch (IllegalArgumentException e) {
            log.warn("Profile deactivation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // ============================================================================
    // Profile Assignment to Shifts
    // ============================================================================

    /**
     * POST /api/shift-profiles/assign - Assign profile to shift with date-based history tracking
     * Request body: { shiftId, profileId, startDate, reason }
     * Requires: ADMIN or MANAGER role
     */
    @PostMapping("/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Object> assignProfileToShift(
            @RequestBody AssignProfileRequest request,
            Principal principal) {
        log.info("POST /api/shift-profiles/assign - Assign profile {} to shift {} starting {}",
                request.getProfileId(), request.getShiftId(), request.getStartDate());

        try {
            var assignment = shiftProfileService.assignProfileToShift(
                    request.getShiftId(),
                    request.getProfileId(),
                    request.getReason() != null ? request.getReason() : "Profile assignment",
                    request.getStartDate(),
                    principal.getName()
            );

            ShiftProfileAssignmentDTO dto = ShiftProfileAssignmentDTO.fromEntity(assignment);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (IllegalArgumentException e) {
            log.warn("Profile assignment failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new Object() {
                public final String error = e.getMessage();
            });
        } catch (DataIntegrityViolationException e) {
            log.warn("Profile assignment constraint violation: {}", e.getMessage());
            String userMessage = "A shift profile is already active for this shift on this date. " +
                    "Please end the current profile assignment first or choose a different date range.";
            if (e.getMessage() != null && e.getMessage().contains("uk_shift_active_profile")) {
                // This is the specific constraint we're checking for
                return ResponseEntity.status(HttpStatus.CONFLICT).body(new Object() {
                    public final String error = userMessage;
                });
            }
            // For other constraint violations, provide a generic message
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new Object() {
                public final String error = "This operation violates database constraints. Please check your input and try again.";
            });
        }
    }

    /**
     * GET /api/shift-profiles/assignments/{shiftId} - Get profile assignment history for a shift
     * Shows all current and past profile assignments with dates
     */
    @GetMapping("/assignments/{shiftId}")
    public ResponseEntity<List<ShiftProfileAssignmentDTO>> getAssignmentHistory(
            @PathVariable Long shiftId) {
        log.info("GET /api/shift-profiles/assignments/{} - Get assignment history", shiftId);

        try {
            List<ShiftProfileAssignmentDTO> history = shiftProfileService.getAssignmentHistory(shiftId)
                    .stream()
                    .map(ShiftProfileAssignmentDTO::fromEntity)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(history);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to get assignment history: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * GET /api/shift-profiles/current-assignment/{shiftId} - Get current active assignment
     * Returns the profile currently assigned to the shift (end_date IS NULL)
     */
    @GetMapping("/current-assignment/{shiftId}")
    public ResponseEntity<ShiftProfileAssignmentDTO> getCurrentAssignment(
            @PathVariable Long shiftId) {
        log.info("GET /api/shift-profiles/current-assignment/{} - Get current assignment", shiftId);

        try {
            ShiftProfileAssignmentDTO current = ShiftProfileAssignmentDTO.fromEntity(
                    shiftProfileService.getCurrentAssignment(shiftId)
            );
            return ResponseEntity.ok(current);
        } catch (IllegalArgumentException e) {
            log.warn("No active assignment found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * POST /api/shift-profiles/end-assignment/{shiftId} - End current profile assignment
     * Requires: ADMIN or MANAGER role
     */
    @PostMapping("/end-assignment/{shiftId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Object> endProfileAssignment(
            @PathVariable Long shiftId,
            @RequestParam java.time.LocalDate endDate,
            Principal principal) {
        log.info("POST /api/shift-profiles/end-assignment/{} - End assignment on {}", shiftId, endDate);

        try {
            var ended = shiftProfileService.endProfileAssignment(shiftId, endDate, principal.getName());
            ShiftProfileAssignmentDTO dto = ShiftProfileAssignmentDTO.fromEntity(ended);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to end assignment: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new Object() {
                public final String error = e.getMessage();
            });
        } catch (DataIntegrityViolationException e) {
            log.warn("Failed to end assignment - constraint violation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new Object() {
                public final String error = "Unable to modify assignment due to database constraints. " +
                        "Please ensure the end date is valid and doesn't conflict with other assignments.";
            });
        }
    }

    /**
     * GET /api/shift-profiles/suggest/{shiftId} - Get suggested profiles for shift
     * Returns profiles that match the shift's attributes
     */
    @GetMapping("/suggest/{shiftId}")
    public ResponseEntity<List<ShiftProfileDTO>> getSuggestedProfiles(
            @PathVariable Long shiftId) {
        log.info("GET /api/shift-profiles/suggest/{} - Get suggested profiles", shiftId);

        try {
            List<ShiftProfileDTO> suggested = shiftProfileService.findSuggestedProfiles(shiftId)
                    .stream()
                    .map(ShiftProfileDTO::fromEntity)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(suggested);
        } catch (IllegalArgumentException e) {
            log.warn("Suggestion failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // ============================================================================
    // Health Check
    // ============================================================================

    /**
     * GET /api/shift-profiles/health - Health check for shift profile service
     */
    @GetMapping("/health")
    public ResponseEntity<Object> health() {
        return ResponseEntity.ok(new Object() {
            public final String status = "UP";
            public final String service = "ShiftProfileService";
        });
    }
}
