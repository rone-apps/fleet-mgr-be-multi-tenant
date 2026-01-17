package com.taxi.domain.lease.repository;

import com.taxi.domain.lease.model.LeasePlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for LeasePlan entity
 */
@Repository
public interface LeasePlanRepository extends JpaRepository<LeasePlan, Long> {

    /**
     * Find the currently active lease plan
     */
    @Query("SELECT lp FROM LeasePlan lp " +
           "WHERE lp.isActive = true " +
           "AND lp.effectiveFrom <= CURRENT_DATE " +
           "AND (lp.effectiveTo IS NULL OR lp.effectiveTo >= CURRENT_DATE) " +
           "ORDER BY lp.effectiveFrom DESC")
    Optional<LeasePlan> findActivePlan();

    /**
     * Find lease plan active on a specific date (for historical reports)
     */
    @Query("SELECT lp FROM LeasePlan lp " +
           "WHERE lp.effectiveFrom <= :date " +
           "AND (lp.effectiveTo IS NULL OR lp.effectiveTo >= :date) " +
           "ORDER BY lp.effectiveFrom DESC")
    Optional<LeasePlan> findPlanActiveOnDate(@Param("date") LocalDate date);

    /**
     * Find all active plans
     */
    List<LeasePlan> findByIsActiveTrueOrderByEffectiveFromDesc();

    /**
     * Find all plans within a date range
     */
    @Query("SELECT lp FROM LeasePlan lp " +
           "WHERE lp.effectiveFrom <= :endDate " +
           "AND (lp.effectiveTo IS NULL OR lp.effectiveTo >= :startDate) " +
           "ORDER BY lp.effectiveFrom DESC")
    List<LeasePlan> findPlansInRange(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find plans by name
     */
    List<LeasePlan> findByPlanNameContainingIgnoreCaseOrderByEffectiveFromDesc(String planName);

    /**
     * Check if there's an active plan
     */
    @Query("SELECT COUNT(lp) > 0 FROM LeasePlan lp " +
           "WHERE lp.isActive = true " +
           "AND lp.effectiveFrom <= CURRENT_DATE " +
           "AND (lp.effectiveTo IS NULL OR lp.effectiveTo >= CURRENT_DATE)")
    boolean hasActivePlan();

    /**
     * Find all plans with lease rates eagerly fetched (avoids lazy loading issues)
     */
    @Query("SELECT DISTINCT lp FROM LeasePlan lp LEFT JOIN FETCH lp.leaseRates ORDER BY lp.effectiveFrom DESC")
    List<LeasePlan> findAllWithRates();

    /**
     * Find plan by ID with lease rates eagerly fetched
     */
    @Query("SELECT lp FROM LeasePlan lp LEFT JOIN FETCH lp.leaseRates WHERE lp.id = :id")
    Optional<LeasePlan> findByIdWithRates(@Param("id") Long id);
}
