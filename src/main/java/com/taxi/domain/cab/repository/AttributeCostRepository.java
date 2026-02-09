package com.taxi.domain.cab.repository;

import com.taxi.domain.cab.model.AttributeCost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttributeCostRepository extends JpaRepository<AttributeCost, Long> {

    /**
     * Find all costs for a specific attribute
     */
    List<AttributeCost> findByAttributeTypeIdOrderByEffectiveFromDesc(Long attributeTypeId);

    /**
     * Find cost for an attribute on a specific date
     * Returns the cost that was active on that date
     */
    @Query("SELECT ac FROM AttributeCost ac " +
           "WHERE ac.attributeType.id = :attributeTypeId " +
           "AND ac.effectiveFrom <= :date " +
           "AND (ac.effectiveTo IS NULL OR ac.effectiveTo >= :date) " +
           "ORDER BY ac.effectiveFrom DESC " +
           "LIMIT 1")
    Optional<AttributeCost> findActiveOn(Long attributeTypeId, LocalDate date);

    /**
     * Find all currently active costs (no end date or end date in future)
     */
    @Query("SELECT ac FROM AttributeCost ac " +
           "WHERE ac.effectiveFrom <= CURDATE() " +
           "AND (ac.effectiveTo IS NULL OR ac.effectiveTo >= CURDATE()) " +
           "ORDER BY ac.effectiveFrom DESC")
    List<AttributeCost> findAllCurrentlActive();

    /**
     * Find all costs for a set of attribute IDs on a specific date
     */
    @Query("SELECT ac FROM AttributeCost ac " +
           "WHERE ac.attributeType.id IN :attributeTypeIds " +
           "AND ac.effectiveFrom <= :date " +
           "AND (ac.effectiveTo IS NULL OR ac.effectiveTo >= :date) " +
           "ORDER BY ac.effectiveFrom DESC")
    List<AttributeCost> findActiveCostsOnDate(List<Long> attributeTypeIds, LocalDate date);

    /**
     * Find all costs by attribute type ID ordered by date
     */
    List<AttributeCost> findByAttributeTypeIdOrderByEffectiveFromAsc(Long attributeTypeId);
}
