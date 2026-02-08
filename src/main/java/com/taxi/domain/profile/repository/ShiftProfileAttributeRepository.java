package com.taxi.domain.profile.repository;

import com.taxi.domain.profile.model.ShiftProfileAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ShiftProfileAttribute entity
 * Manages associations between profiles and dynamic attributes
 */
@Repository
public interface ShiftProfileAttributeRepository extends JpaRepository<ShiftProfileAttribute, Long> {

    /**
     * Get all attributes for a profile
     */
    List<ShiftProfileAttribute> findByProfileId(Long profileId);

    /**
     * Find attribute association between profile and attribute type
     */
    Optional<ShiftProfileAttribute> findByProfileIdAndAttributeTypeId(Long profileId, Long attributeTypeId);

    /**
     * Get all required attributes for a profile
     */
    List<ShiftProfileAttribute> findByProfileIdAndIsRequiredTrue(Long profileId);

    /**
     * Get all excluded attributes for a profile (must not have)
     */
    List<ShiftProfileAttribute> findByProfileIdAndIsRequiredFalse(Long profileId);

    /**
     * Get attributes of a specific type across all profiles
     * Useful for finding which profiles care about a specific attribute
     */
    List<ShiftProfileAttribute> findByAttributeTypeId(Long attributeTypeId);

    /**
     * Delete all attributes for a profile (cleanup when profile is deleted)
     */
    void deleteByProfileId(Long profileId);

    /**
     * Check if a specific attribute is required by a profile
     */
    @Query("SELECT COUNT(spa) > 0 FROM ShiftProfileAttribute spa " +
           "WHERE spa.profile.id = :profileId AND spa.attributeType.id = :attributeTypeId AND spa.isRequired = true")
    boolean isAttributeRequired(@Param("profileId") Long profileId, @Param("attributeTypeId") Long attributeTypeId);
}
