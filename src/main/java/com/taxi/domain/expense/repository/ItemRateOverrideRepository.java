package com.taxi.domain.expense.repository;

import com.taxi.domain.expense.model.ItemRateOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ItemRateOverrideRepository extends JpaRepository<ItemRateOverride, Long> {

    /**
     * Find all active overrides for a specific owner
     */
    List<ItemRateOverride> findByOwnerDriverNumberAndIsActiveTrueOrderByPriorityDesc(String ownerDriverNumber);

    /**
     * Find all overrides (active and inactive) for a specific owner
     */
    List<ItemRateOverride> findByOwnerDriverNumberOrderByPriorityDescStartDateDesc(String ownerDriverNumber);

    /**
     * Find overrides for a specific item rate and owner that are active on a date
     */
    @Query("SELECT o FROM ItemRateOverride o " +
           "WHERE o.itemRate.id = :itemRateId " +
           "AND o.ownerDriverNumber = :ownerDriverNumber " +
           "AND o.isActive = true " +
           "AND o.startDate <= :date " +
           "AND (o.endDate IS NULL OR o.endDate >= :date) " +
           "ORDER BY o.priority DESC")
    List<ItemRateOverride> findActiveOverridesForRate(
            @Param("itemRateId") Long itemRateId,
            @Param("ownerDriverNumber") String ownerDriverNumber,
            @Param("date") LocalDate date);

    /**
     * Count active overrides for an owner
     */
    long countByOwnerDriverNumberAndIsActiveTrue(String ownerDriverNumber);

    /**
     * Count active overrides for a specific item rate
     */
    long countByItemRateIdAndIsActiveTrue(Long itemRateId);

    /**
     * Find overrides for a specific item rate, owner, and date range
     */
    @Query("SELECT o FROM ItemRateOverride o " +
           "WHERE o.itemRate.id = :itemRateId " +
           "AND o.ownerDriverNumber = :ownerDriverNumber " +
           "AND o.isActive = true " +
           "AND o.startDate <= :endDate " +
           "AND (o.endDate IS NULL OR o.endDate >= :startDate) " +
           "ORDER BY o.priority DESC")
    List<ItemRateOverride> findByItemRateIdAndOwnerDriverNumberAndIsActiveTrueAndDateRange(
            @Param("itemRateId") Long itemRateId,
            @Param("ownerDriverNumber") String ownerDriverNumber,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
