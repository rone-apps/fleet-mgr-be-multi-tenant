package com.taxi.web.controller;

import com.taxi.domain.tax.service.TaxCommissionService;
import com.taxi.web.dto.tax.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tax-commissions")
@RequiredArgsConstructor
@CrossOrigin
public class TaxCommissionController {

    private final TaxCommissionService service;

    // ==================== TAX TYPES ====================

    @GetMapping("/tax-types")
    public List<TaxTypeDTO> getAllTaxTypes() {
        return service.getAllTaxTypes();
    }

    @GetMapping("/tax-types/active")
    public List<TaxTypeDTO> getActiveTaxTypes() {
        return service.getActiveTaxTypes();
    }

    @PostMapping("/tax-types")
    public TaxTypeDTO createTaxType(@RequestBody TaxTypeDTO dto) {
        return service.createTaxType(dto);
    }

    @PutMapping("/tax-types/{id}")
    public TaxTypeDTO updateTaxType(@PathVariable Long id, @RequestBody TaxTypeDTO dto) {
        return service.updateTaxType(id, dto);
    }

    // ==================== TAX RATES ====================

    @GetMapping("/tax-types/{taxTypeId}/rates")
    public List<TaxRateDTO> getTaxRates(@PathVariable Long taxTypeId) {
        return service.getTaxRates(taxTypeId);
    }

    @PostMapping("/tax-types/{taxTypeId}/rates")
    public TaxRateDTO createTaxRate(@PathVariable Long taxTypeId, @RequestBody TaxRateDTO dto) {
        return service.createTaxRate(taxTypeId, dto);
    }

    // ==================== TAX ASSIGNMENTS ====================

    @GetMapping("/tax-assignments")
    public List<TaxCategoryAssignmentDTO> getAllTaxAssignments() {
        return service.getAllTaxAssignments();
    }

    @GetMapping("/tax-assignments/active")
    public List<TaxCategoryAssignmentDTO> getActiveTaxAssignments() {
        return service.getActiveTaxAssignments();
    }

    @PostMapping("/tax-assignments")
    public ResponseEntity<?> assignTaxToCategory(@RequestBody Map<String, Object> body) {
        try {
            Long taxTypeId = Long.valueOf(body.get("taxTypeId").toString());
            Long categoryId = Long.valueOf(body.get("expenseCategoryId").toString());
            String notes = body.get("notes") != null ? body.get("notes").toString() : null;
            return ResponseEntity.ok(service.assignTaxToCategory(taxTypeId, categoryId, notes));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/tax-assignments/{id}/unassign")
    public TaxCategoryAssignmentDTO unassignTax(@PathVariable Long id) {
        return service.unassignTaxFromCategory(id);
    }

    // ==================== COMMISSION TYPES ====================

    @GetMapping("/commission-types")
    public List<CommissionTypeDTO> getAllCommissionTypes() {
        return service.getAllCommissionTypes();
    }

    @GetMapping("/commission-types/active")
    public List<CommissionTypeDTO> getActiveCommissionTypes() {
        return service.getActiveCommissionTypes();
    }

    @PostMapping("/commission-types")
    public CommissionTypeDTO createCommissionType(@RequestBody CommissionTypeDTO dto) {
        return service.createCommissionType(dto);
    }

    @PutMapping("/commission-types/{id}")
    public CommissionTypeDTO updateCommissionType(@PathVariable Long id, @RequestBody CommissionTypeDTO dto) {
        return service.updateCommissionType(id, dto);
    }

    // ==================== COMMISSION RATES ====================

    @GetMapping("/commission-types/{typeId}/rates")
    public List<CommissionRateDTO> getCommissionRates(@PathVariable Long typeId) {
        return service.getCommissionRates(typeId);
    }

    @PostMapping("/commission-types/{typeId}/rates")
    public CommissionRateDTO createCommissionRate(@PathVariable Long typeId, @RequestBody CommissionRateDTO dto) {
        return service.createCommissionRate(typeId, dto);
    }

    // ==================== COMMISSION ASSIGNMENTS ====================

    @GetMapping("/commission-assignments")
    public List<CommissionCategoryAssignmentDTO> getAllCommissionAssignments() {
        return service.getAllCommissionAssignments();
    }

    @GetMapping("/commission-assignments/active")
    public List<CommissionCategoryAssignmentDTO> getActiveCommissionAssignments() {
        return service.getActiveCommissionAssignments();
    }

    @PostMapping("/commission-assignments")
    public ResponseEntity<?> assignCommissionToCategory(@RequestBody Map<String, Object> body) {
        try {
            Long typeId = Long.valueOf(body.get("commissionTypeId").toString());
            Long categoryId = Long.valueOf(body.get("revenueCategoryId").toString());
            String notes = body.get("notes") != null ? body.get("notes").toString() : null;
            return ResponseEntity.ok(service.assignCommissionToCategory(typeId, categoryId, notes));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/commission-assignments/{id}/unassign")
    public CommissionCategoryAssignmentDTO unassignCommission(@PathVariable Long id) {
        return service.unassignCommissionFromCategory(id);
    }
}
