package com.taxi.domain.lease.repository;

import com.taxi.domain.lease.model.LeaseRateOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaseRateOverrideRepository extends JpaRepository<LeaseRateOverride, Long> {

    /**
     * Find all active overrides for a specific owner
     */
    List<LeaseRateOverride> findByOwnerDriverNumberAndIsActiveTrue(String ownerDriverNumber);

    /**
     * Find all overrides for a specific owner (active and inactive)
     */
    List<LeaseRateOverride> findByOwnerDriverNumberOrderByPriorityDescCreatedAtDesc(String ownerDriverNumber);

    /**
     * Find all overrides (all owners), ordered by owner, then priority
     */
    List<LeaseRateOverride> findAllByOrderByOwnerDriverNumberAscPriorityDescCreatedAtDesc();

    /**
     * Find active overrides for a specific cab and owner
     */
    List<LeaseRateOverride> findByCabNumberAndOwnerDriverNumberAndIsActiveTrue(
        String cabNumber, String ownerDriverNumber);

    /**
     * Find the best matching override for given criteria
     * Returns overrides ordered by priority (highest first)
     * 
     * This query finds overrides that match:
     * 1. The owner
     * 2. The cab (or null for all cabs)
     * 3. The shift type (or null for all shifts)
     * 4. The day of week (or null for all days)
     * 5. Active on the given date
     * 6. Is active flag = true
     */
    @Query("""
        SELECT lro FROM LeaseRateOverride lro
        WHERE lro.ownerDriverNumber = :ownerDriverNumber
        AND lro.isActive = true
        AND lro.startDate <= :date
        AND (lro.endDate IS NULL OR lro.endDate >= :date)
        AND (lro.cabNumber IS NULL OR lro.cabNumber = :cabNumber)
        AND (lro.shiftType IS NULL OR lro.shiftType = :shiftType)
        AND (lro.dayOfWeek IS NULL OR lro.dayOfWeek = :dayOfWeek)
        ORDER BY lro.priority DESC, lro.id DESC
        """)
    List<LeaseRateOverride> findMatchingOverrides(
        @Param("ownerDriverNumber") String ownerDriverNumber,
        @Param("cabNumber") String cabNumber,
        @Param("shiftType") String shiftType,
        @Param("dayOfWeek") String dayOfWeek,
        @Param("date") LocalDate date
    );

    /**
     * Find all overrides that are currently active (within date range)
     */
    @Query("""
        SELECT lro FROM LeaseRateOverride lro
        WHERE lro.isActive = true
        AND lro.startDate <= :date
        AND (lro.endDate IS NULL OR lro.endDate >= :date)
        ORDER BY lro.ownerDriverNumber, lro.priority DESC
        """)
    List<LeaseRateOverride> findAllActiveOverrides(@Param("date") LocalDate date);

    /**
     * Find overrides expiring soon (within next N days)
     */
    @Query("""
        SELECT lro FROM LeaseRateOverride lro
        WHERE lro.isActive = true
        AND lro.endDate IS NOT NULL
        AND lro.endDate BETWEEN :startDate AND :endDate
        ORDER BY lro.endDate, lro.ownerDriverNumber
        """)
    List<LeaseRateOverride> findExpiringSoon(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Check if an override exists for specific criteria
     */
    @Query("""
        SELECT COUNT(lro) > 0 FROM LeaseRateOverride lro
        WHERE lro.ownerDriverNumber = :ownerDriverNumber
        AND lro.isActive = true
        AND (lro.cabNumber = :cabNumber OR (lro.cabNumber IS NULL AND :cabNumber IS NULL))
        AND (lro.shiftType = :shiftType OR (lro.shiftType IS NULL AND :shiftType IS NULL))
        AND (lro.dayOfWeek = :dayOfWeek OR (lro.dayOfWeek IS NULL AND :dayOfWeek IS NULL))
        AND lro.startDate <= :endDate
        AND (lro.endDate IS NULL OR lro.endDate >= :startDate)
        """)
    boolean existsOverlappingOverride(
        @Param("ownerDriverNumber") String ownerDriverNumber,
        @Param("cabNumber") String cabNumber,
        @Param("shiftType") String shiftType,
        @Param("dayOfWeek") String dayOfWeek,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}