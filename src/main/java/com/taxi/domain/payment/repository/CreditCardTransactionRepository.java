package com.taxi.domain.payment.repository;

import com.taxi.domain.payment.model.CreditCardTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for CreditCardTransaction entity (UPDATED for business identifiers)
 * Uses cab_number and driver_number instead of IDs
 */
@Repository
public interface CreditCardTransactionRepository extends JpaRepository<CreditCardTransaction, Long> {

    /**
     * Find transaction by transaction ID (from payment processor)
     */
    Optional<CreditCardTransaction> findByTransactionId(String transactionId);

    /**
     * Paginated queries for data view
     */
    Page<CreditCardTransaction> findByTransactionDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);
    
    /**
     * Non-paginated query for loading existing transactions (for duplicate checking)
     */
    List<CreditCardTransaction> findByTransactionDateBetween(LocalDate startDate, LocalDate endDate);
    
    Page<CreditCardTransaction> findByTransactionDateBetweenAndCabNumber(
        LocalDate startDate, LocalDate endDate, String cabNumber, Pageable pageable);
    
    Page<CreditCardTransaction> findByTransactionDateBetweenAndDriverNumber(
        LocalDate startDate, LocalDate endDate, String driverNumber, Pageable pageable);
    
    Page<CreditCardTransaction> findByTransactionDateBetweenAndCabNumberAndDriverNumber(
        LocalDate startDate, LocalDate endDate, String cabNumber, String driverNumber, Pageable pageable);

    /**
     * Check if transaction exists by primary unique key
     */
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM CreditCardTransaction t " +
           "WHERE t.terminalId = :terminalId " +
           "AND t.authorizationCode = :authCode " +
           "AND t.amount = :amount " +
           "AND t.transactionDate = :date " +
           "AND t.transactionTime = :time")
    boolean existsByPrimaryKey(
            @Param("terminalId") String terminalId,
            @Param("authCode") String authorizationCode,
            @Param("amount") java.math.BigDecimal amount,
            @Param("date") LocalDate date,
            @Param("time") java.time.LocalTime time
    );

    /**
     * Find all transactions for a specific driver number within a date range
     */
    @Query("SELECT t FROM CreditCardTransaction t " +
           "WHERE t.driverNumber = :driverNumber " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "ORDER BY t.transactionDate, t.transactionTime")
    List<CreditCardTransaction> findByDriverNumberAndDateRange(
            @Param("driverNumber") String driverNumber,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Find all transactions for a driver number (including null drivers) within a date range
     * Useful for reports where driver might have been logged off
     */
    @Query("SELECT t FROM CreditCardTransaction t " +
           "WHERE (t.driverNumber IS NULL OR t.driverNumber = :driverNumber) " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "ORDER BY t.transactionDate, t.transactionTime")
    List<CreditCardTransaction> findByDriverNumberOrNullAndDateRange(
            @Param("driverNumber") String driverNumber,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Find all transactions for a specific cab number within a date range
     */
    @Query("SELECT t FROM CreditCardTransaction t " +
           "WHERE t.cabNumber = :cabNumber " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "ORDER BY t.transactionDate, t.transactionTime")
    List<CreditCardTransaction> findByCabNumberAndDateRange(
            @Param("cabNumber") String cabNumber,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Find all transactions for a specific driver number
     */
    List<CreditCardTransaction> findByDriverNumber(String driverNumber);

    /**
     * Find all transactions for a specific cab number
     */
    List<CreditCardTransaction> findByCabNumber(String cabNumber);

    /**
     * Find all transactions by upload batch ID
     */
    List<CreditCardTransaction> findByUploadBatchId(String uploadBatchId);

    /**
     * Find all transactions by terminal ID
     */
    List<CreditCardTransaction> findByTerminalId(String terminalId);

    /**
     * Find all settled transactions
     */
    List<CreditCardTransaction> findByIsSettled(Boolean isSettled);

    /**
     * Find all transactions by status
     */
    List<CreditCardTransaction> findByTransactionStatus(CreditCardTransaction.TransactionStatus status);

    /**
     * Find unsettled transactions older than a certain date
     */
    @Query("SELECT t FROM CreditCardTransaction t " +
           "WHERE t.isSettled = false " +
           "AND t.transactionDate < :date")
    List<CreditCardTransaction> findUnsettledTransactionsOlderThan(@Param("date") LocalDate date);

    /**
     * Get total amount for driver in date range
     */
    @Query("SELECT SUM(t.amount + COALESCE(t.tipAmount, 0)) FROM CreditCardTransaction t " +
           "WHERE t.driverNumber = :driverNumber " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "AND t.isSettled = true " +
           "AND t.isRefunded = false")
    java.math.BigDecimal getTotalAmountForDriver(
            @Param("driverNumber") String driverNumber,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Find transactions without driver assigned (driver_number is null)
     */
    @Query("SELECT t FROM CreditCardTransaction t " +
           "WHERE t.driverNumber IS NULL " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate")
    List<CreditCardTransaction> findUnassignedTransactions(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Find transactions without cab assigned (cab_number is null)
     */
    @Query("SELECT t FROM CreditCardTransaction t " +
           "WHERE t.cabNumber IS NULL " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate")
    List<CreditCardTransaction> findTransactionsWithoutCab(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Count transactions by upload batch
     */
    @Query("SELECT COUNT(t) FROM CreditCardTransaction t " +
           "WHERE t.uploadBatchId = :batchId")
    long countByUploadBatchId(@Param("batchId") String batchId);

    /**
     * Find transactions by cab and driver (both specified)
     */
    @Query("SELECT t FROM CreditCardTransaction t " +
           "WHERE t.cabNumber = :cabNumber " +
           "AND t.driverNumber = :driverNumber " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate")
    List<CreditCardTransaction> findByCabAndDriver(
            @Param("cabNumber") String cabNumber,
            @Param("driverNumber") String driverNumber,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Get transaction summary by card type for a driver
     */
    @Query("SELECT t.cardType, COUNT(t), SUM(t.amount + COALESCE(t.tipAmount, 0)) " +
           "FROM CreditCardTransaction t " +
           "WHERE t.driverNumber = :driverNumber " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "GROUP BY t.cardType")
    List<Object[]> getCardTypeSummaryForDriver(
            @Param("driverNumber") String driverNumber,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

       /**
     * Check if a transaction already exists (duplicate detection)
     * Uses the unique constraint: terminal_id, authorization_code, amount, transaction_date, transaction_time
     */
    boolean existsByTerminalIdAndAuthorizationCodeAndAmountAndTransactionDateAndTransactionTime(
        String terminalId,
        String authorizationCode,
        BigDecimal amount,
        LocalDate transactionDate,
        LocalTime transactionTime
    );
}