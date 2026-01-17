package com.taxi.web.controller;

import com.taxi.domain.revenue.entity.RevenueCategory;
import com.taxi.domain.revenue.entity.RevenueCategory.AppliesTo;
import com.taxi.domain.revenue.entity.RevenueCategory.CategoryType;
import com.taxi.domain.revenue.service.RevenueCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/revenue-categories")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class RevenueCategoryController {

    private final RevenueCategoryService revenueCategoryService;

    /**
     * Create new revenue category
     * POST /api/revenue-categories
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<RevenueCategory> create(@RequestBody RevenueCategory category) {
        try {
            RevenueCategory created = revenueCategoryService.create(category);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.error("Invalid category: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get category by ID
     * GET /api/revenue-categories/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DRIVER')")
    public ResponseEntity<RevenueCategory> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(revenueCategoryService.getById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get category by code
     * GET /api/revenue-categories/code/{code}
     */
    @GetMapping("/code/{code}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DRIVER')")
    public ResponseEntity<RevenueCategory> getByCategoryCode(@PathVariable String code) {
        try {
            return ResponseEntity.ok(revenueCategoryService.getByCategoryCode(code));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all categories
     * GET /api/revenue-categories
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DRIVER')")
    public ResponseEntity<List<RevenueCategory>> getAll() {
        return ResponseEntity.ok(revenueCategoryService.getAll());
    }

    /**
     * Get all active categories
     * GET /api/revenue-categories/active
     */
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DRIVER')")
    public ResponseEntity<List<RevenueCategory>> getAllActive() {
        return ResponseEntity.ok(revenueCategoryService.getAllActive());
    }

    /**
     * Get categories by applies_to
     * GET /api/revenue-categories/applies-to/{appliesTo}
     */
    @GetMapping("/applies-to/{appliesTo}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DRIVER')")
    public ResponseEntity<List<RevenueCategory>> getByAppliesTo(@PathVariable AppliesTo appliesTo) {
        return ResponseEntity.ok(revenueCategoryService.getByAppliesTo(appliesTo));
    }

    /**
     * Get active categories by applies_to
     * GET /api/revenue-categories/active/applies-to/{appliesTo}
     */
    @GetMapping("/active/applies-to/{appliesTo}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DRIVER')")
    public ResponseEntity<List<RevenueCategory>> getActiveByAppliesTo(@PathVariable AppliesTo appliesTo) {
        return ResponseEntity.ok(revenueCategoryService.getActiveByAppliesTo(appliesTo));
    }

    /**
     * Get categories by type
     * GET /api/revenue-categories/type/{categoryType}
     */
    @GetMapping("/type/{categoryType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<RevenueCategory>> getByCategoryType(@PathVariable CategoryType categoryType) {
        return ResponseEntity.ok(revenueCategoryService.getByCategoryType(categoryType));
    }

    /**
     * Get active categories by type
     * GET /api/revenue-categories/active/type/{categoryType}
     */
    @GetMapping("/active/type/{categoryType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<RevenueCategory>> getActiveByCategoryType(@PathVariable CategoryType categoryType) {
        return ResponseEntity.ok(revenueCategoryService.getActiveByCategoryType(categoryType));
    }

    /**
     * Get active categories by applies_to and type
     * GET /api/revenue-categories/active/filter?appliesTo=DRIVER&categoryType=FIXED
     */
    @GetMapping("/active/filter")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<RevenueCategory>> getActiveByAppliesToAndType(
            @RequestParam AppliesTo appliesTo,
            @RequestParam CategoryType categoryType) {
        return ResponseEntity.ok(revenueCategoryService.getActiveByAppliesToAndType(appliesTo, categoryType));
    }

    /**
     * Search categories by name
     * GET /api/revenue-categories/search/name?term=bonus
     */
    @GetMapping("/search/name")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<RevenueCategory>> searchByName(@RequestParam String term) {
        return ResponseEntity.ok(revenueCategoryService.searchByName(term));
    }

    /**
     * Search categories by code
     * GET /api/revenue-categories/search/code?term=LEASE
     */
    @GetMapping("/search/code")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<RevenueCategory>> searchByCode(@RequestParam String term) {
        return ResponseEntity.ok(revenueCategoryService.searchByCode(term));
    }

    /**
     * Update category
     * PUT /api/revenue-categories/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<RevenueCategory> update(@PathVariable Long id, @RequestBody RevenueCategory category) {
        try {
            return ResponseEntity.ok(revenueCategoryService.update(id, category));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Activate category
     * PUT /api/revenue-categories/{id}/activate
     */
    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<RevenueCategory> activate(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(revenueCategoryService.activate(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Deactivate category
     * PUT /api/revenue-categories/{id}/deactivate
     */
    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<RevenueCategory> deactivate(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(revenueCategoryService.deactivate(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Soft delete category
     * DELETE /api/revenue-categories/{id}/soft
     */
    @DeleteMapping("/{id}/soft")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> softDelete(@PathVariable Long id) {
        try {
            revenueCategoryService.softDelete(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Hard delete category
     * DELETE /api/revenue-categories/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            revenueCategoryService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Check if category code exists
     * GET /api/revenue-categories/exists/code/{code}
     */
    @GetMapping("/exists/code/{code}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Boolean> existsByCategoryCode(@PathVariable String code) {
        return ResponseEntity.ok(revenueCategoryService.existsByCategoryCode(code));
    }

    /**
     * Count active categories
     * GET /api/revenue-categories/count/active
     */
    @GetMapping("/count/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<Long> countActive() {
        return ResponseEntity.ok(revenueCategoryService.countActive());
    }

    /**
     * Count categories by type
     * GET /api/revenue-categories/count/type/{categoryType}
     */
    @GetMapping("/count/type/{categoryType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<Long> countByType(@PathVariable CategoryType categoryType) {
        return ResponseEntity.ok(revenueCategoryService.countByType(categoryType));
    }
}