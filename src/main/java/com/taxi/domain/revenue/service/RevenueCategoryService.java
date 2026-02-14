package com.taxi.domain.revenue.service;

import com.taxi.domain.revenue.entity.RevenueCategory;
import com.taxi.domain.revenue.entity.RevenueCategory.AppliesTo;
import com.taxi.domain.revenue.entity.RevenueCategory.CategoryType;
import com.taxi.domain.revenue.repository.RevenueCategoryRepository;
import com.taxi.web.dto.revenue.CreateRevenueCategoryRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RevenueCategoryService {

    private final RevenueCategoryRepository revenueCategoryRepository;

    /**
     * Create new revenue category from request DTO
     */
    public RevenueCategory create(CreateRevenueCategoryRequest request) {
        log.info("Creating revenue category: {}", request.getCategoryCode());

        // Validate unique constraints
        if (revenueCategoryRepository.existsByCategoryCode(request.getCategoryCode())) {
            throw new IllegalArgumentException("Category code already exists: " + request.getCategoryCode());
        }

        // Build entity from request
        RevenueCategory category = RevenueCategory.builder()
                .categoryCode(request.getCategoryCode())
                .categoryName(request.getCategoryName())
                .description(request.getDescription())
                .appliesTo(request.getAppliesTo())
                .categoryType(request.getCategoryType())
                .applicationType(request.getApplicationType())
                .shiftProfileId(request.getShiftProfileId())
                .specificShiftId(request.getSpecificShiftId())
                .specificOwnerId(request.getSpecificOwnerId())
                .specificDriverId(request.getSpecificDriverId())
                .isActive(request.getIsActive())
                .build();

        return revenueCategoryRepository.save(category);
    }

    /**
     * Create new revenue category (legacy overload for backward compatibility)
     */
    public RevenueCategory createFromEntity(RevenueCategory category) {
        log.info("Creating revenue category: {}", category.getCategoryCode());

        // Validate unique constraints
        if (revenueCategoryRepository.existsByCategoryCode(category.getCategoryCode())) {
            throw new IllegalArgumentException("Category code already exists: " + category.getCategoryCode());
        }

        return revenueCategoryRepository.save(category);
    }

    /**
     * Get category by ID
     */
    @Transactional(readOnly = true)
    public RevenueCategory getById(Long id) {
        return revenueCategoryRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Revenue category not found with id: " + id));
    }

    /**
     * Get category by code
     */
    @Transactional(readOnly = true)
    public RevenueCategory getByCategoryCode(String categoryCode) {
        return revenueCategoryRepository.findByCategoryCode(categoryCode)
            .orElseThrow(() -> new RuntimeException("Revenue category not found with code: " + categoryCode));
    }

    /**
     * Get category by name
     */
    @Transactional(readOnly = true)
    public RevenueCategory getByCategoryName(String categoryName) {
        return revenueCategoryRepository.findByCategoryName(categoryName)
            .orElseThrow(() -> new RuntimeException("Revenue category not found with name: " + categoryName));
    }

    /**
     * Get all categories
     */
    @Transactional(readOnly = true)
    public List<RevenueCategory> getAll() {
        return revenueCategoryRepository.findAll();
    }

    /**
     * Get all active categories
     */
    @Transactional(readOnly = true)
    public List<RevenueCategory> getAllActive() {
        return revenueCategoryRepository.findByIsActiveTrue();
    }

    /**
     * Get categories by applies_to
     */
    @Transactional(readOnly = true)
    public List<RevenueCategory> getByAppliesTo(AppliesTo appliesTo) {
        return revenueCategoryRepository.findByAppliesTo(appliesTo);
    }

    /**
     * Get active categories by applies_to
     */
    @Transactional(readOnly = true)
    public List<RevenueCategory> getActiveByAppliesTo(AppliesTo appliesTo) {
        return revenueCategoryRepository.findActiveByAppliesTo(appliesTo);
    }

    /**
     * Get categories by type
     */
    @Transactional(readOnly = true)
    public List<RevenueCategory> getByCategoryType(CategoryType categoryType) {
        return revenueCategoryRepository.findByCategoryType(categoryType);
    }

    /**
     * Get active categories by type
     */
    @Transactional(readOnly = true)
    public List<RevenueCategory> getActiveByCategoryType(CategoryType categoryType) {
        return revenueCategoryRepository.findActiveByCategoryType(categoryType);
    }

    /**
     * Get active categories by applies_to and type
     */
    @Transactional(readOnly = true)
    public List<RevenueCategory> getActiveByAppliesToAndType(AppliesTo appliesTo, CategoryType categoryType) {
        return revenueCategoryRepository.findActiveByAppliesToAndType(appliesTo, categoryType);
    }

    /**
     * Search categories by name
     */
    @Transactional(readOnly = true)
    public List<RevenueCategory> searchByName(String searchTerm) {
        return revenueCategoryRepository.searchByName(searchTerm);
    }

    /**
     * Search categories by code
     */
    @Transactional(readOnly = true)
    public List<RevenueCategory> searchByCode(String searchTerm) {
        return revenueCategoryRepository.searchByCode(searchTerm);
    }

    /**
     * Update category from request DTO
     */
    public RevenueCategory update(Long id, CreateRevenueCategoryRequest request) {
        RevenueCategory existing = getById(id);

        // Check if code is being changed and if new code already exists
        if (!existing.getCategoryCode().equals(request.getCategoryCode())) {
            if (revenueCategoryRepository.existsByCategoryCode(request.getCategoryCode())) {
                throw new IllegalArgumentException("Category code already exists: " + request.getCategoryCode());
            }
        }

        // Update fields
        existing.setCategoryCode(request.getCategoryCode());
        existing.setCategoryName(request.getCategoryName());
        existing.setDescription(request.getDescription());
        existing.setAppliesTo(request.getAppliesTo());
        existing.setCategoryType(request.getCategoryType());
        existing.setApplicationType(request.getApplicationType());
        existing.setShiftProfileId(request.getShiftProfileId());
        existing.setSpecificShiftId(request.getSpecificShiftId());
        existing.setSpecificOwnerId(request.getSpecificOwnerId());
        existing.setSpecificDriverId(request.getSpecificDriverId());
        existing.setIsActive(request.getIsActive());

        return revenueCategoryRepository.save(existing);
    }

    /**
     * Update category from entity (legacy overload for backward compatibility)
     */
    public RevenueCategory updateFromEntity(Long id, RevenueCategory updatedCategory) {
        RevenueCategory existing = getById(id);

        // Check if code is being changed and if new code already exists
        if (!existing.getCategoryCode().equals(updatedCategory.getCategoryCode())) {
            if (revenueCategoryRepository.existsByCategoryCode(updatedCategory.getCategoryCode())) {
                throw new IllegalArgumentException("Category code already exists: " + updatedCategory.getCategoryCode());
            }
        }

        // Update fields
        existing.setCategoryCode(updatedCategory.getCategoryCode());
        existing.setCategoryName(updatedCategory.getCategoryName());
        existing.setDescription(updatedCategory.getDescription());
        existing.setAppliesTo(updatedCategory.getAppliesTo());
        existing.setCategoryType(updatedCategory.getCategoryType());
        existing.setApplicationType(updatedCategory.getApplicationType());
        existing.setShiftProfileId(updatedCategory.getShiftProfileId());
        existing.setSpecificShiftId(updatedCategory.getSpecificShiftId());
        existing.setSpecificOwnerId(updatedCategory.getSpecificOwnerId());
        existing.setSpecificDriverId(updatedCategory.getSpecificDriverId());
        existing.setIsActive(updatedCategory.getIsActive());

        return revenueCategoryRepository.save(existing);
    }

    /**
     * Activate category
     */
    public RevenueCategory activate(Long id) {
        RevenueCategory category = getById(id);
        category.activate();
        return revenueCategoryRepository.save(category);
    }

    /**
     * Deactivate category
     */
    public RevenueCategory deactivate(Long id) {
        RevenueCategory category = getById(id);
        category.deactivate();
        return revenueCategoryRepository.save(category);
    }

    /**
     * Delete category (soft delete by deactivating)
     */
    public void softDelete(Long id) {
        deactivate(id);
    }

    /**
     * Delete category (hard delete)
     */
    public void delete(Long id) {
        log.warn("Hard deleting revenue category: {}", id);
        revenueCategoryRepository.deleteById(id);
    }

    /**
     * Check if category code exists
     */
    @Transactional(readOnly = true)
    public boolean existsByCategoryCode(String categoryCode) {
        return revenueCategoryRepository.existsByCategoryCode(categoryCode);
    }

    /**
     * Check if category name exists
     */
    @Transactional(readOnly = true)
    public boolean existsByCategoryName(String categoryName) {
        return revenueCategoryRepository.existsByCategoryName(categoryName);
    }

    /**
     * Count active categories
     */
    @Transactional(readOnly = true)
    public Long countActive() {
        return revenueCategoryRepository.countActive();
    }

    /**
     * Count categories by type
     */
    @Transactional(readOnly = true)
    public Long countByType(CategoryType categoryType) {
        return revenueCategoryRepository.countByType(categoryType);
    }
}