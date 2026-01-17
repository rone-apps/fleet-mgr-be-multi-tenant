package com.taxi.web.controller;

import com.taxi.domain.revenue.entity.OtherRevenue;
import com.taxi.domain.revenue.service.OtherRevenueService;
import com.taxi.web.dto.revenue.OtherRevenueDTO;
import com.taxi.web.dto.revenue.OtherRevenueRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/other-revenues")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class OtherRevenueController {
    
    private final OtherRevenueService revenueService;
    
    // Create revenue
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<?> createRevenue(@Valid @RequestBody OtherRevenueRequest request) {
        try {
            OtherRevenueDTO created = revenueService.createRevenue(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.warn("Bad request creating revenue: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Error creating revenue: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error creating revenue", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "An unexpected error occurred"));
        }
    }
    
    // Update revenue
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<?> updateRevenue(
            @PathVariable Long id,
            @Valid @RequestBody OtherRevenueRequest request) {
        try {
            OtherRevenueDTO updated = revenueService.updateRevenue(id, request);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.warn("Bad request updating revenue {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Error updating revenue {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Unexpected error updating revenue {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "An unexpected error occurred"));
        }
    }
    
    // Get revenue by ID
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DISPATCHER')")
    public ResponseEntity<?> getRevenueById(@PathVariable Long id) {
        try {
            OtherRevenueDTO revenue = revenueService.getRevenueById(id);
            return ResponseEntity.ok(revenue);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    // Get all revenues
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DISPATCHER')")
    public ResponseEntity<List<OtherRevenueDTO>> getAllRevenues(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String driverNumber,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) OtherRevenue.EntityType entityType,
            @RequestParam(required = false) OtherRevenue.PaymentStatus paymentStatus) {
        
        List<OtherRevenueDTO> revenues;
        
        if (startDate != null && endDate != null) {
            if (driverNumber != null && !driverNumber.isBlank()) {
                revenues = revenueService.getRevenuesForDriverBetweenDates(driverNumber, startDate, endDate);
            } else if (categoryId != null || entityType != null || paymentStatus != null) {
                revenues = revenueService.getRevenuesWithFilters(
                    startDate, endDate, categoryId, entityType, paymentStatus);
            } else {
                revenues = revenueService.getRevenuesBetweenDates(startDate, endDate);
            }
        } else {
            revenues = revenueService.getAllRevenues();
        }
        
        return ResponseEntity.ok(revenues);
    }
    
    // Get revenues by category
    @GetMapping("/category/{categoryId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DISPATCHER')")
    public ResponseEntity<List<OtherRevenueDTO>> getRevenuesByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(revenueService.getRevenuesByCategory(categoryId));
    }
    
    // Get revenues by entity
    @GetMapping("/entity/{entityType}/{entityId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DISPATCHER')")
    public ResponseEntity<List<OtherRevenueDTO>> getRevenuesByEntity(
            @PathVariable OtherRevenue.EntityType entityType,
            @PathVariable Long entityId) {
        return ResponseEntity.ok(revenueService.getRevenuesByEntity(entityType, entityId));
    }
    
    // Get revenues by payment status
    @GetMapping("/status/{paymentStatus}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DISPATCHER')")
    public ResponseEntity<List<OtherRevenueDTO>> getRevenuesByPaymentStatus(
            @PathVariable OtherRevenue.PaymentStatus paymentStatus) {
        return ResponseEntity.ok(revenueService.getRevenuesByPaymentStatus(paymentStatus));
    }
    
    // Mark revenue as paid
    @PostMapping("/{id}/mark-paid")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<?> markAsPaid(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate paymentDate) {
        try {
            OtherRevenueDTO updated = revenueService.markAsPaid(id, paymentDate);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    // Cancel revenue
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> cancelRevenue(@PathVariable Long id) {
        try {
            OtherRevenueDTO updated = revenueService.cancelRevenue(id);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    // Get total revenue
    @GetMapping("/total")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DISPATCHER')")
    public ResponseEntity<Map<String, BigDecimal>> getTotalRevenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        BigDecimal total = revenueService.getTotalRevenue(startDate, endDate);
        Map<String, BigDecimal> response = new HashMap<>();
        response.put("total", total);
        return ResponseEntity.ok(response);
    }
    
    // Get total revenue by category
    @GetMapping("/total/category/{categoryId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DISPATCHER')")
    public ResponseEntity<Map<String, BigDecimal>> getTotalRevenueByCategory(
            @PathVariable Long categoryId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        BigDecimal total = revenueService.getTotalRevenueByCategory(categoryId, startDate, endDate);
        Map<String, BigDecimal> response = new HashMap<>();
        response.put("total", total);
        return ResponseEntity.ok(response);
    }
    
    // Get total revenue by entity
    @GetMapping("/total/entity/{entityType}/{entityId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DISPATCHER')")
    public ResponseEntity<Map<String, BigDecimal>> getTotalRevenueByEntity(
            @PathVariable OtherRevenue.EntityType entityType,
            @PathVariable Long entityId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        BigDecimal total = revenueService.getTotalRevenueByEntity(entityType, entityId, startDate, endDate);
        Map<String, BigDecimal> response = new HashMap<>();
        response.put("total", total);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/meta/entity-types")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DISPATCHER')")
    public ResponseEntity<List<OtherRevenue.EntityType>> getEntityTypes() {
        return ResponseEntity.ok(Arrays.asList(OtherRevenue.EntityType.values()));
    }

    @GetMapping("/meta/revenue-types")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DISPATCHER')")
    public ResponseEntity<List<OtherRevenue.RevenueType>> getRevenueTypes() {
        return ResponseEntity.ok(Arrays.asList(OtherRevenue.RevenueType.values()));
    }

    @GetMapping("/meta/payment-statuses")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DISPATCHER')")
    public ResponseEntity<List<OtherRevenue.PaymentStatus>> getPaymentStatuses() {
        return ResponseEntity.ok(Arrays.asList(OtherRevenue.PaymentStatus.values()));
    }
    
    // Delete is intentionally not provided - revenues should not be deleted for audit trail
}
