package com.taxi.domain.account.repository;

import com.taxi.domain.account.dto.InvoiceSummaryDTO;
import com.taxi.domain.account.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    @Query(
            "SELECT new com.taxi.domain.account.dto.InvoiceSummaryDTO(" +
                    "i.id, i.invoiceNumber, i.accountId, i.customer.id, i.customer.companyName, " +
                    "i.invoiceDate, i.dueDate, i.status, i.totalAmount, i.amountPaid, i.balanceDue" +
            ") " +
            "FROM Invoice i"
    )
    List<InvoiceSummaryDTO> findAllSummaries();

    // Find by invoice number
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    // Find by account_id
    List<Invoice> findByAccountId(String accountId);

    // Find by customer
    List<Invoice> findByCustomerId(Long customerId);

    // Find by customer and status
    List<Invoice> findByCustomerIdAndStatus(Long customerId, Invoice.InvoiceStatus status);

    // Find by account and status
    List<Invoice> findByAccountIdAndStatus(String accountId, Invoice.InvoiceStatus status);

    // Find by status
    List<Invoice> findByStatus(Invoice.InvoiceStatus status);

    // Find by date range
    List<Invoice> findByInvoiceDateBetween(LocalDate startDate, LocalDate endDate);

    // Find by customer and date range
    List<Invoice> findByCustomerIdAndInvoiceDateBetween(
            Long customerId, LocalDate startDate, LocalDate endDate);

    // Find overdue invoices
    @Query("SELECT i FROM Invoice i WHERE i.status IN ('SENT', 'PARTIAL') AND i.dueDate < CURRENT_DATE")
    List<Invoice> findOverdueInvoices();

    // Find unpaid invoices for customer
    @Query("SELECT i FROM Invoice i WHERE i.customer.id = :customerId AND i.status IN ('SENT', 'PARTIAL', 'OVERDUE')")
    List<Invoice> findUnpaidInvoicesByCustomer(@Param("customerId") Long customerId);

    // Find unpaid invoices for account
    @Query("SELECT i FROM Invoice i WHERE i.accountId = :accountId AND i.status IN ('SENT', 'PARTIAL', 'OVERDUE')")
    List<Invoice> findUnpaidInvoicesByAccount(@Param("accountId") String accountId);

    // Calculate total outstanding for customer
    @Query("SELECT COALESCE(SUM(i.balanceDue), 0) FROM Invoice i WHERE i.customer.id = :customerId AND i.status IN ('SENT', 'PARTIAL', 'OVERDUE')")
    BigDecimal calculateOutstandingBalance(@Param("customerId") Long customerId);

    // Calculate total outstanding for account
    @Query("SELECT COALESCE(SUM(i.balanceDue), 0) FROM Invoice i WHERE i.accountId = :accountId AND i.status IN ('SENT', 'PARTIAL', 'OVERDUE')")
    BigDecimal calculateOutstandingBalanceByAccount(@Param("accountId") String accountId);

    // Get next invoice number (for auto-generation)
    @Query("SELECT MAX(i.invoiceNumber) FROM Invoice i WHERE i.invoiceNumber LIKE :prefix%")
    String findMaxInvoiceNumberWithPrefix(@Param("prefix") String prefix);

    // Find invoices due in next N days
    @Query("SELECT i FROM Invoice i WHERE i.status IN ('SENT', 'PARTIAL') AND i.dueDate BETWEEN CURRENT_DATE AND :futureDate")
    List<Invoice> findInvoicesDueSoon(@Param("futureDate") LocalDate futureDate);

    // Count invoices by status for dashboard
    @Query("SELECT i.status, COUNT(i) FROM Invoice i GROUP BY i.status")
    List<Object[]> countByStatus();

    // Get invoice summary for customer
    @Query("SELECT new map(COUNT(i) as total, " +
           "SUM(CASE WHEN i.status = 'PAID' THEN 1 ELSE 0 END) as paid, " +
           "SUM(CASE WHEN i.status IN ('SENT', 'PARTIAL', 'OVERDUE') THEN 1 ELSE 0 END) as unpaid, " +
           "SUM(i.totalAmount) as totalAmount, " +
           "SUM(i.amountPaid) as amountPaid, " +
           "SUM(i.balanceDue) as balanceDue) " +
           "FROM Invoice i WHERE i.customer.id = :customerId")
    List<Object> getInvoiceSummaryByCustomer(@Param("customerId") Long customerId);
}
