package com.taxi.domain.profile.repository;

import com.taxi.domain.profile.model.ShiftProfileAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for ShiftProfileAssignment entity
 * Manages the audit trail of profile assignments to shifts
 */
@Repository
public interface ShiftProfileAssignmentRepository extends JpaRepository<ShiftProfileAssignment, Long> {

    /**
     * Get all assignments for a shift (including historical)
     */
    List<ShiftProfileAssignment> findByShiftIdOrderByStartDateDesc(Long shiftId);

    /**
     * Get the current active assignment for a shift (where end_date IS NULL)
     */
    @Query("SELECT a FROM ShiftProfileAssignment a " +
           "WHERE a.shift.id = :shiftId AND a.endDate IS NULL")
    Optional<ShiftProfileAssignment> findActiveAssignmentByShiftId(@Param("shiftId") Long shiftId);

    /**
     * Get all assignments for a profile
     */
    List<ShiftProfileAssignment> findByProfileIdOrderByStartDateDesc(Long profileId);

    /**
     * Get all current active assignments for a profile
     */
    @Query("SELECT a FROM ShiftProfileAssignment a " +
           "WHERE a.profile.id = :profileId AND a.endDate IS NULL")
    List<ShiftProfileAssignment> findActiveAssignmentsByProfileId(@Param("profileId") Long profileId);

    /**
     * Get the assignment that was active on a specific date
     */
    @Query("SELECT a FROM ShiftProfileAssignment a " +
           "WHERE a.shift.id = :shiftId " +
           "AND a.startDate <= :date " +
           "AND (a.endDate IS NULL OR a.endDate >= :date)")
    Optional<ShiftProfileAssignment> findAssignmentActiveOnDate(
            @Param("shiftId") Long shiftId,
            @Param("date") LocalDate date);

    /**
     * Check if a shift has any profile assignment
     */
    @Query("SELECT COUNT(a) > 0 FROM ShiftProfileAssignment a " +
           "WHERE a.shift.id = :shiftId")
    boolean hasAnyAssignment(@Param("shiftId") Long shiftId);

    /**
     * Count total shifts currently assigned to a profile
     */
    @Query("SELECT COUNT(DISTINCT a.shift.id) FROM ShiftProfileAssignment a " +
           "WHERE a.profile.id = :profileId AND a.endDate IS NULL")
    long countActiveShiftsByProfileId(@Param("profileId") Long profileId);

    /**
     * Get all shifts currently assigned to a profile
     */
    @Query("SELECT DISTINCT a.shift FROM ShiftProfileAssignment a " +
           "WHERE a.profile.id = :profileId AND a.endDate IS NULL")
    List<Object> findActiveShiftsByProfileId(@Param("profileId") Long profileId);

    /**
     * Get profile assignment history for a shift within a date range
     */
    @Query("SELECT a FROM ShiftProfileAssignment a " +
           "WHERE a.shift.id = :shiftId " +
           "AND a.startDate <= :endDate " +
           "AND (a.endDate IS NULL OR a.endDate >= :startDate) " +
           "ORDER BY a.startDate DESC")
    List<ShiftProfileAssignment> findAssignmentsInDateRange(
            @Param("shiftId") Long shiftId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
