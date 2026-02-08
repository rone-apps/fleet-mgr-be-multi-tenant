package com.taxi.domain.profile.repository;

import com.taxi.domain.cab.model.CabType;
import com.taxi.domain.cab.model.ShareType;
import com.taxi.domain.profile.model.ShiftProfile;
import com.taxi.domain.shift.model.ShiftType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ShiftProfile entity
 * Provides database queries for profile lookup, filtering, and matching
 */
@Repository
public interface ShiftProfileRepository extends JpaRepository<ShiftProfile, Long> {

    /**
     * Find profile by unique code
     */
    Optional<ShiftProfile> findByProfileCode(String profileCode);

    /**
     * Get all active profiles
     */
    List<ShiftProfile> findByIsActiveTrue();

    /**
     * Get all active profiles ordered by display order
     */
    List<ShiftProfile> findByIsActiveTrueOrderByDisplayOrder();

    /**
     * Find profiles by category
     */
    List<ShiftProfile> findByCategory(String category);

    /**
     * Find profiles by cab type
     */
    List<ShiftProfile> findByCabType(CabType cabType);

    /**
     * Find profiles by share type
     */
    List<ShiftProfile> findByShareType(ShareType shareType);

    /**
     * Find profiles by shift type
     */
    List<ShiftProfile> findByShiftType(ShiftType shiftType);

    /**
     * Find system profiles (cannot be deleted)
     */
    List<ShiftProfile> findByIsSystemProfileTrue();

    /**
     * Find active system profiles
     */
    List<ShiftProfile> findByIsSystemProfileTrueAndIsActiveTrue();

    /**
     * Count total shifts using a specific profile
     * Used to prevent deletion of profiles in active use
     */
    @Query("SELECT COUNT(cs) FROM CabShift cs WHERE cs.currentProfile.id = :profileId")
    long countShiftsUsingProfile(@Param("profileId") Long profileId);

    /**
     * Count total expense categories linked to a profile
     */
    @Query("SELECT COUNT(ec) FROM ExpenseCategory ec WHERE ec.shiftProfileId = :profileId")
    long countExpenseCategoriesUsingProfile(@Param("profileId") Long profileId);

    /**
     * Find profiles matching shift attributes (used for auto-suggestion)
     * Profiles with NULL attributes match any value
     */
    @Query("SELECT sp FROM ShiftProfile sp WHERE sp.isActive = true " +
           "AND (sp.cabType IS NULL OR sp.cabType = :cabType) " +
           "AND (sp.shareType IS NULL OR sp.shareType = :shareType) " +
           "AND (sp.hasAirportLicense IS NULL OR sp.hasAirportLicense = :hasAirportLicense) " +
           "AND (sp.shiftType IS NULL OR sp.shiftType = :shiftType) " +
           "ORDER BY sp.displayOrder ASC")
    List<ShiftProfile> findMatchingProfiles(
            @Param("cabType") CabType cabType,
            @Param("shareType") ShareType shareType,
            @Param("hasAirportLicense") Boolean hasAirportLicense,
            @Param("shiftType") ShiftType shiftType
    );
}
