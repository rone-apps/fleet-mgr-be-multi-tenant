package com.taxi.domain.revenue.repository;

import com.taxi.domain.revenue.entity.OtherRevenue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface OtherRevenueRepository extends JpaRepository<OtherRevenue, Long> {
    
    // Find revenues between dates
    List<OtherRevenue> findByRevenueDateBetween(LocalDate startDate, LocalDate endDate);
    
    // Find by category
    List<OtherRevenue> findByCategory_Id(Long categoryId);
    
    // Find by entity type and ID
    List<OtherRevenue> findByEntityTypeAndEntityId(OtherRevenue.EntityType entityType, Long entityId);
    
    // Find by payment status
    List<OtherRevenue> findByPaymentStatus(OtherRevenue.PaymentStatus paymentStatus);
    
    // Find by revenue type
    List<OtherRevenue> findByRevenueType(OtherRevenue.RevenueType revenueType);
    
    // Find by cab
    List<OtherRevenue> findByCab_Id(Long cabId);
    
    // Find by driver
    List<OtherRevenue> findByDriver_Id(Long driverId);
    
    // Find by owner
    List<OtherRevenue> findByOwner_Id(Long ownerId);
    
    // Find by shift
    List<OtherRevenue> findByShift_Id(Long shiftId);
    
    // Complex queries
    @Query("SELECT r FROM OtherRevenue r WHERE r.revenueDate BETWEEN :startDate AND :endDate " +
           "AND (:categoryId IS NULL OR r.category.id = :categoryId) " +
           "AND (:entityType IS NULL OR r.entityType = :entityType) " +
           "AND (:paymentStatus IS NULL OR r.paymentStatus = :paymentStatus) " +
           "ORDER BY r.revenueDate DESC, r.createdAt DESC")
    List<OtherRevenue> findWithFilters(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("categoryId") Long categoryId,
        @Param("entityType") OtherRevenue.EntityType entityType,
        @Param("paymentStatus") OtherRevenue.PaymentStatus paymentStatus
    );

    @Query("SELECT r FROM OtherRevenue r " +
           "LEFT JOIN FETCH r.category " +
           "LEFT JOIN FETCH r.driver " +
           "LEFT JOIN FETCH r.owner " +
           "LEFT JOIN FETCH r.cab " +
           "LEFT JOIN FETCH r.shift " +
           "WHERE r.revenueDate BETWEEN :startDate AND :endDate " +
           "AND (" +
           "     (r.entityType = :driverEntityType AND r.entityId = :driverId) OR " +
           "     (r.entityType = :ownerEntityType AND r.entityId = :driverId) OR " +
           "     (r.driver IS NOT NULL AND r.driver.id = :driverId) OR " +
           "     (r.owner IS NOT NULL AND r.owner.id = :driverId)" +
           ") " +
           "ORDER BY r.revenueDate DESC, r.createdAt DESC")
    List<OtherRevenue> findForDriverBetweenDatesWithTypes(
        @Param("driverId") Long driverId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("driverEntityType") OtherRevenue.EntityType driverEntityType,
        @Param("ownerEntityType") OtherRevenue.EntityType ownerEntityType
    );
    
    // Convenience default method that passes the enum values
    default List<OtherRevenue> findForDriverBetweenDates(Long driverId, LocalDate startDate, LocalDate endDate) {
        return findForDriverBetweenDatesWithTypes(
            driverId, 
            startDate, 
            endDate, 
            OtherRevenue.EntityType.DRIVER, 
            OtherRevenue.EntityType.OWNER
        );
    }
    
    // Total revenue by date range
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM OtherRevenue r " +
           "WHERE r.revenueDate BETWEEN :startDate AND :endDate " +
           "AND r.paymentStatus = 'PAID'")
    BigDecimal getTotalRevenueBetweenDates(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    // Total revenue by category and date range
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM OtherRevenue r " +
           "WHERE r.category.id = :categoryId " +
           "AND r.revenueDate BETWEEN :startDate AND :endDate " +
           "AND r.paymentStatus = 'PAID'")
    BigDecimal getTotalRevenueByCategoryAndDateRange(
        @Param("categoryId") Long categoryId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    // Total revenue by entity
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM OtherRevenue r " +
           "WHERE r.entityType = :entityType " +
           "AND r.entityId = :entityId " +
           "AND r.revenueDate BETWEEN :startDate AND :endDate " +
           "AND r.paymentStatus = 'PAID'")
    BigDecimal getTotalRevenueByEntityTypeAndEntityIdAndRevenueDateBetween(
        @Param("entityType") OtherRevenue.EntityType entityType,
        @Param("entityId") Long entityId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}