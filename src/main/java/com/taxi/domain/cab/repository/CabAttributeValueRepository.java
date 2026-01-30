package com.taxi.domain.cab.repository;

import com.taxi.domain.cab.model.Cab;
import com.taxi.domain.cab.model.CabAttributeType;
import com.taxi.domain.cab.model.CabAttributeValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for CabAttributeValue
 * Implements complex temporal queries for date range filtering and overlap detection
 */
@Repository
public interface CabAttributeValueRepository extends JpaRepository<CabAttributeValue, Long> {

    /**
     * Get all attribute values for a cab (including history)
     */
    @Query("SELECT v FROM CabAttributeValue v " +
           "JOIN FETCH v.cab " +
           "JOIN FETCH v.attributeType " +
           "WHERE v.cab = :cab " +
           "ORDER BY v.startDate DESC")
    List<CabAttributeValue> findByCabOrderByStartDateDesc(@Param("cab") Cab cab);

    /**
     * Get current attributes for a cab (where endDate is null)
     * Following CabOwnerHistory pattern
     */
    @Query("SELECT v FROM CabAttributeValue v " +
           "JOIN FETCH v.cab " +
           "JOIN FETCH v.attributeType " +
           "WHERE v.cab = :cab AND v.endDate IS NULL")
    List<CabAttributeValue> findCurrentAttributesByCab(@Param("cab") Cab cab);

    /**
     * Get current attributes by cab ID (more efficient)
     */
    @Query("SELECT v FROM CabAttributeValue v " +
           "JOIN FETCH v.cab " +
           "JOIN FETCH v.attributeType " +
           "WHERE v.cab.id = :cabId AND v.endDate IS NULL")
    List<CabAttributeValue> findCurrentAttributesByCabId(@Param("cabId") Long cabId);

    /**
     * Get attributes active on a specific date
     * Following LeaseRateOverride.isActiveOn() logic
     */
    @Query("SELECT v FROM CabAttributeValue v " +
           "JOIN FETCH v.cab " +
           "JOIN FETCH v.attributeType " +
           "WHERE v.cab = :cab " +
           "AND v.startDate <= :date " +
           "AND (v.endDate IS NULL OR v.endDate >= :date)")
    List<CabAttributeValue> findAttributesActiveOnDate(
        @Param("cab") Cab cab,
        @Param("date") LocalDate date);

    /**
     * Check if cab has a specific attribute currently
     */
    @Query("SELECT v FROM CabAttributeValue v " +
           "WHERE v.cab = :cab " +
           "AND v.attributeType = :attributeType " +
           "AND v.endDate IS NULL")
    Optional<CabAttributeValue> findCurrentAttributeByCabAndType(
        @Param("cab") Cab cab,
        @Param("attributeType") CabAttributeType attributeType);

    /**
     * Check if cab has a specific attribute on a date
     */
    @Query("SELECT v FROM CabAttributeValue v " +
           "WHERE v.cab = :cab " +
           "AND v.attributeType = :attributeType " +
           "AND v.startDate <= :date " +
           "AND (v.endDate IS NULL OR v.endDate >= :date)")
    Optional<CabAttributeValue> findAttributeOnDate(
        @Param("cab") Cab cab,
        @Param("attributeType") CabAttributeType attributeType,
        @Param("date") LocalDate date);

    /**
     * Get attribute history for a specific type
     */
    @Query("SELECT v FROM CabAttributeValue v " +
           "JOIN FETCH v.cab " +
           "JOIN FETCH v.attributeType " +
           "WHERE v.cab = :cab " +
           "AND v.attributeType = :attributeType " +
           "ORDER BY v.startDate DESC")
    List<CabAttributeValue> findAttributeHistoryByCabAndType(
        @Param("cab") Cab cab,
        @Param("attributeType") CabAttributeType attributeType);

    /**
     * Find all cabs with a specific attribute currently
     */
    @Query("SELECT v FROM CabAttributeValue v " +
           "JOIN FETCH v.cab " +
           "JOIN FETCH v.attributeType " +
           "WHERE v.attributeType = :attributeType " +
           "AND v.endDate IS NULL")
    List<CabAttributeValue> findCabsWithCurrentAttribute(
        @Param("attributeType") CabAttributeType attributeType);

    /**
     * Find all cabs with a specific attribute on a date
     */
    @Query("SELECT v FROM CabAttributeValue v " +
           "JOIN FETCH v.cab " +
           "JOIN FETCH v.attributeType " +
           "WHERE v.attributeType = :attributeType " +
           "AND v.startDate <= :date " +
           "AND (v.endDate IS NULL OR v.endDate >= :date)")
    List<CabAttributeValue> findCabsWithAttributeOnDate(
        @Param("attributeType") CabAttributeType attributeType,
        @Param("date") LocalDate date);

    /**
     * Check for overlapping attribute assignments
     * Used for validation to prevent duplicate active attributes of the same type
     * Excludes the record with the given ID (useful for update operations)
     */
    @Query("SELECT v FROM CabAttributeValue v " +
           "JOIN FETCH v.cab " +
           "JOIN FETCH v.attributeType " +
           "WHERE v.cab = :cab " +
           "AND v.attributeType = :attributeType " +
           "AND v.id != :excludeId " +
           "AND v.startDate <= :endDate " +
           "AND (v.endDate IS NULL OR v.endDate >= :startDate)")
    List<CabAttributeValue> findOverlappingAttributes(
        @Param("cab") Cab cab,
        @Param("attributeType") CabAttributeType attributeType,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("excludeId") Long excludeId);
}
