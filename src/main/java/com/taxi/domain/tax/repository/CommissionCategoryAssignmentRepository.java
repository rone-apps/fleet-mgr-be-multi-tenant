package com.taxi.domain.tax.repository;

import com.taxi.domain.tax.model.CommissionCategoryAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommissionCategoryAssignmentRepository extends JpaRepository<CommissionCategoryAssignment, Long> {

    @Query("SELECT a FROM CommissionCategoryAssignment a JOIN FETCH a.commissionType JOIN FETCH a.revenueCategory " +
           "WHERE a.isActive = true ORDER BY a.commissionType.name, a.revenueCategory.categoryName")
    List<CommissionCategoryAssignment> findAllActiveWithDetails();

    @Query("SELECT a FROM CommissionCategoryAssignment a JOIN FETCH a.commissionType JOIN FETCH a.revenueCategory " +
           "ORDER BY a.commissionType.name, a.assignedAt DESC")
    List<CommissionCategoryAssignment> findAllWithDetails();

    @Query("SELECT a FROM CommissionCategoryAssignment a JOIN FETCH a.commissionType JOIN FETCH a.revenueCategory " +
           "WHERE a.commissionType.id = :typeId ORDER BY a.assignedAt DESC")
    List<CommissionCategoryAssignment> findByCommissionTypeIdWithDetails(@Param("typeId") Long typeId);

    @Query("SELECT a FROM CommissionCategoryAssignment a WHERE a.commissionType.id = :typeId " +
           "AND a.revenueCategory.id = :categoryId AND a.isActive = true")
    Optional<CommissionCategoryAssignment> findActiveAssignment(@Param("typeId") Long typeId,
                                                                 @Param("categoryId") Long categoryId);
}
