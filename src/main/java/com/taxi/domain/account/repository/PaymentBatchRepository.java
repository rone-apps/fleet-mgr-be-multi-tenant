package com.taxi.domain.account.repository;

import com.taxi.domain.account.model.PaymentBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentBatchRepository extends JpaRepository<PaymentBatch, Long> {

    Optional<PaymentBatch> findByBatchNumber(String batchNumber);

    @Query("SELECT p FROM PaymentBatch p ORDER BY p.createdAt DESC")
    List<PaymentBatch> findAll();

    @Query("SELECT p FROM PaymentBatch p WHERE p.status = :status ORDER BY p.createdAt DESC")
    List<PaymentBatch> findByStatus(@Param("status") String status);

    @Query("SELECT p FROM PaymentBatch p WHERE p.batchDate BETWEEN :startDate AND :endDate ORDER BY p.batchDate DESC")
    List<PaymentBatch> findByBatchDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT p FROM PaymentBatch p WHERE p.periodStart = :periodStart AND p.periodEnd = :periodEnd")
    List<PaymentBatch> findByPeriod(@Param("periodStart") LocalDate periodStart, @Param("periodEnd") LocalDate periodEnd);
}
