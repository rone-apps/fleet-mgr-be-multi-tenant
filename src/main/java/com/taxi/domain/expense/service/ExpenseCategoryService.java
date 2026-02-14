package com.taxi.domain.expense.service;

import com.taxi.domain.expense.model.ExpenseCategory;
import com.taxi.domain.expense.model.ExpenseCategoryRule;
import com.taxi.domain.expense.repository.ExpenseCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ExpenseCategoryService {

    private final ExpenseCategoryRepository expenseCategoryRepository;

    public ExpenseCategory createCategory(ExpenseCategory category) {
        log.info("Creating expense category: {}", category.getCategoryName());
        
        if (expenseCategoryRepository.existsByCategoryCode(category.getCategoryCode())) {
            throw new RuntimeException("Category with code already exists: " + category.getCategoryCode());
        }
        
        return expenseCategoryRepository.save(category);
    }

    public ExpenseCategory updateCategory(Long id, ExpenseCategory updates) {
        log.info("Updating expense category ID: {}", id);

        ExpenseCategory category = expenseCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found: " + id));

        if (updates.getCategoryName() != null) {
            category.setCategoryName(updates.getCategoryName());
        }
        if (updates.getDescription() != null) {
            category.setDescription(updates.getDescription());
        }
        if (updates.getCategoryType() != null) {
            category.setCategoryType(updates.getCategoryType());
        }
        if (updates.getAppliesTo() != null) {
            category.setAppliesTo(updates.getAppliesTo());
        }
        if (updates.getApplicationType() != null) {
            category.setApplicationType(updates.getApplicationType());
        }
        if (updates.getShiftProfileId() != null) {
            category.setShiftProfileId(updates.getShiftProfileId());
        }
        if (updates.getSpecificShiftId() != null) {
            category.setSpecificShiftId(updates.getSpecificShiftId());
        }
        if (updates.getSpecificOwnerId() != null) {
            category.setSpecificOwnerId(updates.getSpecificOwnerId());
        }
        if (updates.getSpecificDriverId() != null) {
            category.setSpecificDriverId(updates.getSpecificDriverId());
        }
        if (updates.getAttributeTypeId() != null) {
            category.setAttributeTypeId(updates.getAttributeTypeId());
        }

        return expenseCategoryRepository.save(category);
    }

    public void deleteCategory(Long id) {
        log.info("Deleting expense category ID: {}", id);
        expenseCategoryRepository.deleteById(id);
    }

    public void activateCategory(Long id) {
        ExpenseCategory category = expenseCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found: " + id));
        category.activate();
        expenseCategoryRepository.save(category);
    }

    public void deactivateCategory(Long id) {
        ExpenseCategory category = expenseCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found: " + id));
        category.deactivate();
        expenseCategoryRepository.save(category);
    }

    public ExpenseCategory getCategory(Long id) {
        return expenseCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found: " + id));
    }

    public List<ExpenseCategory> getAllCategories() {
        return expenseCategoryRepository.findAll();
    }

    public List<ExpenseCategory> getActiveCategories() {
        return expenseCategoryRepository.findByIsActiveTrue();
    }

    public List<ExpenseCategory> getCategoriesByType(ExpenseCategory.CategoryType type) {
        return expenseCategoryRepository.findByCategoryType(type);
    }

    public List<ExpenseCategory> getCategoriesByAppliesTo(ExpenseCategory.AppliesTo appliesTo) {
        return expenseCategoryRepository.findByAppliesTo(appliesTo);
    }

    /**
     * Create category with associated rule
     */
    public ExpenseCategory createCategoryWithRule(ExpenseCategory category, ExpenseCategoryRule rule) {
        log.info("Creating category {} with rule", category.getCategoryName());

        ExpenseCategory saved = expenseCategoryRepository.save(category);

        if (rule != null) {
            rule.setExpenseCategory(saved);
            rule.setId(null); // Ensure new rule
            // Note: Rule will be saved through cascade
        }

        return saved;
    }

    /**
     * Get category rule if it exists
     */
    public ExpenseCategoryRule getCategoryRule(Long categoryId) {
        // This would normally come from a repository, but we can access through the category
        ExpenseCategory category = getCategory(categoryId);
        return category.getCategoryRule();
    }

    /**
     * Update or create category rule
     */
    public ExpenseCategoryRule updateCategoryRule(Long categoryId, ExpenseCategoryRule rule) {
        log.info("Updating category rule for category ID: {}", categoryId);

        ExpenseCategory category = getCategory(categoryId);
        rule.setExpenseCategory(category);
        category.setCategoryRule(rule);

        expenseCategoryRepository.save(category);

        return rule;
    }
}
