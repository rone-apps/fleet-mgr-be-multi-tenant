package com.taxi.domain.revenue.repository;

import com.taxi.domain.revenue.entity.RevenueCategory;
import com.taxi.domain.revenue.entity.RevenueCategory.AppliesTo;
import com.taxi.domain.revenue.entity.RevenueCategory.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RevenueCategoryRepository extends JpaRepository<RevenueCategory, Long> {

    // Find by unique code
    Optional<RevenueCategory> findByCategoryCode(String categoryCode);

    // Find by name
    Optional<RevenueCategory> findByCategoryName(String categoryName);

    // Find all active categories
    List<RevenueCategory> findByIsActiveTrue();

    // Find by applies_to
    List<RevenueCategory> findByAppliesTo(AppliesTo appliesTo);

    // Find active categories by applies_to
    @Query("SELECT rc FROM RevenueCategory rc WHERE rc.appliesTo = :appliesTo AND rc.isActive = true")
    List<RevenueCategory> findActiveByAppliesTo(@Param("appliesTo") AppliesTo appliesTo);

    // Find by category type
    List<RevenueCategory> findByCategoryType(CategoryType categoryType);

    // Find active categories by type
    @Query("SELECT rc FROM RevenueCategory rc WHERE rc.categoryType = :categoryType AND rc.isActive = true")
    List<RevenueCategory> findActiveByCategoryType(@Param("categoryType") CategoryType categoryType);

    // Find active categories by applies_to and type
    @Query("SELECT rc FROM RevenueCategory rc WHERE rc.appliesTo = :appliesTo " +
           "AND rc.categoryType = :categoryType AND rc.isActive = true")
    List<RevenueCategory> findActiveByAppliesToAndType(
        @Param("appliesTo") AppliesTo appliesTo,
        @Param("categoryType") CategoryType categoryType
    );

    // Search by name (partial match)
    @Query("SELECT rc FROM RevenueCategory rc WHERE LOWER(rc.categoryName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<RevenueCategory> searchByName(@Param("searchTerm") String searchTerm);

    // Search by code (partial match)
    @Query("SELECT rc FROM RevenueCategory rc WHERE LOWER(rc.categoryCode) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<RevenueCategory> searchByCode(@Param("searchTerm") String searchTerm);

    // Count active categories
    @Query("SELECT COUNT(rc) FROM RevenueCategory rc WHERE rc.isActive = true")
    Long countActive();

    // Count categories by type
    @Query("SELECT COUNT(rc) FROM RevenueCategory rc WHERE rc.categoryType = :categoryType")
    Long countByType(@Param("categoryType") CategoryType categoryType);

    // Check if category code exists
    boolean existsByCategoryCode(String categoryCode);

    // Check if category name exists
    boolean existsByCategoryName(String categoryName);
}