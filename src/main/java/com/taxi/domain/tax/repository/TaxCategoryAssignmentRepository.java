package com.taxi.domain.tax.repository;

import com.taxi.domain.tax.model.TaxCategoryAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TaxCategoryAssignmentRepository extends JpaRepository<TaxCategoryAssignment, Long> {

    @Query("SELECT a FROM TaxCategoryAssignment a JOIN FETCH a.taxType JOIN FETCH a.expenseCategory " +
           "WHERE a.isActive = true ORDER BY a.taxType.name, a.expenseCategory.categoryName")
    List<TaxCategoryAssignment> findAllActiveWithDetails();

    @Query("SELECT a FROM TaxCategoryAssignment a JOIN FETCH a.taxType JOIN FETCH a.expenseCategory " +
           "ORDER BY a.taxType.name, a.assignedAt DESC")
    List<TaxCategoryAssignment> findAllWithDetails();

    @Query("SELECT a FROM TaxCategoryAssignment a JOIN FETCH a.taxType JOIN FETCH a.expenseCategory " +
           "WHERE a.taxType.id = :taxTypeId ORDER BY a.assignedAt DESC")
    List<TaxCategoryAssignment> findByTaxTypeIdWithDetails(@Param("taxTypeId") Long taxTypeId);

    @Query("SELECT a FROM TaxCategoryAssignment a WHERE a.taxType.id = :taxTypeId " +
           "AND a.expenseCategory.id = :categoryId AND a.isActive = true")
    Optional<TaxCategoryAssignment> findActiveAssignment(@Param("taxTypeId") Long taxTypeId,
                                                          @Param("categoryId") Long categoryId);
}
