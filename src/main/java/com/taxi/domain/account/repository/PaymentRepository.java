package com.taxi.domain.account.repository;

import com.taxi.domain.account.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Find by payment number
    Optional<Payment> findByPaymentNumber(String paymentNumber);

    // Find by invoice
    List<Payment> findByInvoiceId(Long invoiceId);

    // Find by customer
    List<Payment> findByCustomerId(Long customerId);

    // Find by account
    List<Payment> findByAccountId(String accountId);

    // Find by date range
    List<Payment> findByPaymentDateBetween(LocalDate startDate, LocalDate endDate);

    // Find by customer and date range
    List<Payment> findByCustomerIdAndPaymentDateBetween(
            Long customerId, LocalDate startDate, LocalDate endDate);

    // Find by account and date range
    List<Payment> findByAccountIdAndPaymentDateBetween(
            String accountId, LocalDate startDate, LocalDate endDate);

    // Find by payment method
    List<Payment> findByPaymentMethod(Payment.PaymentMethod paymentMethod);

    // Get total payments for invoice
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.invoice.id = :invoiceId")
    BigDecimal calculateTotalPaymentsForInvoice(@Param("invoiceId") Long invoiceId);

    // Get total payments for customer
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.customer.id = :customerId")
    BigDecimal calculateTotalPaymentsByCustomer(@Param("customerId") Long customerId);

    // Get total payments for customer in date range
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.customer.id = :customerId AND p.paymentDate BETWEEN :startDate AND :endDate")
    BigDecimal calculateTotalPaymentsByCustomerAndDateRange(
            @Param("customerId") Long customerId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // Get recent payments (last N days)
    @Query("SELECT p FROM Payment p WHERE p.paymentDate >= :sinceDate ORDER BY p.paymentDate DESC")
    List<Payment> findRecentPayments(@Param("sinceDate") LocalDate sinceDate);

    // Get next payment number (for auto-generation)
    @Query("SELECT MAX(p.paymentNumber) FROM Payment p WHERE p.paymentNumber LIKE :prefix%")
    String findMaxPaymentNumberWithPrefix(@Param("prefix") String prefix);

    // Payment summary by method
    @Query("SELECT p.paymentMethod, COUNT(p), SUM(p.amount) FROM Payment p GROUP BY p.paymentMethod")
    List<Object[]> getPaymentSummaryByMethod();

    // Payments for a specific period
    @Query("SELECT p FROM Payment p WHERE p.paymentDate BETWEEN :startDate AND :endDate ORDER BY p.paymentDate DESC")
    List<Payment> findPaymentsByPeriod(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
