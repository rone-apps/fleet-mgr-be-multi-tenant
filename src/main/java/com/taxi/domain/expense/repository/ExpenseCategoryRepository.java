package com.taxi.domain.expense.repository;

import com.taxi.domain.expense.model.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ExpenseCategory entity
 */
@Repository
public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Long> {

    Optional<ExpenseCategory> findByCategoryCode(String categoryCode);

    List<ExpenseCategory> findByIsActiveTrue();

    List<ExpenseCategory> findByCategoryType(ExpenseCategory.CategoryType categoryType);

    List<ExpenseCategory> findByAppliesTo(ExpenseCategory.AppliesTo appliesTo);

    @Query("SELECT ec FROM ExpenseCategory ec WHERE ec.isActive = true AND ec.categoryType = :categoryType")
    List<ExpenseCategory> findActiveByCategoryType(ExpenseCategory.CategoryType categoryType);

    @Query("SELECT ec FROM ExpenseCategory ec WHERE ec.isActive = true AND ec.appliesTo = :appliesTo")
    List<ExpenseCategory> findActiveByAppliesTo(ExpenseCategory.AppliesTo appliesTo);

    boolean existsByCategoryCode(String categoryCode);
}
