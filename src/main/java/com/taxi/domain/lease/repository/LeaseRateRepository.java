package com.taxi.domain.lease.repository;

import com.taxi.domain.cab.model.CabType;
import com.taxi.domain.lease.model.LeasePlan;
import com.taxi.domain.lease.model.LeaseRate;
import com.taxi.domain.shift.model.ShiftType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

/**
 * Repository for LeaseRate entity
 */
@Repository
public interface LeaseRateRepository extends JpaRepository<LeaseRate, Long> {

    /**
     * Find all rates for a specific lease plan
     */
    List<LeaseRate> findByLeasePlan(LeasePlan leasePlan);

    /**
     * Find all rates for a lease plan by plan ID
     */
    @Query("SELECT lr FROM LeaseRate lr WHERE lr.leasePlan.id = :planId")
    List<LeaseRate> findByLeasePlanId(@Param("planId") Long planId);

    /**
     * Find specific lease rate by all criteria
     */
    Optional<LeaseRate> findByLeasePlanAndCabTypeAndHasAirportLicenseAndShiftTypeAndDayOfWeek(
        LeasePlan leasePlan,
        CabType cabType,
        boolean hasAirportLicense,
        ShiftType shiftType,
        DayOfWeek dayOfWeek
    );

    /**
     * Find lease rate by plan ID and all criteria
     */
    @Query("SELECT lr FROM LeaseRate lr " +
           "WHERE lr.leasePlan.id = :planId " +
           "AND lr.cabType = :cabType " +
           "AND lr.hasAirportLicense = :hasAirportLicense " +
           "AND lr.shiftType = :shiftType " +
           "AND lr.dayOfWeek = :dayOfWeek")
    Optional<LeaseRate> findRateByCriteria(
        @Param("planId") Long planId,
        @Param("cabType") CabType cabType,
        @Param("hasAirportLicense") boolean hasAirportLicense,
        @Param("shiftType") ShiftType shiftType,
        @Param("dayOfWeek") DayOfWeek dayOfWeek
    );

    /**
     * Find all rates for a specific cab type
     */
    @Query("SELECT lr FROM LeaseRate lr WHERE lr.leasePlan.id = :planId AND lr.cabType = :cabType")
    List<LeaseRate> findByCabType(
        @Param("planId") Long planId,
        @Param("cabType") CabType cabType
    );

    /**
     * Find all rates for a specific shift type
     */
    @Query("SELECT lr FROM LeaseRate lr WHERE lr.leasePlan.id = :planId AND lr.shiftType = :shiftType")
    List<LeaseRate> findByShiftType(
        @Param("planId") Long planId,
        @Param("shiftType") ShiftType shiftType
    );

    /**
     * Find all rates for a specific day of week
     */
    @Query("SELECT lr FROM LeaseRate lr WHERE lr.leasePlan.id = :planId AND lr.dayOfWeek = :dayOfWeek")
    List<LeaseRate> findByDayOfWeek(
        @Param("planId") Long planId,
        @Param("dayOfWeek") DayOfWeek dayOfWeek
    );

    /**
     * Find rates with airport license requirement
     */
    @Query("SELECT lr FROM LeaseRate lr WHERE lr.leasePlan.id = :planId AND lr.hasAirportLicense = true")
    List<LeaseRate> findWithAirportLicense(@Param("planId") Long planId);

    /**
     * Check if rate exists for given criteria
     */
    @Query("SELECT COUNT(lr) > 0 FROM LeaseRate lr " +
           "WHERE lr.leasePlan.id = :planId " +
           "AND lr.cabType = :cabType " +
           "AND lr.hasAirportLicense = :hasAirportLicense " +
           "AND lr.shiftType = :shiftType " +
           "AND lr.dayOfWeek = :dayOfWeek")
    boolean existsRateForCriteria(
        @Param("planId") Long planId,
        @Param("cabType") CabType cabType,
        @Param("hasAirportLicense") boolean hasAirportLicense,
        @Param("shiftType") ShiftType shiftType,
        @Param("dayOfWeek") DayOfWeek dayOfWeek
    );

    /**
     * Count rates for a plan
     */
    long countByLeasePlan(LeasePlan leasePlan);
}
