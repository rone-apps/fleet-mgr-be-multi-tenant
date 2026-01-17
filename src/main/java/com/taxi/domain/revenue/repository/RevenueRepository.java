package com.taxi.domain.revenue.repository;

import com.taxi.domain.revenue.model.Revenue;
import com.taxi.domain.shift.model.ShiftLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Revenue entity
 */
@Repository
public interface RevenueRepository extends JpaRepository<Revenue, Long> {

    /**
     * Find all revenues for a shift log
     */
    List<Revenue> findByShiftLog(ShiftLog shiftLog);

    /**
     * Find all revenues for a shift log by ID
     */
    @Query("SELECT r FROM Revenue r WHERE r.shiftLog.id = :shiftLogId ORDER BY r.timestamp")
    List<Revenue> findByShiftLogId(@Param("shiftLogId") Long shiftLogId);

    /**
     * Find revenues by type for a shift log
     */
    @Query("SELECT r FROM Revenue r " +
           "WHERE r.shiftLog.id = :shiftLogId " +
           "AND r.revenueType = :revenueType " +
           "ORDER BY r.timestamp")
    List<Revenue> findByShiftLogIdAndType(
        @Param("shiftLogId") Long shiftLogId,
        @Param("revenueType") Revenue.RevenueType revenueType
    );

    /**
     * Find revenues by payment method
     */
    @Query("SELECT r FROM Revenue r " +
           "WHERE r.shiftLog.id = :shiftLogId " +
           "AND r.paymentMethod = :paymentMethod " +
           "ORDER BY r.timestamp")
    List<Revenue> findByShiftLogIdAndPaymentMethod(
        @Param("shiftLogId") Long shiftLogId,
        @Param("paymentMethod") Revenue.PaymentMethod paymentMethod
    );

    /**
     * Find all revenues for an owner in a date range
     */
    @Query("SELECT r FROM Revenue r " +
           "JOIN r.shiftLog sl " +
           "WHERE sl.owner.id = :ownerId " +
           "AND sl.logDate BETWEEN :startDate AND :endDate " +
           "ORDER BY sl.logDate, r.timestamp")
    List<Revenue> findByOwnerIdAndDateRange(
        @Param("ownerId") Long ownerId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Get total revenue for an owner in a date range
     */
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Revenue r " +
           "JOIN r.shiftLog sl " +
           "WHERE sl.owner.id = :ownerId " +
           "AND sl.logDate BETWEEN :startDate AND :endDate")
    Double getTotalRevenueByOwner(
        @Param("ownerId") Long ownerId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Get total revenue by type for an owner
     */
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Revenue r " +
           "JOIN r.shiftLog sl " +
           "WHERE sl.owner.id = :ownerId " +
           "AND r.revenueType = :revenueType " +
           "AND sl.logDate BETWEEN :startDate AND :endDate")
    Double getTotalRevenueByOwnerAndType(
        @Param("ownerId") Long ownerId,
        @Param("revenueType") Revenue.RevenueType revenueType,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find all cash revenues for a shift log
     */
    @Query("SELECT r FROM Revenue r " +
           "WHERE r.shiftLog.id = :shiftLogId " +
           "AND r.paymentMethod = 'CASH' " +
           "ORDER BY r.timestamp")
    List<Revenue> findCashRevenues(@Param("shiftLogId") Long shiftLogId);

    /**
     * Find all credit card revenues for a shift log
     */
    @Query("SELECT r FROM Revenue r " +
           "WHERE r.shiftLog.id = :shiftLogId " +
           "AND r.paymentMethod IN ('CREDIT_CARD', 'DEBIT_CARD') " +
           "ORDER BY r.timestamp")
    List<Revenue> findCardRevenues(@Param("shiftLogId") Long shiftLogId);

    /**
     * Find all charge account revenues for a shift log
     */
    @Query("SELECT r FROM Revenue r " +
           "WHERE r.shiftLog.id = :shiftLogId " +
           "AND r.paymentMethod = 'CHARGE_ACCOUNT' " +
           "ORDER BY r.timestamp")
    List<Revenue> findChargeAccountRevenues(@Param("shiftLogId") Long shiftLogId);

    /**
     * Find revenues by reference number (for tracking card transactions)
     */
    List<Revenue> findByReferenceNumber(String referenceNumber);

    /**
     * Find revenues within a timestamp range
     */
    @Query("SELECT r FROM Revenue r " +
           "WHERE r.timestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY r.timestamp")
    List<Revenue> findByTimestampRange(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * Count revenues for a shift log
     */
    long countByShiftLog(ShiftLog shiftLog);
}
