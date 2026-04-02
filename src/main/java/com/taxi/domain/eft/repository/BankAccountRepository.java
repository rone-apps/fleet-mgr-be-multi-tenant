package com.taxi.domain.eft.repository;

import com.taxi.domain.eft.model.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {
    List<BankAccount> findByDriverId(Long driverId);
    Optional<BankAccount> findByDriverIdAndIsActiveTrue(Long driverId);
    List<BankAccount> findByIsActiveTrue();
}
