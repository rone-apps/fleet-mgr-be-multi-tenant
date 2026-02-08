package com.taxi.domain.expense.repository;

import com.taxi.domain.expense.model.ExpenseCategoryRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ExpenseCategoryRuleRepository - Data access for category matching rules
 */
@Repository
public interface ExpenseCategoryRuleRepository extends JpaRepository<ExpenseCategoryRule, Long> {

    /**
     * Find rule by expense category ID
     */
    Optional<ExpenseCategoryRule> findByExpenseCategoryId(Long categoryId);

    /**
     * Find all rules by configuration mode
     */
    List<ExpenseCategoryRule> findByConfigurationMode(ExpenseCategoryRule.ConfigurationMode mode);

    /**
     * Find all active rules
     */
    List<ExpenseCategoryRule> findByIsActiveTrue();

    /**
     * Find rules that have share type criteria
     */
    @Query("SELECT r FROM ExpenseCategoryRule r WHERE r.hasShareTypeRule = true AND r.isActive = true")
    List<ExpenseCategoryRule> findWithShareTypeRules();

    /**
     * Find rules that have airport license criteria
     */
    @Query("SELECT r FROM ExpenseCategoryRule r WHERE r.hasAirportLicenseRule = true AND r.isActive = true")
    List<ExpenseCategoryRule> findWithAirportLicenseRules();

    /**
     * Find rules that have cab shift type criteria
     */
    @Query("SELECT r FROM ExpenseCategoryRule r WHERE r.hasCabShiftTypeRule = true AND r.isActive = true")
    List<ExpenseCategoryRule> findWithCabShiftTypeRules();
}
