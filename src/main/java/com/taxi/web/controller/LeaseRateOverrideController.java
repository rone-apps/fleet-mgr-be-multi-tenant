package com.taxi.web.controller;

import com.taxi.domain.lease.model.LeaseRateOverride;
import com.taxi.domain.lease.service.LeaseRateOverrideService;
import com.taxi.web.dto.lease.LeaseRateOverrideDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for managing custom lease rate overrides
 */
@RestController
@RequestMapping("/lease-rate-overrides")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class LeaseRateOverrideController {

    private final LeaseRateOverrideService leaseRateOverrideService;

    /**
     * Create a new lease rate override
     * POST /api/lease-rate-overrides
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> createOverride(@RequestBody LeaseRateOverrideDTO dto) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            LeaseRateOverride override = convertToEntity(dto);
            LeaseRateOverride created = leaseRateOverrideService.createOverride(override);
            
            response.put("success", true);
            response.put("message", "Lease rate override created successfully");
            response.put("data", convertToDTO(created));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to create lease rate override");
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Update an existing override
     * PUT /api/lease-rate-overrides/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> updateOverride(
            @PathVariable Long id,
            @RequestBody LeaseRateOverrideDTO dto) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            LeaseRateOverride updates = convertToEntity(dto);
            LeaseRateOverride updated = leaseRateOverrideService.updateOverride(id, updates);
            
            response.put("success", true);
            response.put("message", "Lease rate override updated successfully");
            response.put("data", convertToDTO(updated));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to update lease rate override");
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Delete an override
     * DELETE /api/lease-rate-overrides/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> deleteOverride(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            leaseRateOverrideService.deleteOverride(id);
            
            response.put("success", true);
            response.put("message", "Lease rate override deleted successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to delete lease rate override");
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Deactivate an override (soft delete)
     * POST /api/lease-rate-overrides/{id}/deactivate
     */
    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> deactivateOverride(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            LeaseRateOverride updated = leaseRateOverrideService.deactivateOverride(id);
            
            response.put("success", true);
            response.put("message", "Lease rate override deactivated successfully");
            response.put("data", convertToDTO(updated));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to deactivate lease rate override");
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Activate an override
     * POST /api/lease-rate-overrides/{id}/activate
     */
    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> activateOverride(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            LeaseRateOverride updated = leaseRateOverrideService.activateOverride(id);
            
            response.put("success", true);
            response.put("message", "Lease rate override activated successfully");
            response.put("data", convertToDTO(updated));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to activate lease rate override");
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Get all overrides for an owner
     * GET /api/lease-rate-overrides/owner/{ownerDriverNumber}
     */
    @GetMapping("/owner/{ownerDriverNumber}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<Map<String, Object>> getOwnerOverrides(
            @PathVariable String ownerDriverNumber,
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<LeaseRateOverride> overrides;
            
            if (activeOnly) {
                overrides = leaseRateOverrideService.getActiveOwnerOverrides(ownerDriverNumber);
            } else {
                overrides = leaseRateOverrideService.getOwnerOverrides(ownerDriverNumber);
            }
            
            List<LeaseRateOverrideDTO> dtos = overrides.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
            
            response.put("success", true);
            response.put("message", "Overrides retrieved successfully");
            response.put("count", dtos.size());
            response.put("data", dtos);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to retrieve overrides");
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Get all lease rate overrides (all owners)
     * GET /api/lease-rate-overrides
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<Map<String, Object>> getAllOverrides(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<LeaseRateOverride> overrides;
            
            if (activeOnly) {
                overrides = leaseRateOverrideService.getAllActiveOverrides();
            } else {
                overrides = leaseRateOverrideService.getAllOverrides();
            }
            
            List<LeaseRateOverrideDTO> dtos = overrides.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
            
            response.put("success", true);
            response.put("message", "All overrides retrieved successfully");
            response.put("count", dtos.size());
            response.put("data", dtos);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to retrieve all overrides");
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Get the applicable lease rate for specific criteria
     * GET /api/lease-rate-overrides/lookup
     */
    @GetMapping("/lookup")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<Map<String, Object>> lookupLeaseRate(
            @RequestParam String ownerDriverNumber,
            @RequestParam String cabNumber,
            @RequestParam String shiftType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            BigDecimal rate = leaseRateOverrideService.getApplicableLeaseRate(
                ownerDriverNumber, cabNumber, shiftType, date);
            
            response.put("success", true);
            response.put("ownerDriverNumber", ownerDriverNumber);
            response.put("cabNumber", cabNumber);
            response.put("shiftType", shiftType);
            response.put("date", date.toString());
            
            if (rate != null) {
                response.put("leaseRate", rate);
                response.put("isOverride", true);
                response.put("message", "Custom override rate found");
            } else {
                response.put("leaseRate", null);
                response.put("isOverride", false);
                response.put("message", "No override found - use default rate");
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to lookup lease rate");
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Get all currently active overrides
     * GET /api/lease-rate-overrides/active
     */
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<Map<String, Object>> getAllActiveOverrides() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<LeaseRateOverride> overrides = leaseRateOverrideService.getAllActiveOverrides();
            
            List<LeaseRateOverrideDTO> dtos = overrides.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
            
            response.put("success", true);
            response.put("message", "Active overrides retrieved successfully");
            response.put("count", dtos.size());
            response.put("data", dtos);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to retrieve active overrides");
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Get overrides expiring soon
     * GET /api/lease-rate-overrides/expiring?days=30
     */
    @GetMapping("/expiring")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> getExpiringSoon(
            @RequestParam(defaultValue = "30") int days) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<LeaseRateOverride> overrides = leaseRateOverrideService.getExpiringSoon(days);
            
            List<LeaseRateOverrideDTO> dtos = overrides.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
            
            response.put("success", true);
            response.put("message", "Expiring overrides retrieved successfully");
            response.put("count", dtos.size());
            response.put("days", days);
            response.put("data", dtos);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to retrieve expiring overrides");
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Bulk create overrides
     * POST /api/lease-rate-overrides/bulk
     */
    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> createBulkOverrides(
            @RequestBody Map<String, Object> request) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String ownerDriverNumber = (String) request.get("ownerDriverNumber");
            String cabNumber = (String) request.get("cabNumber");
            String shiftType = (String) request.get("shiftType");
            @SuppressWarnings("unchecked")
            List<String> daysOfWeek = (List<String>) request.get("daysOfWeek");
            BigDecimal leaseRate = new BigDecimal(request.get("leaseRate").toString());
            LocalDate startDate = request.get("startDate") != null 
                ? LocalDate.parse(request.get("startDate").toString()) 
                : LocalDate.now();
            LocalDate endDate = request.get("endDate") != null 
                ? LocalDate.parse(request.get("endDate").toString()) 
                : null;
            String notes = (String) request.get("notes");
            
            List<LeaseRateOverride> overrides = leaseRateOverrideService.createBulkOverrides(
                ownerDriverNumber, cabNumber, shiftType, daysOfWeek, 
                leaseRate, startDate, endDate, notes);
            
            List<LeaseRateOverrideDTO> dtos = overrides.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
            
            response.put("success", true);
            response.put("message", "Bulk overrides created successfully");
            response.put("count", dtos.size());
            response.put("data", dtos);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to create bulk overrides");
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // Helper methods for conversion
    
    private LeaseRateOverride convertToEntity(LeaseRateOverrideDTO dto) {
        return LeaseRateOverride.builder()
            .ownerDriverNumber(dto.getOwnerDriverNumber())
            .cabNumber(dto.getCabNumber())
            .shiftType(dto.getShiftType())
            .dayOfWeek(dto.getDayOfWeek())
            .leaseRate(dto.getLeaseRate())
            .startDate(dto.getStartDate())
            .endDate(dto.getEndDate())
            .isActive(dto.getIsActive())
            .priority(dto.getPriority())
            .notes(dto.getNotes())
            .build();
    }
    
    private LeaseRateOverrideDTO convertToDTO(LeaseRateOverride entity) {
        return LeaseRateOverrideDTO.builder()
            .id(entity.getId())
            .ownerDriverNumber(entity.getOwnerDriverNumber())
            .cabNumber(entity.getCabNumber())
            .shiftType(entity.getShiftType())
            .dayOfWeek(entity.getDayOfWeek())
            .leaseRate(entity.getLeaseRate())
            .startDate(entity.getStartDate())
            .endDate(entity.getEndDate())
            .isActive(entity.getIsActive())
            .priority(entity.getPriority())
            .notes(entity.getNotes())
            .build();
    }
}