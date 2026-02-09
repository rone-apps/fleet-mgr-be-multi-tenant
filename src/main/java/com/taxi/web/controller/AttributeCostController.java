package com.taxi.web.controller;

import com.taxi.domain.cab.model.AttributeCost;
import com.taxi.domain.cab.service.AttributeCostService;
import com.taxi.domain.shift.service.ShiftChargeCalculationService;
import com.taxi.web.dto.cab.attribute.AttributeCostDTO;
import com.taxi.web.dto.cab.attribute.CreateAttributeCostRequest;
import com.taxi.web.dto.cab.attribute.UpdateAttributeCostRequest;
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
 * REST Controller for Attribute Cost management
 *
 * Manages pricing for custom attributes assigned to shifts
 * Supports historical pricing with temporal tracking (effective dates)
 */
@RestController
@RequestMapping("/attribute-costs")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AttributeCostController {

    private final AttributeCostService attributeCostService;
    private final ShiftChargeCalculationService chargeCalculationService;

    /**
     * Get all attribute costs
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<AttributeCostDTO>> getAll() {
        log.info("GET /attribute-costs - Get all attribute costs");
        List<AttributeCostDTO> costs = attributeCostService.getAllCurrentlyActive()
                .stream()
                .map(AttributeCostDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(costs);
    }

    /**
     * Get costs for a specific attribute type
     */
    @GetMapping("/attribute/{attributeTypeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<AttributeCostDTO>> getCostsByAttributeType(
            @PathVariable Long attributeTypeId) {
        log.info("GET /attribute-costs/attribute/{} - Get costs for attribute", attributeTypeId);
        List<AttributeCostDTO> costs = attributeCostService.getCostsByAttributeType(attributeTypeId)
                .stream()
                .map(AttributeCostDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(costs);
    }

    /**
     * Get attribute cost by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<AttributeCostDTO> getById(@PathVariable Long id) {
        log.info("GET /attribute-costs/{} - Get attribute cost", id);
        try {
            AttributeCost cost = attributeCostService.getById(id);
            return ResponseEntity.ok(AttributeCostDTO.fromEntity(cost));
        } catch (RuntimeException e) {
            log.error("Error fetching attribute cost: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get active cost for an attribute on a specific date
     */
    @GetMapping("/attribute/{attributeTypeId}/on-date")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<AttributeCostDTO> getActiveOn(
            @PathVariable Long attributeTypeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.info("GET /attribute-costs/attribute/{}/on-date?date={}", attributeTypeId, date);
        return attributeCostService.getActiveOn(attributeTypeId, date)
                .map(cost -> ResponseEntity.ok(AttributeCostDTO.fromEntity(cost)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Create a new attribute cost
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> create(
            @Valid @RequestBody CreateAttributeCostRequest request,
            Authentication authentication) {
        log.info("POST /attribute-costs - Create attribute cost: attributeTypeId={}, price={}",
                request.getAttributeTypeId(), request.getPrice());

        try {
            String username = authentication != null ? authentication.getName() : "system";

            AttributeCost cost = attributeCostService.create(
                    request.getAttributeTypeId(),
                    request.getPrice(),
                    AttributeCost.BillingUnit.valueOf(request.getBillingUnit()),
                    request.getEffectiveFrom(),
                    request.getEffectiveTo(),
                    username
            );

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Attribute cost created successfully");
            response.put("attributeCost", AttributeCostDTO.fromEntity(cost));

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (RuntimeException e) {
            log.error("Error creating attribute cost: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Update an attribute cost
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAttributeCostRequest request,
            Authentication authentication) {
        log.info("PUT /attribute-costs/{} - Update attribute cost: price={}", id, request.getPrice());

        try {
            String username = authentication != null ? authentication.getName() : "system";

            AttributeCost cost = attributeCostService.update(
                    id,
                    request.getPrice(),
                    AttributeCost.BillingUnit.valueOf(request.getBillingUnit()),
                    request.getEffectiveTo(),
                    username
            );

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Attribute cost updated successfully");
            response.put("attributeCost", AttributeCostDTO.fromEntity(cost));

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Error updating attribute cost: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * End an attribute cost (set end date)
     */
    @PostMapping("/{id}/end")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> endCost(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {
        log.info("POST /attribute-costs/{}/end - End attribute cost: endDate={}", id, endDate);

        try {
            String username = authentication != null ? authentication.getName() : "system";

            AttributeCost cost = attributeCostService.endCost(id, endDate, username);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Attribute cost ended successfully");
            response.put("attributeCost", AttributeCostDTO.fromEntity(cost));

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Error ending attribute cost: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Delete an attribute cost
     * Only allows deletion of future costs, not historical ones
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        log.info("DELETE /attribute-costs/{} - Delete attribute cost", id);

        try {
            attributeCostService.delete(id);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Attribute cost deleted successfully");

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Error deleting attribute cost: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Calculate shift charges for a date range
     * Shows what a shift should be charged for based on attributes during the period
     */
    @GetMapping("/shift/{shiftId}/charges")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<?> calculateShiftCharges(
            @PathVariable Long shiftId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("GET /attribute-costs/shift/{}/charges - Calculate charges from {} to {}",
                shiftId, startDate, endDate);

        try {
            ShiftChargeCalculationService.ShiftChargeResult result =
                    chargeCalculationService.calculateShiftCharges(shiftId, startDate, endDate);

            Map<String, Object> response = new HashMap<>();
            response.put("chargeCalculation", result);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Error calculating shift charges: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
