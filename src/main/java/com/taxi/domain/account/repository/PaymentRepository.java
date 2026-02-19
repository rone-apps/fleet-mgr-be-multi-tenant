package com.taxi.domain.account.repository;

import com.taxi.domain.account.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByStatementId(Long statementId);

    // Alias for findByStatementId for backwards compatibility
    @Query("SELECT p FROM Payment p WHERE p.statement.id = :invoiceId")
    List<Payment> findByInvoiceId(@Param("invoiceId") Long invoiceId);

    List<Payment> findByPaymentBatchId(Long paymentBatchId);

    @Query("SELECT p FROM Payment p WHERE p.statement.customer.id = :customerId")
    List<Payment> findByCustomerId(@Param("customerId") Long customerId);

    @Query("SELECT p FROM Payment p WHERE p.statement.id = :statementId AND p.status = 'COMPLETED'")
    List<Payment> findCompletedPaymentsByStatement(@Param("statementId") Long statementId);

    @Query("SELECT p FROM Payment p WHERE p.paymentDate BETWEEN :startDate AND :endDate ORDER BY p.paymentDate DESC")
    List<Payment> findByPaymentDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT p FROM Payment p WHERE p.status = :status ORDER BY p.createdAt DESC")
    List<Payment> findByStatus(@Param("status") String status);

    // For backwards compatibility with legacy method
    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(p.referenceNumber, LENGTH(:prefix) + 1) AS INT)), 0) FROM Payment p WHERE p.referenceNumber LIKE CONCAT(:prefix, '%')")
    String findMaxPaymentNumberWithPrefix(@Param("prefix") String prefix);
}
