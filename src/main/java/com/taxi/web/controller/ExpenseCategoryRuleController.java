package com.taxi.web.controller;

import com.taxi.domain.expense.model.ExpenseCategoryRule;
import com.taxi.domain.expense.model.MatchingCriteria;
import com.taxi.domain.expense.service.CabMatchingService;
import com.taxi.domain.expense.service.CabMatchingService.MatchingCabsPreview;
import com.taxi.domain.expense.service.ExpenseAutoApplyService;
import com.taxi.domain.expense.service.ExpenseAutoApplyService.AutoApplyResult;
import com.taxi.domain.expense.service.ExpenseAutoApplyService.BulkCreateResult;
import com.taxi.domain.expense.service.ExpenseCategoryService;
import com.taxi.web.dto.expense.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * ExpenseCategoryRuleController - REST endpoints for managing expense category rules
 */
@RestController
@RequestMapping("/expense-category-rules")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
public class ExpenseCategoryRuleController {

    private final ExpenseCategoryService categoryService;
    private final CabMatchingService cabMatchingService;
    private final ExpenseAutoApplyService autoApplyService;
    private final ObjectMapper objectMapper;

    /**
     * Get rule for a category
     */
    @GetMapping("/{categoryId}")
    public ResponseEntity<?> getCategoryRule(@PathVariable Long categoryId) {
        log.info("Getting category rule for category ID: {}", categoryId);

        ExpenseCategoryRule rule = categoryService.getCategoryRule(categoryId);
        if (rule == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(ExpenseCategoryRuleDTO.fromEntity(rule));
    }

    /**
     * Create or update category rule
     */
    @PostMapping("/{categoryId}")
    public ResponseEntity<?> createOrUpdateRule(
            @PathVariable Long categoryId,
            @RequestBody CreateExpenseCategoryRuleRequest request) {

        log.info("Creating/updating category rule for category ID: {}", categoryId);

        try {
            // Convert matching criteria to JSON
            String criteriaJson = objectMapper.writeValueAsString(request.getMatchingCriteria());

            ExpenseCategoryRule rule = ExpenseCategoryRule.builder()
                .configurationMode(request.getConfigurationMode())
                .matchingCriteria(criteriaJson)
                .isActive(true)
                .build();

            // Update indexed flags based on criteria
            if (request.getMatchingCriteria().getShareType() != null) {
                rule.setHasShareTypeRule(true);
            }
            if (request.getMatchingCriteria().getHasAirportLicense() != null) {
                rule.setHasAirportLicenseRule(true);
            }
            if (request.getMatchingCriteria().getCabShiftType() != null) {
                rule.setHasCabShiftTypeRule(true);
            }
            if (request.getMatchingCriteria().getCabType() != null) {
                rule.setHasCabTypeRule(true);
            }

            ExpenseCategoryRule saved = categoryService.updateCategoryRule(categoryId, rule);
            return ResponseEntity.ok(ExpenseCategoryRuleDTO.fromEntity(saved));

        } catch (Exception e) {
            log.error("Error creating/updating category rule: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get cabs matching category rules
     */
    @GetMapping("/{categoryId}/matching-cabs")
    public ResponseEntity<?> getMatchingCabs(@PathVariable Long categoryId) {
        log.info("Getting matching cabs for category ID: {}", categoryId);

        ExpenseCategoryRule rule = categoryService.getCategoryRule(categoryId);
        if (rule == null || rule.getMatchingCriteria() == null) {
            return ResponseEntity.ok(new MatchingCabsPreviewDTO(0, null, false));
        }

        try {
            MatchingCriteria criteria = objectMapper.readValue(
                rule.getMatchingCriteria(), MatchingCriteria.class);

            MatchingCabsPreview preview = cabMatchingService.getMatchingPreview(criteria);
            return ResponseEntity.ok(MatchingCabsPreviewDTO.fromService(preview));

        } catch (Exception e) {
            log.error("Error getting matching cabs: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Auto-apply expenses to matching cabs
     */
    @PostMapping("/{categoryId}/auto-apply")
    public ResponseEntity<?> autoApplyExpenses(
            @PathVariable Long categoryId,
            @RequestBody AutoApplyRequest request,
            Authentication authentication) {

        log.info("Auto-applying expenses for category ID: {}", categoryId);

        try {
            ExpenseCategoryRule rule = categoryService.getCategoryRule(categoryId);
            if (rule == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Category rule not found");
                return ResponseEntity.badRequest().body(error);
            }

            String createdBy = authentication != null ? authentication.getName() : "SYSTEM";

            AutoApplyResult result = autoApplyService.autoApplyExpenses(
                rule,
                request.getAmount(),
                request.getBillingMethod(),
                request.getEffectiveFrom(),
                createdBy
            );

            return ResponseEntity.ok(AutoApplyResultDTO.fromService(result));

        } catch (Exception e) {
            log.error("Error auto-applying expenses: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Bulk create expenses with individual amounts
     */
    @PostMapping("/{categoryId}/bulk-create")
    public ResponseEntity<?> bulkCreateExpenses(
            @PathVariable Long categoryId,
            @RequestBody BulkCreateRequest request,
            Authentication authentication) {

        log.info("Bulk creating expenses for category ID: {} with {} cabs", categoryId, request.getCabAmounts().size());

        try {
            ExpenseCategoryRule rule = categoryService.getCategoryRule(categoryId);
            if (rule == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Category rule not found");
                return ResponseEntity.badRequest().body(error);
            }

            String createdBy = authentication != null ? authentication.getName() : "SYSTEM";

            BulkCreateResult result = autoApplyService.bulkCreateIndividualExpenses(
                rule,
                request.getCabAmounts(),
                request.getBillingMethod(),
                request.getEffectiveFrom(),
                createdBy
            );

            return ResponseEntity.ok(BulkCreateResultDTO.fromService(result));

        } catch (Exception e) {
            log.error("Error bulk creating expenses: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Preview match for unsaved criteria
     */
    @PostMapping("/preview-match")
    public ResponseEntity<?> previewMatch(
            @RequestBody PreviewMatchRequest request) {

        log.info("Previewing match for criteria");

        try {
            MatchingCabsPreview preview = cabMatchingService.getMatchingPreview(
                request.getMatchingCriteria());

            return ResponseEntity.ok(MatchingCabsPreviewDTO.fromService(preview));

        } catch (Exception e) {
            log.error("Error previewing match: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
