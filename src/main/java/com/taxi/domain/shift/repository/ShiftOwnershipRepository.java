package com.taxi.domain.shift.repository;

import com.taxi.domain.shift.model.CabShift;
import com.taxi.domain.shift.model.ShiftOwnership;
import com.taxi.domain.driver.model.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for ShiftOwnership entity
 */
@Repository
public interface ShiftOwnershipRepository extends JpaRepository<ShiftOwnership, Long> {

    /**
     * Find all ownership records for a shift
     */
    List<ShiftOwnership> findByShiftOrderByStartDateDesc(CabShift shift);

    /**
     * Find all ownership records for a shift by shift ID
     */
    @Query("SELECT so FROM ShiftOwnership so WHERE so.shift.id = :shiftId ORDER BY so.startDate DESC")
    List<ShiftOwnership> findByShiftIdOrderByStartDateDesc(@Param("shiftId") Long shiftId);

    /**
     * Find current ownership for a shift (no end date)
     */
    @Query("SELECT so FROM ShiftOwnership so WHERE so.shift.id = :shiftId AND so.endDate IS NULL")
    Optional<ShiftOwnership> findCurrentOwnership(@Param("shiftId") Long shiftId);

    /**
     * Find ownership on a specific date (for historical reports)
     */
    @Query("SELECT so FROM ShiftOwnership so " +
           "WHERE so.shift.id = :shiftId " +
           "AND so.startDate <= :date " +
           "AND (so.endDate IS NULL OR so.endDate >= :date)")
    Optional<ShiftOwnership> findOwnershipOnDate(
        @Param("shiftId") Long shiftId, 
        @Param("date") LocalDate date
    );

    /**
     * Find all ownerships by a driver
     */
    List<ShiftOwnership> findByOwnerOrderByStartDateDesc(Driver owner);

    /**
     * Find all ownerships by driver ID
     */
    @Query("SELECT so FROM ShiftOwnership so WHERE so.owner.id = :ownerId ORDER BY so.startDate DESC")
    List<ShiftOwnership> findByOwnerIdOrderByStartDateDesc(@Param("ownerId") Long ownerId);

    /**
     * Find all current ownerships by a driver
     */
    @Query("SELECT so FROM ShiftOwnership so WHERE so.owner.id = :ownerId AND so.endDate IS NULL")
    List<ShiftOwnership> findCurrentOwnershipsByDriver(@Param("ownerId") Long ownerId);

    /**
     * Find ownerships transferred to a specific driver
     */
    @Query("SELECT so FROM ShiftOwnership so WHERE so.transferredTo.id = :driverId ORDER BY so.endDate DESC")
    List<ShiftOwnership> findTransferredToDriver(@Param("driverId") Long driverId);

    /**
     * Find ownerships within a date range for a shift
     */
    @Query("SELECT so FROM ShiftOwnership so " +
           "WHERE so.shift.id = :shiftId " +
           "AND so.startDate <= :endDate " +
           "AND (so.endDate IS NULL OR so.endDate >= :startDate) " +
           "ORDER BY so.startDate")
    List<ShiftOwnership> findOwnershipInRange(
        @Param("shiftId") Long shiftId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find all shifts owned by a driver within a date range
     */
    @Query("SELECT so FROM ShiftOwnership so " +
           "WHERE so.owner.id = :ownerId " +
           "AND so.startDate <= :endDate " +
           "AND (so.endDate IS NULL OR so.endDate >= :startDate) " +
           "ORDER BY so.startDate DESC")
    List<ShiftOwnership> findOwnershipsInRange(
        @Param("ownerId") Long ownerId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}
