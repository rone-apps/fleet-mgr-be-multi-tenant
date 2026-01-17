package com.taxi.domain.expense.repository;

import com.taxi.domain.expense.model.RecurringExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for RecurringExpense entity
 */
@Repository
public interface RecurringExpenseRepository extends JpaRepository<RecurringExpense, Long> {

    /**
     * Find all active recurring expenses
     */
    List<RecurringExpense> findByIsActiveTrue();

    /**
     * Find recurring expenses for a specific entity
     */
    List<RecurringExpense> findByEntityTypeAndEntityId(
        RecurringExpense.EntityType entityType, 
        Long entityId
    );

    /**
     * Find active recurring expenses for a specific entity
     */
    @Query("SELECT re FROM RecurringExpense re WHERE re.entityType = :entityType " +
           "AND re.entityId = :entityId AND re.isActive = true")
    List<RecurringExpense> findActiveByEntityTypeAndEntityId(
        @Param("entityType") RecurringExpense.EntityType entityType,
        @Param("entityId") Long entityId
    );

    /**
     * Find recurring expenses effective on a specific date
     */
    @Query("SELECT re FROM RecurringExpense re WHERE re.isActive = true " +
           "AND re.effectiveFrom <= :date " +
           "AND (re.effectiveTo IS NULL OR re.effectiveTo >= :date)")
    List<RecurringExpense> findEffectiveOn(@Param("date") LocalDate date);

    /**
     * Find recurring expenses for entity effective on date
     */
    @Query("SELECT re FROM RecurringExpense re WHERE re.entityType = :entityType " +
           "AND re.entityId = :entityId AND re.isActive = true " +
           "AND re.effectiveFrom <= :date " +
           "AND (re.effectiveTo IS NULL OR re.effectiveTo >= :date)")
    List<RecurringExpense> findEffectiveForEntityOn(
        @Param("entityType") RecurringExpense.EntityType entityType,
        @Param("entityId") Long entityId,
        @Param("date") LocalDate date
    );

    /**
     * Find recurring expenses by category
     */
    @Query("SELECT re FROM RecurringExpense re WHERE re.expenseCategory.id = :categoryId " +
           "AND re.isActive = true")
    List<RecurringExpense> findActiveByExpenseCategoryId(@Param("categoryId") Long categoryId);

    /**
     * Find recurring expenses for date range
     */
    // @Query("SELECT re FROM RecurringExpense re WHERE re.isActive = true " +
    //        "AND ((re.effectiveFrom <= :endDate AND (re.effectiveTo IS NULL OR re.effectiveTo >= :startDate)))")
    // List<RecurringExpense> findEffectiveBetween(
    //     @Param("startDate") LocalDate startDate,
    //     @Param("endDate") LocalDate endDate
    // );

    @Query("SELECT re FROM RecurringExpense re WHERE re.isActive = true " +
       "AND (DATE(re.effectiveFrom) <= :endDate) " +
       "AND (re.effectiveTo IS NULL OR DATE(re.effectiveTo) >= :startDate)")
List<RecurringExpense> findEffectiveBetween(
    @Param("startDate") LocalDate startDate,
    @Param("endDate") LocalDate endDate
);


    /**
     * Find recurring expenses for entity in date range
     */
    @Query("SELECT re FROM RecurringExpense re WHERE re.entityType = :entityType " +
           "AND re.entityId = :entityId " +
           "AND ((re.effectiveFrom <= :endDate AND (re.effectiveTo IS NULL OR re.effectiveTo >= :startDate)))")
    List<RecurringExpense> findForEntityBetween(
        @Param("entityType") RecurringExpense.EntityType entityType,
        @Param("entityId") Long entityId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}
