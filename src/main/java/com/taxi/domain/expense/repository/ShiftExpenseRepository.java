package com.taxi.domain.expense.repository;

import com.taxi.domain.expense.model.ShiftExpense;
import com.taxi.domain.shift.model.ShiftLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for ShiftExpense entity
 */
@Repository
public interface ShiftExpenseRepository extends JpaRepository<ShiftExpense, Long> {

    /**
     * Find all expenses for a shift log
     */
    List<ShiftExpense> findByShiftLog(ShiftLog shiftLog);

    /**
     * Find all expenses for a shift log by ID
     */
    @Query("SELECT e FROM ShiftExpense e WHERE e.shiftLog.id = :shiftLogId ORDER BY e.timestamp")
    List<ShiftExpense> findByShiftLogId(@Param("shiftLogId") Long shiftLogId);

    /**
     * Find expenses by type for a shift log
     */
    @Query("SELECT e FROM ShiftExpense e " +
           "WHERE e.shiftLog.id = :shiftLogId " +
           "AND e.expenseType = :expenseType " +
           "ORDER BY e.timestamp")
    List<ShiftExpense> findByShiftLogIdAndType(
        @Param("shiftLogId") Long shiftLogId,
        @Param("expenseType") ShiftExpense.ExpenseType expenseType
    );

    /**
     * Find expenses by who paid
     */
    @Query("SELECT e FROM ShiftExpense e " +
           "WHERE e.shiftLog.id = :shiftLogId " +
           "AND e.paidBy = :paidBy " +
           "ORDER BY e.timestamp")
    List<ShiftExpense> findByShiftLogIdAndPaidBy(
        @Param("shiftLogId") Long shiftLogId,
        @Param("paidBy") ShiftExpense.PaidBy paidBy
    );

    /**
     * Find all expenses for an owner in a date range
     */
    @Query("SELECT e FROM ShiftExpense e " +
           "JOIN e.shiftLog sl " +
           "WHERE sl.owner.id = :ownerId " +
           "AND sl.logDate BETWEEN :startDate AND :endDate " +
           "ORDER BY sl.logDate, e.timestamp")
    List<ShiftExpense> findByOwnerIdAndDateRange(
        @Param("ownerId") Long ownerId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Get total expenses for an owner in a date range
     */
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM ShiftExpense e " +
           "JOIN e.shiftLog sl " +
           "WHERE sl.owner.id = :ownerId " +
           "AND sl.logDate BETWEEN :startDate AND :endDate")
    Double getTotalExpensesByOwner(
        @Param("ownerId") Long ownerId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Get total expenses by type for an owner
     */
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM ShiftExpense e " +
           "JOIN e.shiftLog sl " +
           "WHERE sl.owner.id = :ownerId " +
           "AND e.expenseType = :expenseType " +
           "AND sl.logDate BETWEEN :startDate AND :endDate")
    Double getTotalExpensesByOwnerAndType(
        @Param("ownerId") Long ownerId,
        @Param("expenseType") ShiftExpense.ExpenseType expenseType,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find company expenses (dispatch, internet charges)
     */
    @Query("SELECT e FROM ShiftExpense e " +
           "WHERE e.shiftLog.id = :shiftLogId " +
           "AND e.expenseType IN ('DISPATCH_FEE', 'INTERNET_CHARGE') " +
           "ORDER BY e.timestamp")
    List<ShiftExpense> findCompanyExpenses(@Param("shiftLogId") Long shiftLogId);

    /**
     * Find maintenance expenses
     */
    @Query("SELECT e FROM ShiftExpense e " +
           "WHERE e.shiftLog.id = :shiftLogId " +
           "AND e.expenseType IN ('MAINTENANCE', 'TIRE_REPLACEMENT', 'OIL_CHANGE', 'INSPECTION') " +
           "ORDER BY e.timestamp")
    List<ShiftExpense> findMaintenanceExpenses(@Param("shiftLogId") Long shiftLogId);

    /**
     * Find expenses without receipts
     */
    @Query("SELECT e FROM ShiftExpense e " +
           "WHERE e.shiftLog.id = :shiftLogId " +
           "AND (e.receiptUrl IS NULL OR e.receiptUrl = '') " +
           "ORDER BY e.timestamp")
    List<ShiftExpense> findExpensesWithoutReceipts(@Param("shiftLogId") Long shiftLogId);

    /**
     * Find expenses by responsible party
     */
    @Query("SELECT e FROM ShiftExpense e " +
           "WHERE e.shiftLog.id = :shiftLogId " +
           "AND e.responsibleParty = :responsibleParty " +
           "ORDER BY e.timestamp")
    List<ShiftExpense> findByResponsibleParty(
        @Param("shiftLogId") Long shiftLogId,
        @Param("responsibleParty") ShiftExpense.ResponsibleParty responsibleParty
    );

    /**
     * Find expenses within a timestamp range
     */
    @Query("SELECT e FROM ShiftExpense e " +
           "WHERE e.timestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY e.timestamp")
    List<ShiftExpense> findByTimestampRange(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * Get total company expenses for a date range
     */
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM ShiftExpense e " +
           "JOIN e.shiftLog sl " +
           "WHERE e.expenseType IN ('DISPATCH_FEE', 'INTERNET_CHARGE') " +
           "AND sl.logDate BETWEEN :startDate AND :endDate")
    Double getTotalCompanyExpenses(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Count expenses for a shift log
     */
    long countByShiftLog(ShiftLog shiftLog);
}
