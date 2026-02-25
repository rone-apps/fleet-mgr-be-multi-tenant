package com.taxi.domain.expense.repository;

import com.taxi.domain.expense.model.ItemRate;
import com.taxi.domain.profile.model.ItemRateChargedTo;
import com.taxi.domain.profile.model.ItemRateUnitType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ItemRateRepository extends JpaRepository<ItemRate, Long> {

    /**
     * Find all active item rates
     */
    List<ItemRate> findByIsActiveTrueOrderByName();

    /**
     * Find rate by name
     */
    Optional<ItemRate> findByName(String name);

    /**
     * Find rate by name (case-insensitive)
     */
    @Query("SELECT r FROM ItemRate r WHERE UPPER(r.name) = UPPER(:name)")
    Optional<ItemRate> findByNameIgnoreCase(@Param("name") String name);

    /**
     * Find all rates active on a given date
     */
    @Query("SELECT r FROM ItemRate r " +
           "WHERE r.isActive = true " +
           "AND r.effectiveFrom <= :date " +
           "AND (r.effectiveTo IS NULL OR r.effectiveTo >= :date) " +
           "ORDER BY r.name")
    List<ItemRate> findActiveOnDate(@Param("date") LocalDate date);

    /**
     * Find all rates for a specific charged-to type
     */
    List<ItemRate> findByChargedToAndIsActiveTrueOrderByName(ItemRateChargedTo chargedTo);

    /**
     * Find all rates for a specific unit type and charged-to type
     */
    List<ItemRate> findByUnitTypeAndChargedToAndIsActiveTrueOrderByName(ItemRateUnitType unitType, ItemRateChargedTo chargedTo);

    /**
     * Count active rates
     */
    long countByIsActiveTrue();
}
