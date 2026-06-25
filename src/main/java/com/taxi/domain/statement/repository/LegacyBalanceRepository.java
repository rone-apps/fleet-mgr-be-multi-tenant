package com.taxi.domain.statement.repository;

import com.taxi.domain.statement.model.LegacyBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing manually imported legacy balance data
 */
@Repository
public interface LegacyBalanceRepository extends JpaRepository<LegacyBalance, Long> {

    /**
     * Find the most recent legacy balance for a driver before or on a given date.
     * Used when generating statements to get the previous balance.
     *
     * @param driverNumber Driver business identifier
     * @param beforeDate Only consider balances effective before or on this date
     * @return Most recent balance record, if any
     */
    @Query("SELECT lb FROM LegacyBalance lb WHERE lb.driverNumber = :driverNumber " +
           "AND lb.effectiveDate <= :beforeDate ORDER BY lb.effectiveDate DESC LIMIT 1")
    Optional<LegacyBalance> findLatestByDriverNumberBefore(
        @Param("driverNumber") String driverNumber,
        @Param("beforeDate") LocalDate beforeDate
    );

    /**
     * Find all balances for a driver, ordered by most recent first
     */
    List<LegacyBalance> findByDriverNumberOrderByEffectiveDateDesc(String driverNumber);

    /**
     * Check if any legacy balance exists for a driver
     */
    boolean existsByDriverNumber(String driverNumber);

    /**
     * Find balance for specific driver and date
     */
    Optional<LegacyBalance> findByDriverNumberAndEffectiveDate(String driverNumber, LocalDate effectiveDate);

    /**
     * Delete all legacy balances (used for re-import)
     */
    @Override
    @Modifying
    void deleteAll();
}
