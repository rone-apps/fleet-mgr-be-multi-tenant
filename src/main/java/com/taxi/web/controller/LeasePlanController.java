package com.taxi.web.controller;

import com.taxi.domain.lease.model.LeasePlan;
import com.taxi.domain.lease.model.LeaseRate;
import com.taxi.domain.lease.service.LeasePlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for Lease Plan Management
 * 
 * BUSINESS RULES:
 * 1. Plans CANNOT be deleted (only deactivated)
 * 2. Rates CANNOT be edited or deleted
 * 3. To change rates: create new plan and close current one
 * 4. Only ONE plan can be active at a time (no date overlaps)
 * 5. Plans auto-deactivate when end date is reached
 */
@RestController
@RequestMapping("/lease-plans")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class LeasePlanController {

    private final LeasePlanService leasePlanService;

    /**
     * Test endpoint - no security for debugging
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> test() {
        return ResponseEntity.ok(Map.of(
            "status", "LeasePlanController is working",
            "endpoint", "/api/lease-plans",
            "message", "If you see this, the controller is accessible"
        ));
    }

    /**
     * Create a new lease plan
     * Validates no date overlap with existing plans
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> createPlan(@RequestBody LeasePlan plan) {
        try {
            LeasePlan created = leasePlanService.createPlan(plan);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.error("Validation error creating lease plan: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating lease plan", e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to create lease plan: " + e.getMessage()));
        }
    }

    /**
     * Update lease plan (ONLY name, notes, or end date)
     * Start date and rates CANNOT be changed
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> updatePlan(
            @PathVariable Long id,
            @RequestBody LeasePlan updates) {
        try {
            LeasePlan updated = leasePlanService.updatePlan(id, updates);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Validation error updating lease plan: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating lease plan", e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to update lease plan: " + e.getMessage()));
        }
    }

    /**
     * Deactivate a plan by setting end date
     * This is the proper way to "close" a plan
     */
    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> deactivatePlan(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            LeasePlan deactivated = leasePlanService.deactivatePlan(id, endDate);
            return ResponseEntity.ok(deactivated);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Validation error deactivating plan: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error deactivating lease plan", e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to deactivate plan: " + e.getMessage()));
        }
    }

    /**
     * DELETE is NOT ALLOWED - returns error with explanation
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> deletePlan(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
            .body(Map.of(
                "error", "Lease plans cannot be deleted",
                "reason", "Historical reporting requires all plans to be preserved",
                "solution", "Use the deactivate endpoint to close a plan instead"
            ));
    }

    /**
     * Get plan by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<?> getPlanById(@PathVariable Long id) {
        try {
            LeasePlan plan = leasePlanService.getPlanById(id);
            return ResponseEntity.ok(plan);
        } catch (Exception e) {
            log.error("Error getting lease plan: {}", id, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all plans (active and inactive)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<LeasePlan>> getAllPlans() {
        try {
            List<LeasePlan> plans = leasePlanService.getAllPlans();
            return ResponseEntity.ok(plans);
        } catch (Exception e) {
            log.error("Error getting all lease plans", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get only active plans
     */
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<LeasePlan>> getActivePlans() {
        try {
            List<LeasePlan> plans = leasePlanService.getActivePlans();
            return ResponseEntity.ok(plans);
        } catch (Exception e) {
            log.error("Error getting active lease plans", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get plan active on a specific date
     */
    @GetMapping("/active-on")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<?> getPlanActiveOnDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            LeasePlan plan = leasePlanService.getPlanActiveOnDate(date);
            if (plan == null) {
                return ResponseEntity.ok(Map.of("message", "No plan active on " + date));
            }
            return ResponseEntity.ok(plan);
        } catch (Exception e) {
            log.error("Error getting plan for date: {}", date, e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get rates for a plan
     */
    @GetMapping("/{planId}/rates")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<?> getRates(@PathVariable Long planId) {
        try {
            LeasePlan plan = leasePlanService.getPlanById(planId);
            return ResponseEntity.ok(plan.getLeaseRates());
        } catch (Exception e) {
            log.error("Error getting rates for plan: {}", planId, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Add rates to a plan
     * Rates can ONLY be added, never edited or deleted
     */
    @PostMapping("/{planId}/rates")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> addRates(
            @PathVariable Long planId,
            @RequestBody List<LeaseRate> rates) {
        try {
            LeasePlan updated = leasePlanService.addRates(planId, rates);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Error adding rates to plan: {}", planId, e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to add rates: " + e.getMessage()));
        }
    }

    /**
     * Add rates to a plan
     * Rates can ONLY be added, never edited or deleted
     */
    @PostMapping("/{planId}/rate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> addRate(
            @PathVariable Long planId,
            @RequestBody LeaseRate rate) {
        try {
            LeasePlan updated = leasePlanService.addRate(planId, rate);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Error adding rates to plan: {}", planId, e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to add rates: " + e.getMessage()));
        }
    }

    /**
     * UPDATE rate - NOT ALLOWED - returns error with explanation
     */
    @PutMapping("/rates/{rateId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> updateRate(
            @PathVariable Long rateId,
            @RequestBody LeaseRate updates) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
            .body(Map.of(
                "error", "Lease rates cannot be edited",
                "reason", "Editing rates would affect historical reports and lease expense calculations",
                "solution", "Create a new lease plan with the updated rates and close the current plan"
            ));
    }

    /**
     * DELETE rate - NOT ALLOWED - returns error with explanation
     */
    @DeleteMapping("/rate/{rateId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> deleteRate(@PathVariable Long rateId) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
            .body(Map.of(
                "error", "Lease rates cannot be deleted",
                "reason", "Deleting rates would affect historical reports and lease expense calculations",
                "solution", "Create a new lease plan with the correct rates and close the current plan"
            ));
    }

    /**
     * Get business rules documentation
     */
    @GetMapping("/business-rules")
    public ResponseEntity<Map<String, Object>> getBusinessRules() {
        Map<String, Object> rules = new HashMap<>();
        
        rules.put("overview", "Lease plans manage daily cab lease rates with strict audit trail requirements");
        
        rules.put("rules", List.of(
            Map.of(
                "rule", "Plans cannot be deleted",
                "reason", "Historical reporting requires all plans to be preserved",
                "action", "Use deactivate to close a plan"
            ),
            Map.of(
                "rule", "Rates cannot be edited or deleted",
                "reason", "Editing rates would affect historical lease expense calculations",
                "action", "Create new plan with updated rates and close current plan"
            ),
            Map.of(
                "rule", "Only one plan can be active at a time",
                "reason", "Prevents confusion about which rates apply on a given date",
                "action", "Ensure date ranges don't overlap when creating plans"
            ),
            Map.of(
                "rule", "Plans auto-deactivate at end date",
                "reason", "Ensures plans don't remain active past their intended period",
                "action", "System automatically deactivates when end date is reached"
            )
        ));
        
        rules.put("workflow", Map.of(
            "current_plan", "Winter 2024 (Nov 1 - Feb 28)",
            "to_change_rates", List.of(
                "1. Create new plan 'Spring 2025' (Mar 1 - May 31) with new rates",
                "2. System validates no overlap",
                "3. Old plan auto-deactivates on Feb 28",
                "4. New plan activates on Mar 1"
            )
        ));
        
        return ResponseEntity.ok(rules);
    }

    /**
     * Manual trigger to auto-deactivate expired plans
     * Normally runs on schedule, but can be triggered manually
     */
    @PostMapping("/auto-deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, String>> autoDeactivateExpiredPlans() {
        try {
            leasePlanService.autoDeactivateExpiredPlans();
            return ResponseEntity.ok(Map.of("message", "Auto-deactivation completed"));
        } catch (Exception e) {
            log.error("Error auto-deactivating plans", e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to auto-deactivate: " + e.getMessage()));
        }
    }
}
