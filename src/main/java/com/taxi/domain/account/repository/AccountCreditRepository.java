package com.taxi.domain.account.repository;

import com.taxi.domain.account.model.AccountCredit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface AccountCreditRepository extends JpaRepository<AccountCredit, Long> {

    List<AccountCredit> findByCustomerId(Long customerId);

    List<AccountCredit> findByAccountId(String accountId);

    List<AccountCredit> findByCustomerIdAndIsActiveTrue(Long customerId);

    List<AccountCredit> findByAccountIdAndIsActiveTrue(String accountId);

    @Query("SELECT SUM(ac.remainingAmount) FROM AccountCredit ac WHERE ac.customer.id = :customerId AND ac.isActive = true")
    BigDecimal calculateTotalRemainingCredit(@Param("customerId") Long customerId);

    @Query("SELECT SUM(ac.remainingAmount) FROM AccountCredit ac WHERE ac.accountId = :accountId AND ac.isActive = true")
    BigDecimal calculateTotalRemainingCreditByAccountId(@Param("accountId") String accountId);
}
