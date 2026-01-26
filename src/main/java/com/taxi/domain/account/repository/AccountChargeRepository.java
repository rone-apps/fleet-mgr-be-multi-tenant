package com.taxi.domain.account.repository;

import com.taxi.domain.account.model.AccountCharge;
import com.taxi.domain.account.model.AccountCustomer;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Repository
public interface AccountChargeRepository extends JpaRepository<AccountCharge, Long> {

    @EntityGraph(attributePaths = {"accountCustomer", "cab", "driver"})
    Page<AccountCharge> findAllBy(Pageable pageable);

    @EntityGraph(attributePaths = {"accountCustomer", "cab", "driver"})
    Page<AccountCharge> findByAccountCustomerCompanyNameContainingIgnoreCase(String customerName, Pageable pageable);

    @EntityGraph(attributePaths = {"accountCustomer", "cab", "driver"})
    @Query(
            value = "SELECT ac FROM AccountCharge ac " +
                    "WHERE (:customerName IS NULL OR LOWER(ac.accountCustomer.companyName) LIKE LOWER(CONCAT('%', :customerName, '%'))) " +
                    "AND (:cabId IS NULL OR ac.cab.id = :cabId) " +
                    "AND (:driverId IS NULL OR ac.driver.id = :driverId) " +
                    "AND (:startDate IS NULL OR ac.tripDate >= :startDate) " +
                    "AND (:endDate IS NULL OR ac.tripDate <= :endDate)",
            countQuery = "SELECT COUNT(ac) FROM AccountCharge ac " +
                    "WHERE (:customerName IS NULL OR LOWER(ac.accountCustomer.companyName) LIKE LOWER(CONCAT('%', :customerName, '%'))) " +
                    "AND (:cabId IS NULL OR ac.cab.id = :cabId) " +
                    "AND (:driverId IS NULL OR ac.driver.id = :driverId) " +
                    "AND (:startDate IS NULL OR ac.tripDate >= :startDate) " +
                    "AND (:endDate IS NULL OR ac.tripDate <= :endDate)"
    )
    Page<AccountCharge> searchCharges(
            @Param("customerName") String customerName,
            @Param("cabId") Long cabId,
            @Param("driverId") Long driverId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

        /**
     * Find all charges for a specific driver within a date range
     */
    @EntityGraph(attributePaths = {"accountCustomer", "cab", "driver"})
    @Query("SELECT ac FROM AccountCharge ac " +
           "WHERE ac.driver.driverNumber = :driverNumber " +
           "AND ac.tripDate BETWEEN :startDate AND :endDate " +
           "ORDER BY ac.tripDate, ac.startTime")
    List<AccountCharge> findByDriverNumberAndDateRange(
            @Param("driverNumber") String driverNumber,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );


    // Find by account_id
    @EntityGraph(attributePaths = {"accountCustomer", "cab", "driver"})
    List<AccountCharge> findByAccountId(String accountId);

    // Find by account_id and paid status
    @EntityGraph(attributePaths = {"accountCustomer", "cab", "driver"})
    List<AccountCharge> findByAccountIdAndPaidFalse(String accountId);

    // Find by account_id and date range
    @EntityGraph(attributePaths = {"accountCustomer", "cab", "driver"})
    List<AccountCharge> findByAccountIdAndTripDateBetween(
            String accountId,
            LocalDate startDate,
            LocalDate endDate
    );

    // Find by customer
    @EntityGraph(attributePaths = {"accountCustomer", "cab", "driver"})
    List<AccountCharge> findByAccountCustomerId(Long customerId);

    // Find unpaid charges by customer
    // FIXED: Changed from findByIsPaidFalse to findByPaidFalse
    @EntityGraph(attributePaths = {"accountCustomer", "cab", "driver"})
    List<AccountCharge> findByAccountCustomerIdAndPaidFalse(Long customerId);

    // Find charges by customer and date range
    @EntityGraph(attributePaths = {"accountCustomer", "cab", "driver"})
    List<AccountCharge> findByAccountCustomerIdAndTripDateBetween(
            Long customerId,
            LocalDate startDate,
            LocalDate endDate
    );

    // Find unpaid charges by customer and date range
    // FIXED: Changed from isPaid to paid
    @EntityGraph(attributePaths = {"accountCustomer", "cab", "driver"})
    List<AccountCharge> findByAccountCustomerIdAndPaidFalseAndTripDateBetween(
            Long customerId,
            LocalDate startDate,
            LocalDate endDate
    );

    // Find unpaid charges by customer and date range that are NOT already on an invoice
    @EntityGraph(attributePaths = {"accountCustomer", "cab", "driver"})
    @Query("SELECT c FROM AccountCharge c WHERE c.accountCustomer.id = :customerId " +
           "AND c.paid = false AND c.invoiceId IS NULL " +
           "AND c.tripDate BETWEEN :startDate AND :endDate")
    List<AccountCharge> findUninvoicedChargesByCustomerAndDateRange(
            @Param("customerId") Long customerId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

     
    /**
     * Find charges by customer with pagination
     */
    @EntityGraph(attributePaths = {"accountCustomer", "cab", "driver"})
    Page<AccountCharge> findByAccountCustomerId(Long customerId, Pageable pageable);
    

    // Find by job code
    @EntityGraph(attributePaths = {"accountCustomer", "cab", "driver"})
    List<AccountCharge> findByJobCodeIgnoreCase(String jobCode);
    
    // Find existing charge by unique constraint fields (for upsert logic)
    @Query("SELECT c FROM AccountCharge c WHERE c.accountId = :accountId " +
           "AND (c.cab.id = :cabId OR (c.cab IS NULL AND :cabId IS NULL)) " +
           "AND (c.driver.id = :driverId OR (c.driver IS NULL AND :driverId IS NULL)) " +
           "AND c.tripDate = :tripDate " +
           "AND (c.startTime = :startTime OR (c.startTime IS NULL AND :startTime IS NULL)) " +
           "AND c.jobCode = :jobCode")
    java.util.Optional<AccountCharge> findByUniqueConstraint(
            @Param("accountId") String accountId,
            @Param("cabId") Long cabId,
            @Param("driverId") Long driverId,
            @Param("tripDate") LocalDate tripDate,
            @Param("startTime") java.time.LocalTime startTime,
            @Param("jobCode") String jobCode
    );

    // Find by date range
    @EntityGraph(attributePaths = {"accountCustomer", "cab", "driver"})
    List<AccountCharge> findByTripDateBetween(LocalDate startDate, LocalDate endDate);

    // Find all unpaid charges
    // FIXED: Changed from findByIsPaidFalse to findByPaidFalse
    @EntityGraph(attributePaths = {"accountCustomer", "cab", "driver"})
    List<AccountCharge> findByPaidFalse();

    // Find overdue charges (unpaid and older than cutoff date)
    @EntityGraph(attributePaths = {"accountCustomer", "cab", "driver"})
    @Query("SELECT c FROM AccountCharge c WHERE c.paid = false AND c.tripDate < :cutoffDate")
    List<AccountCharge> findOverdueCharges(@Param("cutoffDate") LocalDate cutoffDate);

    // Calculate unpaid total for a customer
    @Query("SELECT COALESCE(SUM(c.fareAmount + c.tipAmount), 0) FROM AccountCharge c " +
           "WHERE c.accountCustomer.id = :customerId AND c.paid = false")
    BigDecimal calculateUnpaidTotal(@Param("customerId") Long customerId);

    // Calculate total for a period
    @Query("SELECT COALESCE(SUM(c.fareAmount + c.tipAmount), 0) FROM AccountCharge c " +
           "WHERE c.accountCustomer.id = :customerId AND c.tripDate BETWEEN :startDate AND :endDate")
    BigDecimal calculateTotalForPeriod(
            @Param("customerId") Long customerId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // Find by driver
    @EntityGraph(attributePaths = {"accountCustomer", "cab", "driver"})
    List<AccountCharge> findByDriverId(Long driverId);

    // Find by cab
    @EntityGraph(attributePaths = {"accountCustomer", "cab", "driver"})
    List<AccountCharge> findByCabId(Long cabId);

    // Count unpaid charges for customer
    // FIXED: Changed from isPaid to paid
    Long countByAccountCustomerIdAndPaidFalse(Long customerId);

    // Find by invoice number
    @EntityGraph(attributePaths = {"accountCustomer", "cab", "driver"})
    List<AccountCharge> findByInvoiceNumber(String invoiceNumber);

    // Find charges for billing (unpaid in date range)
    @EntityGraph(attributePaths = {"accountCustomer", "cab", "driver"})
    @Query("SELECT c FROM AccountCharge c WHERE c.paid = false AND c.tripDate BETWEEN :startDate AND :endDate")
    List<AccountCharge> findChargesForBilling(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // Group charges by customer for billing
    @EntityGraph(attributePaths = {"accountCustomer", "cab", "driver"})
    @Query("SELECT c FROM AccountCharge c WHERE c.paid = false AND c.tripDate BETWEEN :startDate AND :endDate ORDER BY c.accountCustomer.id, c.tripDate")
    List<AccountCharge> findChargesGroupedByCustomerForBilling(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // Search charges without pagination
    @EntityGraph(attributePaths = {"accountCustomer", "cab", "driver"})
    @Query(
            value = "SELECT ac FROM AccountCharge ac " +
                    "WHERE (:customerName IS NULL OR LOWER(ac.accountCustomer.companyName) LIKE LOWER(CONCAT('%', :customerName, '%'))) " +
                    "AND (:cabId IS NULL OR ac.cab.id = :cabId) " +
                    "AND (:driverId IS NULL OR ac.driver.id = :driverId) " +
                    "AND (:startDate IS NULL OR ac.tripDate >= :startDate) " +
                    "AND (:endDate IS NULL OR ac.tripDate <= :endDate)"
    )
    List<AccountCharge> searchChargesNoPaging(
            @Param("customerName") String customerName,
            @Param("cabId") Long cabId,
            @Param("driverId") Long driverId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // Get summary statistics for filtered charges
    @Query(
            value = "SELECT new map(" +
                    "COUNT(ac) as totalChargesCount, " +
                    "SUM(CASE WHEN ac.paid = false THEN 1 ELSE 0 END) as unpaidChargesCount, " +
                    "COALESCE(SUM(CASE WHEN ac.paid = false THEN ac.fareAmount + ac.tipAmount ELSE 0 END), 0) as outstandingBalance" +
                    ") FROM AccountCharge ac " +
                    "WHERE (:customerName IS NULL OR LOWER(ac.accountCustomer.companyName) LIKE LOWER(CONCAT('%', :customerName, '%'))) " +
                    "AND (:cabId IS NULL OR ac.cab.id = :cabId) " +
                    "AND (:driverId IS NULL OR ac.driver.id = :driverId) " +
                    "AND (:startDate IS NULL OR ac.tripDate >= :startDate) " +
                    "AND (:endDate IS NULL OR ac.tripDate <= :endDate)"
    )
    Map<String, Object> getSummaryStatistics(
            @Param("customerName") String customerName,
            @Param("cabId") Long cabId,
            @Param("driverId") Long driverId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}