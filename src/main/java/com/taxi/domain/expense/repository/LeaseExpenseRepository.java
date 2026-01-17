package com.taxi.domain.expense.repository;

import com.taxi.domain.expense.model.LeaseExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LeaseExpenseRepository extends JpaRepository<LeaseExpense, Long> {

    /**
     * Find lease expenses between dates
     */
    @Query("SELECT le FROM LeaseExpense le WHERE le.leaseDate BETWEEN :startDate AND :endDate ORDER BY le.leaseDate DESC")
    List<LeaseExpense> findByLeaseDateBetween(
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find lease expenses for a driver
     */
    List<LeaseExpense> findByDriverIdOrderByLeaseDateDesc(Long driverId);

    /**
     * Find lease expenses for a driver between dates
     */
    @Query("SELECT le FROM LeaseExpense le WHERE le.driverId = :driverId AND le.leaseDate BETWEEN :startDate AND :endDate ORDER BY le.leaseDate DESC")
    List<LeaseExpense> findByDriverIdAndLeaseDateBetween(
        @Param("driverId") Long driverId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find lease expenses for a cab
     */
    List<LeaseExpense> findByCabIdOrderByLeaseDateDesc(Long cabId);

    /**
     * Find lease expenses for a shift
     */
    Optional<LeaseExpense> findByShiftId(Long shiftId);

    /**
     * Find unpaid lease expenses
     */
    @Query("SELECT le FROM LeaseExpense le WHERE le.isPaid = false ORDER BY le.leaseDate DESC")
    List<LeaseExpense> findUnpaid();

    /**
     * Find unpaid lease expenses for a driver
     */
    @Query("SELECT le FROM LeaseExpense le WHERE le.driverId = :driverId AND le.isPaid = false ORDER BY le.leaseDate DESC")
    List<LeaseExpense> findUnpaidByDriverId(@Param("driverId") Long driverId);

    /**
     * Get total unpaid amount for a driver
     */
    @Query("SELECT COALESCE(SUM(le.totalAmount), 0) FROM LeaseExpense le WHERE le.driverId = :driverId AND le.isPaid = false")
    java.math.BigDecimal getTotalUnpaidByDriverId(@Param("driverId") Long driverId);

    /**
     * Check if lease expense exists for a shift
     */
    boolean existsByShiftId(Long shiftId);
}
