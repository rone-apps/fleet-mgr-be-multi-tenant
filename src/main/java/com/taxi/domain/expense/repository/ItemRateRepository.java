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
     * Find active rate by name
     */
    Optional<ItemRate> findByNameAndIsActiveTrue(String name);

    /**
     * Find active rate by name (case-insensitive)
     */
    @Query("SELECT r FROM ItemRate r WHERE UPPER(r.name) = UPPER(:name) AND r.isActive = true")
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
     * Find active rates by unit type and attribute type on a given date
     */
    @Query("SELECT r FROM ItemRate r " +
           "WHERE r.isActive = true " +
           "AND r.unitType = :unitType " +
           "AND r.attributeType.id = :attributeTypeId " +
           "AND r.effectiveFrom <= :date " +
           "AND (r.effectiveTo IS NULL OR r.effectiveTo >= :date) " +
           "ORDER BY r.effectiveFrom DESC")
    List<ItemRate> findActiveByUnitTypeAndAttributeType(
        @Param("unitType") ItemRateUnitType unitType,
        @Param("attributeTypeId") Long attributeTypeId,
        @Param("date") LocalDate date);

    /**
     * Find active rates by unit type (no attribute) on a given date
     */
    @Query("SELECT r FROM ItemRate r " +
           "WHERE r.isActive = true " +
           "AND r.unitType = :unitType " +
           "AND r.attributeType IS NULL " +
           "AND r.effectiveFrom <= :date " +
           "AND (r.effectiveTo IS NULL OR r.effectiveTo >= :date) " +
           "ORDER BY r.effectiveFrom DESC")
    List<ItemRate> findActiveByUnitTypeNoAttribute(
        @Param("unitType") ItemRateUnitType unitType,
        @Param("date") LocalDate date);

    /**
     * Count active rates
     */
    long countByIsActiveTrue();
}
