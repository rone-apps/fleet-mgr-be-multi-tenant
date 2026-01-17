package com.taxi.domain.shift.repository;

import com.taxi.domain.shift.model.CabShift;
import com.taxi.domain.shift.model.ShiftLog;
import com.taxi.domain.cab.model.Cab;
import com.taxi.domain.driver.model.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for ShiftLog entity
 */
@Repository
public interface ShiftLogRepository extends JpaRepository<ShiftLog, Long> {

    /**
     * Find shift log for a specific cab, shift, and date
     */
    Optional<ShiftLog> findByCabAndShiftAndLogDate(Cab cab, CabShift shift, LocalDate logDate);

    /**
     * Find all shift logs for a cab within a date range
     */
    @Query("SELECT sl FROM ShiftLog sl " +
           "WHERE sl.cab.id = :cabId " +
           "AND sl.logDate BETWEEN :startDate AND :endDate " +
           "ORDER BY sl.logDate DESC")
    List<ShiftLog> findByCabIdAndDateRange(
        @Param("cabId") Long cabId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find all shift logs for a shift within a date range
     */
    @Query("SELECT sl FROM ShiftLog sl " +
           "WHERE sl.shift.id = :shiftId " +
           "AND sl.logDate BETWEEN :startDate AND :endDate " +
           "ORDER BY sl.logDate DESC")
    List<ShiftLog> findByShiftIdAndDateRange(
        @Param("shiftId") Long shiftId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find all shift logs owned by a driver within a date range
     */
    @Query("SELECT sl FROM ShiftLog sl " +
           "WHERE sl.owner.id = :ownerId " +
           "AND sl.logDate BETWEEN :startDate AND :endDate " +
           "ORDER BY sl.logDate DESC")
    List<ShiftLog> findByOwnerIdAndDateRange(
        @Param("ownerId") Long ownerId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find all shift logs where a driver operated (via segments)
     */
    @Query("SELECT DISTINCT sl FROM ShiftLog sl " +
           "JOIN sl.segments seg " +
           "WHERE seg.driver.id = :driverId " +
           "AND sl.logDate BETWEEN :startDate AND :endDate " +
           "ORDER BY sl.logDate DESC")
    List<ShiftLog> findByDriverIdAndDateRange(
        @Param("driverId") Long driverId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find all unsettled shift logs
     */
    @Query("SELECT sl FROM ShiftLog sl WHERE sl.settlementStatus = 'PENDING' ORDER BY sl.logDate")
    List<ShiftLog> findUnsettled();

    /**
     * Find all disputed shift logs
     */
    @Query("SELECT sl FROM ShiftLog sl WHERE sl.settlementStatus = 'DISPUTED' ORDER BY sl.logDate")
    List<ShiftLog> findDisputed();

    /**
     * Find shift logs for a specific date
     */
    List<ShiftLog> findByLogDateOrderByCreatedAtDesc(LocalDate logDate);

    /**
     * Count shift logs for an owner in a date range
     */
    @Query("SELECT COUNT(sl) FROM ShiftLog sl " +
           "WHERE sl.owner.id = :ownerId " +
           "AND sl.logDate BETWEEN :startDate AND :endDate")
    long countByOwnerIdAndDateRange(
        @Param("ownerId") Long ownerId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Get total revenue for an owner in a date range
     */
    @Query("SELECT COALESCE(SUM(sl.financialSummary.ownerEarnings), 0) FROM ShiftLog sl " +
           "WHERE sl.owner.id = :ownerId " +
           "AND sl.logDate BETWEEN :startDate AND :endDate " +
           "AND sl.settlementStatus = 'SETTLED'")
    Double getTotalOwnerEarnings(
        @Param("ownerId") Long ownerId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Get total earnings for a driver in a date range
     */
    @Query("SELECT COALESCE(SUM(seg.segmentNetEarnings), 0) FROM DriverSegment seg " +
           "JOIN seg.shiftLog sl " +
           "WHERE seg.driver.id = :driverId " +
           "AND sl.logDate BETWEEN :startDate AND :endDate " +
           "AND sl.settlementStatus = 'SETTLED'")
    Double getTotalDriverEarnings(
        @Param("driverId") Long driverId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}
