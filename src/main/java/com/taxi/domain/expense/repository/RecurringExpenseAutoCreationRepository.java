package com.taxi.domain.expense.repository;

import com.taxi.domain.expense.model.ExpenseCategoryRule;
import com.taxi.domain.expense.model.RecurringExpense;
import com.taxi.domain.expense.model.RecurringExpenseAutoCreation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * RecurringExpenseAutoCreationRepository - Audit trail for auto-created expenses
 */
@Repository
public interface RecurringExpenseAutoCreationRepository extends JpaRepository<RecurringExpenseAutoCreation, Long> {

    /**
     * Find audit entries by recurring expense
     */
    List<RecurringExpenseAutoCreation> findByRecurringExpense(RecurringExpense expense);

    /**
     * Find audit entries by category rule
     */
    List<RecurringExpenseAutoCreation> findByCategoryRule(ExpenseCategoryRule rule);

    /**
     * Count auto-created expenses by rule and type
     */
    @Query("SELECT COUNT(a) FROM RecurringExpenseAutoCreation a " +
           "WHERE a.categoryRule.id = :ruleId AND a.creationType = :creationType")
    long countByRuleAndCreationType(@Param("ruleId") Long ruleId,
                                    @Param("creationType") RecurringExpenseAutoCreation.CreationType creationType);

    /**
     * Find all auto-created entries for a category rule
     */
    @Query("SELECT a FROM RecurringExpenseAutoCreation a " +
           "WHERE a.categoryRule.id = :ruleId " +
           "ORDER BY a.createdAt DESC")
    List<RecurringExpenseAutoCreation> findByRuleIdOrderByCreatedAtDesc(@Param("ruleId") Long ruleId);
}
