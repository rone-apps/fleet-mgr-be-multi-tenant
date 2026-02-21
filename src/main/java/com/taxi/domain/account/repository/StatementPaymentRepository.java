package com.taxi.domain.account.repository;

import com.taxi.domain.account.model.StatementPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StatementPaymentRepository extends JpaRepository<StatementPayment, Long> {

    List<StatementPayment> findByStatementId(Long statementId);

    List<StatementPayment> findByPersonId(Long personId);

    List<StatementPayment> findByPaymentBatchId(Long paymentBatchId);

    @Query("SELECT sp FROM StatementPayment sp WHERE sp.status = :status ORDER BY sp.createdAt DESC")
    List<StatementPayment> findByStatus(@Param("status") String status);

    @Query("SELECT sp FROM StatementPayment sp WHERE sp.paymentDate BETWEEN :startDate AND :endDate ORDER BY sp.paymentDate DESC")
    List<StatementPayment> findByPaymentDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT sp FROM StatementPayment sp WHERE sp.status = 'COMPLETED' AND sp.statementId = :statementId")
    List<StatementPayment> findCompletedPaymentsByStatement(@Param("statementId") Long statementId);

    @Query("SELECT sp FROM StatementPayment sp WHERE sp.personId = :personId AND sp.paymentDate BETWEEN :startDate AND :endDate AND sp.status = 'COMPLETED' ORDER BY sp.paymentDate DESC")
    List<StatementPayment> findByPersonIdAndPaymentDateRange(@Param("personId") Long personId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
