package com.taxi.domain.expense.repository;

import com.taxi.domain.expense.model.OneTimeExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Repository for OneTimeExpense entity
 */
@Repository
public interface OneTimeExpenseRepository extends JpaRepository<OneTimeExpense, Long> {

    /**
     * Find expenses for a specific entity
     */
    List<OneTimeExpense> findByEntityTypeAndEntityId(
        OneTimeExpense.EntityType entityType,
        Long entityId
    );

    /**
     * Find expenses for entity in date range
     */
    @Query("SELECT ote FROM OneTimeExpense ote WHERE ote.entityType = :entityType " +
           "AND ote.entityId = :entityId " +
           "AND ote.expenseDate BETWEEN :startDate AND :endDate " +
           "ORDER BY ote.expenseDate DESC")
    List<OneTimeExpense> findForEntityBetween(
        @Param("entityType") OneTimeExpense.EntityType entityType,
        @Param("entityId") Long entityId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find all expenses in date range
     */
    @Query("SELECT ote FROM OneTimeExpense ote " +
           "WHERE ote.expenseDate BETWEEN :startDate AND :endDate " +
           "ORDER BY ote.expenseDate DESC")
    List<OneTimeExpense> findBetween(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Query("SELECT ote FROM OneTimeExpense ote " +
           "WHERE ote.expenseDate BETWEEN :startDate AND :endDate " +
           "AND (" +
           "     (ote.entityType = 'DRIVER' AND ote.entityId = :driverId) OR " +
           "     (ote.entityType = 'OWNER' AND ote.entityId = :driverId) OR " +
           "     (ote.driver IS NOT NULL AND ote.driver.id = :driverId) OR " +
           "     (ote.owner IS NOT NULL AND ote.owner.id = :driverId)" +
           ") " +
           "ORDER BY ote.expenseDate DESC, ote.createdAt DESC")
    List<OneTimeExpense> findForDriverBetween(
        @Param("driverId") Long driverId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find expenses by date range (simple method name - used by FixedExpenseReportService)
     * ✅ ADDED - Spring Data JPA will auto-implement this
     */
    List<OneTimeExpense> findByExpenseDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Find expenses by category
     */
    @Query("SELECT ote FROM OneTimeExpense ote " +
           "WHERE ote.expenseCategory.id = :categoryId " +
           "ORDER BY ote.expenseDate DESC")
    List<OneTimeExpense> findByExpenseCategoryId(@Param("categoryId") Long categoryId);

    /**
     * Find reimbursable expenses
     */
    @Query("SELECT ote FROM OneTimeExpense ote " +
           "WHERE ote.isReimbursable = true AND ote.isReimbursed = false " +
           "ORDER BY ote.expenseDate ASC")
    List<OneTimeExpense> findPendingReimbursements();

    /**
     * Find reimbursable expenses for specific payer
     */
    @Query("SELECT ote FROM OneTimeExpense ote " +
           "WHERE ote.isReimbursable = true AND ote.isReimbursed = false " +
           "AND ote.paidBy = :paidBy " +
           "ORDER BY ote.expenseDate ASC")
    List<OneTimeExpense> findPendingReimbursementsByPaidBy(
        @Param("paidBy") OneTimeExpense.PaidBy paidBy
    );

    /**
     * Find expenses without receipt
     */
    @Query("SELECT ote FROM OneTimeExpense ote " +
           "WHERE ote.receiptUrl IS NULL OR ote.receiptUrl = '' " +
           "AND ote.expenseDate >= :sinceDate " +
           "ORDER BY ote.expenseDate DESC")
    List<OneTimeExpense> findWithoutReceiptSince(@Param("sinceDate") LocalDate sinceDate);

    /**
     * Calculate total expenses for entity in date range
     */
    @Query("SELECT COALESCE(SUM(ote.amount), 0) FROM OneTimeExpense ote " +
           "WHERE ote.entityType = :entityType " +
           "AND ote.entityId = :entityId " +
           "AND ote.expenseDate BETWEEN :startDate AND :endDate")
    BigDecimal calculateTotalForEntityBetween(
        @Param("entityType") OneTimeExpense.EntityType entityType,
        @Param("entityId") Long entityId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Calculate total expenses by category in date range
     */
    @Query("SELECT COALESCE(SUM(ote.amount), 0) FROM OneTimeExpense ote " +
           "WHERE ote.expenseCategory.id = :categoryId " +
           "AND ote.expenseDate BETWEEN :startDate AND :endDate")
    BigDecimal calculateTotalByCategoryBetween(
        @Param("categoryId") Long categoryId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find expenses by responsible party
     */
    @Query("SELECT ote FROM OneTimeExpense ote " +
           "WHERE ote.responsibleParty = :responsibleParty " +
           "AND ote.expenseDate BETWEEN :startDate AND :endDate " +
           "ORDER BY ote.expenseDate DESC")
    List<OneTimeExpense> findByResponsiblePartyBetween(
        @Param("responsibleParty") OneTimeExpense.ResponsibleParty responsibleParty,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * ✅ Find all one-time expenses applicable to a driver/owner based on application type
     * Handles both legacy entity-based and new application-type-based targeting
     *
     * KEY RULE: Shift-based charges (SHIFT_PROFILE, SPECIFIC_SHIFT, SHIFTS_WITH_ATTRIBUTE, ALL_ACTIVE_SHIFTS)
     * are ONLY for OWNERS. Drivers only get driver-specific charges.
     */
    @Query("SELECT DISTINCT ote FROM OneTimeExpense ote " +
           "WHERE ote.expenseDate BETWEEN :startDate AND :endDate " +
           "AND (" +
           "     (ote.entityType = 'DRIVER' AND ote.entityId = :personId) OR " +
           "     (ote.entityType = 'OWNER' AND ote.entityId = :personId) OR " +
           "     (ote.applicationType = 'SPECIFIC_PERSON' AND ote.specificPersonId = :personId) OR " +
           "     (ote.applicationType = 'ALL_OWNERS' AND :isOwner = TRUE) OR " +
           "     (ote.applicationType = 'ALL_ACTIVE_SHIFTS' AND :isOwner = TRUE) OR " +
           "     (ote.applicationType = 'ALL_DRIVERS' AND :isOwner = FALSE) " +
           ") " +
           "ORDER BY ote.expenseDate DESC, ote.createdAt DESC")
    List<OneTimeExpense> findApplicableExpensesBetween(
        @Param("personId") Long personId,
        @Param("isOwner") Boolean isOwner,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find one-time expenses for specific shift profiles in date range
     */
    @Query("SELECT ote FROM OneTimeExpense ote " +
           "WHERE ote.expenseDate BETWEEN :startDate AND :endDate " +
           "AND ote.applicationType = 'SHIFT_PROFILE' " +
           "AND ote.shiftProfileId IN :profileIds " +
           "ORDER BY ote.expenseDate DESC")
    List<OneTimeExpense> findByShiftProfileIdsBetween(
        @Param("profileIds") java.util.List<Long> profileIds,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find one-time expenses for specific shifts in date range
     */
    @Query("SELECT ote FROM OneTimeExpense ote " +
           "WHERE ote.expenseDate BETWEEN :startDate AND :endDate " +
           "AND ote.applicationType = 'SPECIFIC_SHIFT' " +
           "AND ote.specificShiftId IN :shiftIds " +
           "ORDER BY ote.expenseDate DESC")
    List<OneTimeExpense> findBySpecificShiftIdsBetween(
        @Param("shiftIds") java.util.List<Long> shiftIds,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find one-time expenses for shifts with specific attributes in date range
     */
    @Query("SELECT ote FROM OneTimeExpense ote " +
           "WHERE ote.expenseDate BETWEEN :startDate AND :endDate " +
           "AND ote.applicationType = 'SHIFTS_WITH_ATTRIBUTE' " +
           "AND ote.attributeTypeId IN :attributeTypeIds " +
           "ORDER BY ote.expenseDate DESC")
    List<OneTimeExpense> findByAttributeTypeIdsBetween(
        @Param("attributeTypeIds") java.util.List<Long> attributeTypeIds,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}