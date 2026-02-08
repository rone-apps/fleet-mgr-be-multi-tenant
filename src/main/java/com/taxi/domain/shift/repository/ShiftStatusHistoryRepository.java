package com.taxi.domain.shift.repository;

import com.taxi.domain.shift.model.ShiftStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for ShiftStatusHistory entity
 *
 * Handles queries related to shift status history for auditing and historical reporting
 */
@Repository
public interface ShiftStatusHistoryRepository extends JpaRepository<ShiftStatusHistory, Long> {

    /**
     * Get all status history records for a shift, ordered by most recent first
     *
     * @param shiftId The shift ID
     * @return List of status history records, newest first
     */
    List<ShiftStatusHistory> findByShiftIdOrderByEffectiveFromDesc(Long shiftId);

    /**
     * Get the current status of a shift (where effective_to is NULL)
     *
     * Only one record per shift should have effective_to = NULL
     *
     * @param shiftId The shift ID
     * @return Optional containing the current status record if exists
     */
    @Query("SELECT h FROM ShiftStatusHistory h " +
           "WHERE h.shift.id = :shiftId " +
           "AND h.effectiveTo IS NULL")
    Optional<ShiftStatusHistory> findCurrentStatus(@Param("shiftId") Long shiftId);

    /**
     * Check if a shift was active on a specific date
     *
     * A shift is considered active on a date if:
     * 1. is_active = true
     * 2. date >= effective_from
     * 3. date <= effective_to OR effective_to IS NULL
     *
     * @param shiftId The shift ID
     * @param date The date to check
     * @return Optional containing the active status record if shift was active
     */
    @Query("SELECT h FROM ShiftStatusHistory h " +
           "WHERE h.shift.id = :shiftId " +
           "AND h.isActive = true " +
           "AND :date >= h.effectiveFrom " +
           "AND (:date <= h.effectiveTo OR h.effectiveTo IS NULL)")
    Optional<ShiftStatusHistory> findActiveStatusOn(@Param("shiftId") Long shiftId, @Param("date") LocalDate date);

    /**
     * Get all status history records for a shift within a date range
     *
     * @param shiftId The shift ID
     * @param fromDate Start date (inclusive)
     * @param toDate End date (inclusive)
     * @return List of status history records that overlap with the date range
     */
    @Query("SELECT h FROM ShiftStatusHistory h " +
           "WHERE h.shift.id = :shiftId " +
           "AND NOT (h.effectiveTo < :fromDate OR h.effectiveFrom > :toDate) " +
           "ORDER BY h.effectiveFrom DESC")
    List<ShiftStatusHistory> findStatusHistoryInDateRange(
            @Param("shiftId") Long shiftId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /**
     * Delete all status history records for a shift
     *
     * Used during testing and maintenance
     *
     * @param shiftId The shift ID
     */
    void deleteByShiftId(Long shiftId);
}
