package com.taxi.web.controller;

import com.taxi.domain.revenue.entity.RevenueCategory;
import com.taxi.domain.revenue.entity.RevenueCategory.AppliesTo;
import com.taxi.domain.revenue.entity.RevenueCategory.CategoryType;
import com.taxi.domain.revenue.service.RevenueCategoryService;
import com.taxi.web.dto.revenue.RevenueCategoryDTO;
import com.taxi.web.dto.revenue.CreateRevenueCategoryRequest;
import com.taxi.web.dto.revenue.ApplicationPreviewDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

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
    public ResponseEntity<RevenueCategoryDTO> create(@Valid @RequestBody CreateRevenueCategoryRequest request) {
        try {
            RevenueCategory created = revenueCategoryService.create(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(RevenueCategoryDTO.fromEntity(created));
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
    public ResponseEntity<RevenueCategoryDTO> getById(@PathVariable Long id) {
        try {
            RevenueCategory category = revenueCategoryService.getById(id);
            return ResponseEntity.ok(RevenueCategoryDTO.fromEntity(category));
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
    public ResponseEntity<List<RevenueCategoryDTO>> getAll() {
        List<RevenueCategoryDTO> categories = revenueCategoryService.getAll()
                .stream()
                .map(RevenueCategoryDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(categories);
    }

    /**
     * Get all active categories
     * GET /api/revenue-categories/active
     */
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DRIVER')")
    public ResponseEntity<List<RevenueCategoryDTO>> getAllActive() {
        List<RevenueCategoryDTO> categories = revenueCategoryService.getAllActive()
                .stream()
                .map(RevenueCategoryDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(categories);
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
    public ResponseEntity<RevenueCategoryDTO> update(@PathVariable Long id, @Valid @RequestBody CreateRevenueCategoryRequest request) {
        try {
            RevenueCategory updated = revenueCategoryService.update(id, request);
            return ResponseEntity.ok(RevenueCategoryDTO.fromEntity(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Preview application target for a revenue category
     * GET /api/revenue-categories/{id}/preview-application
     */
    @GetMapping("/{id}/preview-application")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApplicationPreviewDTO> previewApplication(@PathVariable Long id) {
        try {
            RevenueCategory category = revenueCategoryService.getById(id);
            ApplicationPreviewDTO preview = buildApplicationPreview(category);
            return ResponseEntity.ok(preview);
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
    public ResponseEntity<RevenueCategoryDTO> activate(@PathVariable Long id) {
        try {
            RevenueCategory activated = revenueCategoryService.activate(id);
            return ResponseEntity.ok(RevenueCategoryDTO.fromEntity(activated));
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
    public ResponseEntity<RevenueCategoryDTO> deactivate(@PathVariable Long id) {
        try {
            RevenueCategory deactivated = revenueCategoryService.deactivate(id);
            return ResponseEntity.ok(RevenueCategoryDTO.fromEntity(deactivated));
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
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
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

    /**
     * Build a preview of which entities this category applies to
     */
    private ApplicationPreviewDTO buildApplicationPreview(RevenueCategory category) {
        String description = switch (category.getApplicationType()) {
            case SHIFT_PROFILE -> "This revenue category will apply to all shifts with the specified profile";
            case SPECIFIC_SHIFT -> "This revenue category will apply to a single specific shift";
            case SPECIFIC_PERSON -> "This revenue category will apply to a specific person (driver or owner)";
            case ALL_OWNERS -> "This revenue category will apply to all owners";
            case ALL_DRIVERS -> "This revenue category will apply to all drivers";
            case ALL_ACTIVE_SHIFTS -> "This revenue category will apply to all currently active shifts";
            case SHIFTS_WITH_ATTRIBUTE -> "This revenue category will apply to all shifts with the specified attribute";
        };

        return ApplicationPreviewDTO.builder()
                .applicationType(category.getApplicationType())
                .description(description)
                .build();
    }
}