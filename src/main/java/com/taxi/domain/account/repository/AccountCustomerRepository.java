package com.taxi.domain.account.repository;

import com.taxi.domain.account.model.AccountCustomer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountCustomerRepository extends JpaRepository<AccountCustomer, Long> {

    // Find by account_id
    List<AccountCustomer> findByAccountId(String accountId);

    // Find active customers by account_id
    List<AccountCustomer> findByAccountIdAndActive(String accountId, boolean active);

    // Find active customers only
    // FIXED: Changed from findByIsActiveTrue to findByActiveTrue
    List<AccountCustomer> findByActiveTrue();

    // Find by city (case-insensitive)
    List<AccountCustomer> findByCityIgnoreCase(String city);

    // Find by city and active status
    // FIXED: Changed IsActive to Active
    List<AccountCustomer> findByCityIgnoreCaseAndActive(String city, boolean active);

    // Find by province (case-insensitive)
    List<AccountCustomer> findByProvinceIgnoreCase(String province);

    // Find by province and active status
    // FIXED: Changed IsActive to Active
    List<AccountCustomer> findByProvinceIgnoreCaseAndActive(String province, boolean active);

    // Search by company name (partial match, case-insensitive)
    @Query("SELECT c FROM AccountCustomer c WHERE LOWER(c.companyName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<AccountCustomer> searchByCompanyName(@Param("name") String name);

    // Find by email
    Optional<AccountCustomer> findByEmailIgnoreCase(String email);

    // Check if company name exists (for duplicate prevention)
    boolean existsByCompanyNameIgnoreCase(String companyName);

    // Find customers with outstanding balance (have unpaid charges)
    @Query("SELECT DISTINCT c FROM AccountCustomer c JOIN AccountCharge ac ON ac.accountCustomer.id = c.id " +
           "WHERE ac.paid = false")
    List<AccountCustomer> findCustomersWithOutstandingBalance();

    // Find by billing period
    List<AccountCustomer> findByBillingPeriod(String billingPeriod);

    // Find active customers by billing period
    // FIXED: Changed IsActive to Active
    List<AccountCustomer> findByBillingPeriodAndActive(String billingPeriod, boolean active);
}