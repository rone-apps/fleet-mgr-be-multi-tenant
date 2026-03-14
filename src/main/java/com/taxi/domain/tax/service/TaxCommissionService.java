package com.taxi.domain.tax.service;

import com.taxi.domain.expense.model.ExpenseCategory;
import com.taxi.domain.expense.repository.ExpenseCategoryRepository;
import com.taxi.domain.revenue.entity.RevenueCategory;
import com.taxi.domain.revenue.repository.RevenueCategoryRepository;
import com.taxi.domain.tax.model.*;
import com.taxi.domain.tax.repository.*;
import com.taxi.web.dto.tax.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TaxCommissionService {

    private final TaxTypeRepository taxTypeRepository;
    private final TaxRateRepository taxRateRepository;
    private final TaxCategoryAssignmentRepository taxCategoryAssignmentRepository;
    private final CommissionTypeRepository commissionTypeRepository;
    private final CommissionRateRepository commissionRateRepository;
    private final CommissionCategoryAssignmentRepository commissionCategoryAssignmentRepository;
    private final ExpenseCategoryRepository expenseCategoryRepository;
    private final RevenueCategoryRepository revenueCategoryRepository;

    // ==================== TAX TYPES ====================

    public List<TaxTypeDTO> getAllTaxTypes() {
        return taxTypeRepository.findAllByOrderByName().stream()
                .map(this::mapTaxType).collect(Collectors.toList());
    }

    public List<TaxTypeDTO> getActiveTaxTypes() {
        return taxTypeRepository.findByIsActiveTrueOrderByName().stream()
                .map(this::mapTaxType).collect(Collectors.toList());
    }

    @Transactional
    public TaxTypeDTO createTaxType(TaxTypeDTO dto) {
        log.info("Creating tax type: {}", dto.getCode());
        TaxType t = TaxType.builder()
                .code(dto.getCode().toUpperCase())
                .name(dto.getName())
                .description(dto.getDescription())
                .isActive(true)
                .build();
        return mapTaxType(taxTypeRepository.save(t));
    }

    @Transactional
    public TaxTypeDTO updateTaxType(Long id, TaxTypeDTO dto) {
        TaxType t = taxTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tax type not found: " + id));
        t.setName(dto.getName());
        t.setDescription(dto.getDescription());
        t.setIsActive(dto.getIsActive());
        return mapTaxType(taxTypeRepository.save(t));
    }

    // ==================== TAX RATES ====================

    public List<TaxRateDTO> getTaxRates(Long taxTypeId) {
        return taxRateRepository.findByTaxTypeIdOrderByEffectiveFromDesc(taxTypeId).stream()
                .map(this::mapTaxRate).collect(Collectors.toList());
    }

    @Transactional
    public TaxRateDTO createTaxRate(Long taxTypeId, TaxRateDTO dto) {
        TaxType type = taxTypeRepository.findById(taxTypeId)
                .orElseThrow(() -> new RuntimeException("Tax type not found: " + taxTypeId));
        log.info("Creating tax rate {}% for {} effective {}", dto.getRate(), type.getCode(), dto.getEffectiveFrom());

        // Close any current active rate
        taxRateRepository.findActiveRateOnDate(taxTypeId, dto.getEffectiveFrom()).ifPresent(old -> {
            old.setEffectiveTo(dto.getEffectiveFrom().minusDays(1));
            old.setIsActive(false);
            taxRateRepository.save(old);
            log.info("Closed old rate ID {} effective to {}", old.getId(), old.getEffectiveTo());
        });

        TaxRate rate = TaxRate.builder()
                .taxType(type)
                .rate(dto.getRate())
                .effectiveFrom(dto.getEffectiveFrom())
                .isActive(true)
                .notes(dto.getNotes())
                .build();
        return mapTaxRate(taxRateRepository.save(rate));
    }

    // ==================== TAX CATEGORY ASSIGNMENTS ====================

    public List<TaxCategoryAssignmentDTO> getAllTaxAssignments() {
        return taxCategoryAssignmentRepository.findAllWithDetails().stream()
                .map(this::mapTaxAssignment).collect(Collectors.toList());
    }

    public List<TaxCategoryAssignmentDTO> getActiveTaxAssignments() {
        return taxCategoryAssignmentRepository.findAllActiveWithDetails().stream()
                .map(this::mapTaxAssignment).collect(Collectors.toList());
    }

    @Transactional
    public TaxCategoryAssignmentDTO assignTaxToCategory(Long taxTypeId, Long categoryId, String notes) {
        // Check not already assigned
        taxCategoryAssignmentRepository.findActiveAssignment(taxTypeId, categoryId).ifPresent(a -> {
            throw new RuntimeException("This tax is already assigned to this category");
        });

        TaxType type = taxTypeRepository.findById(taxTypeId)
                .orElseThrow(() -> new RuntimeException("Tax type not found"));
        ExpenseCategory cat = expenseCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Expense category not found"));

        log.info("Assigning tax {} to expense category {}", type.getCode(), cat.getCategoryCode());

        TaxCategoryAssignment a = TaxCategoryAssignment.builder()
                .taxType(type)
                .expenseCategory(cat)
                .assignedAt(LocalDate.now())
                .isActive(true)
                .notes(notes)
                .build();
        return mapTaxAssignment(taxCategoryAssignmentRepository.save(a));
    }

    @Transactional
    public TaxCategoryAssignmentDTO unassignTaxFromCategory(Long assignmentId) {
        TaxCategoryAssignment a = taxCategoryAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
        log.info("Unassigning tax {} from category {}", a.getTaxType().getCode(),
                a.getExpenseCategory().getCategoryCode());
        a.setIsActive(false);
        a.setUnassignedAt(LocalDate.now());
        return mapTaxAssignment(taxCategoryAssignmentRepository.save(a));
    }

    // ==================== COMMISSION TYPES ====================

    public List<CommissionTypeDTO> getAllCommissionTypes() {
        return commissionTypeRepository.findAllByOrderByName().stream()
                .map(this::mapCommissionType).collect(Collectors.toList());
    }

    public List<CommissionTypeDTO> getActiveCommissionTypes() {
        return commissionTypeRepository.findByIsActiveTrueOrderByName().stream()
                .map(this::mapCommissionType).collect(Collectors.toList());
    }

    @Transactional
    public CommissionTypeDTO createCommissionType(CommissionTypeDTO dto) {
        log.info("Creating commission type: {}", dto.getCode());
        CommissionType t = CommissionType.builder()
                .code(dto.getCode().toUpperCase())
                .name(dto.getName())
                .description(dto.getDescription())
                .isActive(true)
                .build();
        return mapCommissionType(commissionTypeRepository.save(t));
    }

    @Transactional
    public CommissionTypeDTO updateCommissionType(Long id, CommissionTypeDTO dto) {
        CommissionType t = commissionTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commission type not found: " + id));
        t.setName(dto.getName());
        t.setDescription(dto.getDescription());
        t.setIsActive(dto.getIsActive());
        return mapCommissionType(commissionTypeRepository.save(t));
    }

    // ==================== COMMISSION RATES ====================

    public List<CommissionRateDTO> getCommissionRates(Long commissionTypeId) {
        return commissionRateRepository.findByCommissionTypeIdOrderByEffectiveFromDesc(commissionTypeId).stream()
                .map(this::mapCommissionRate).collect(Collectors.toList());
    }

    @Transactional
    public CommissionRateDTO createCommissionRate(Long commissionTypeId, CommissionRateDTO dto) {
        CommissionType type = commissionTypeRepository.findById(commissionTypeId)
                .orElseThrow(() -> new RuntimeException("Commission type not found: " + commissionTypeId));
        log.info("Creating commission rate {}% for {} effective {}", dto.getRate(), type.getCode(), dto.getEffectiveFrom());

        // Close any current active rate
        commissionRateRepository.findActiveRateOnDate(commissionTypeId, dto.getEffectiveFrom()).ifPresent(old -> {
            old.setEffectiveTo(dto.getEffectiveFrom().minusDays(1));
            old.setIsActive(false);
            commissionRateRepository.save(old);
        });

        CommissionRate rate = CommissionRate.builder()
                .commissionType(type)
                .rate(dto.getRate())
                .effectiveFrom(dto.getEffectiveFrom())
                .isActive(true)
                .notes(dto.getNotes())
                .build();
        return mapCommissionRate(commissionRateRepository.save(rate));
    }

    // ==================== COMMISSION CATEGORY ASSIGNMENTS ====================

    public List<CommissionCategoryAssignmentDTO> getAllCommissionAssignments() {
        return commissionCategoryAssignmentRepository.findAllWithDetails().stream()
                .map(this::mapCommissionAssignment).collect(Collectors.toList());
    }

    public List<CommissionCategoryAssignmentDTO> getActiveCommissionAssignments() {
        return commissionCategoryAssignmentRepository.findAllActiveWithDetails().stream()
                .map(this::mapCommissionAssignment).collect(Collectors.toList());
    }

    @Transactional
    public CommissionCategoryAssignmentDTO assignCommissionToCategory(Long commissionTypeId, Long categoryId, String notes) {
        commissionCategoryAssignmentRepository.findActiveAssignment(commissionTypeId, categoryId).ifPresent(a -> {
            throw new RuntimeException("This commission is already assigned to this category");
        });

        CommissionType type = commissionTypeRepository.findById(commissionTypeId)
                .orElseThrow(() -> new RuntimeException("Commission type not found"));
        RevenueCategory cat = revenueCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Revenue category not found"));

        log.info("Assigning commission {} to revenue category {}", type.getCode(), cat.getCategoryCode());

        CommissionCategoryAssignment a = CommissionCategoryAssignment.builder()
                .commissionType(type)
                .revenueCategory(cat)
                .assignedAt(LocalDate.now())
                .isActive(true)
                .notes(notes)
                .build();
        return mapCommissionAssignment(commissionCategoryAssignmentRepository.save(a));
    }

    @Transactional
    public CommissionCategoryAssignmentDTO unassignCommissionFromCategory(Long assignmentId) {
        CommissionCategoryAssignment a = commissionCategoryAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
        log.info("Unassigning commission {} from category {}", a.getCommissionType().getCode(),
                a.getRevenueCategory().getCategoryCode());
        a.setIsActive(false);
        a.setUnassignedAt(LocalDate.now());
        return mapCommissionAssignment(commissionCategoryAssignmentRepository.save(a));
    }

    // ==================== MAPPERS ====================

    private TaxTypeDTO mapTaxType(TaxType t) {
        // Get current active rate
        String currentRate = taxRateRepository.findActiveRateOnDate(t.getId(), LocalDate.now())
                .map(r -> r.getRate().stripTrailingZeros().toPlainString() + "%")
                .orElse("No active rate");

        return TaxTypeDTO.builder()
                .id(t.getId())
                .code(t.getCode())
                .name(t.getName())
                .description(t.getDescription())
                .isActive(t.getIsActive())
                .currentRate(currentRate)
                .createdAt(t.getCreatedAt())
                .build();
    }

    private TaxRateDTO mapTaxRate(TaxRate r) {
        return TaxRateDTO.builder()
                .id(r.getId())
                .taxTypeId(r.getTaxType().getId())
                .taxTypeName(r.getTaxType().getName())
                .rate(r.getRate())
                .effectiveFrom(r.getEffectiveFrom())
                .effectiveTo(r.getEffectiveTo())
                .isActive(r.getIsActive())
                .notes(r.getNotes())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private TaxCategoryAssignmentDTO mapTaxAssignment(TaxCategoryAssignment a) {
        return TaxCategoryAssignmentDTO.builder()
                .id(a.getId())
                .taxTypeId(a.getTaxType().getId())
                .taxTypeName(a.getTaxType().getName())
                .expenseCategoryId(a.getExpenseCategory().getId())
                .expenseCategoryName(a.getExpenseCategory().getCategoryName())
                .expenseCategoryCode(a.getExpenseCategory().getCategoryCode())
                .assignedAt(a.getAssignedAt())
                .unassignedAt(a.getUnassignedAt())
                .isActive(a.getIsActive())
                .notes(a.getNotes())
                .createdAt(a.getCreatedAt())
                .build();
    }

    private CommissionTypeDTO mapCommissionType(CommissionType t) {
        String currentRate = commissionRateRepository.findActiveRateOnDate(t.getId(), LocalDate.now())
                .map(r -> r.getRate().stripTrailingZeros().toPlainString() + "%")
                .orElse("No active rate");

        return CommissionTypeDTO.builder()
                .id(t.getId())
                .code(t.getCode())
                .name(t.getName())
                .description(t.getDescription())
                .isActive(t.getIsActive())
                .currentRate(currentRate)
                .createdAt(t.getCreatedAt())
                .build();
    }

    private CommissionRateDTO mapCommissionRate(CommissionRate r) {
        return CommissionRateDTO.builder()
                .id(r.getId())
                .commissionTypeId(r.getCommissionType().getId())
                .commissionTypeName(r.getCommissionType().getName())
                .rate(r.getRate())
                .effectiveFrom(r.getEffectiveFrom())
                .effectiveTo(r.getEffectiveTo())
                .isActive(r.getIsActive())
                .notes(r.getNotes())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private CommissionCategoryAssignmentDTO mapCommissionAssignment(CommissionCategoryAssignment a) {
        return CommissionCategoryAssignmentDTO.builder()
                .id(a.getId())
                .commissionTypeId(a.getCommissionType().getId())
                .commissionTypeName(a.getCommissionType().getName())
                .revenueCategoryId(a.getRevenueCategory().getId())
                .revenueCategoryName(a.getRevenueCategory().getCategoryName())
                .revenueCategoryCode(a.getRevenueCategory().getCategoryCode())
                .assignedAt(a.getAssignedAt())
                .unassignedAt(a.getUnassignedAt())
                .isActive(a.getIsActive())
                .notes(a.getNotes())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
