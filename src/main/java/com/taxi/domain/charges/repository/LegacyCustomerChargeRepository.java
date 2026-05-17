package com.taxi.domain.charges.repository;

import com.taxi.domain.charges.model.LegacyCustomerCharge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LegacyCustomerChargeRepository extends JpaRepository<LegacyCustomerCharge, Long> {

    /**
     * Find charges by driver ID and date range
     */
    @Query("SELECT lcc FROM LegacyCustomerCharge lcc " +
           "WHERE lcc.driver.id = :driverId " +
           "AND lcc.date BETWEEN :startDate AND :endDate " +
           "ORDER BY lcc.date")
    List<LegacyCustomerCharge> findByDriverIdAndDateRange(
        @Param("driverId") Long driverId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find charges by driver number and date range
     */
    @Query("SELECT lcc FROM LegacyCustomerCharge lcc " +
           "WHERE lcc.driver.driverNumber = :driverNumber " +
           "AND lcc.date BETWEEN :startDate AND :endDate " +
           "ORDER BY lcc.date")
    List<LegacyCustomerCharge> findByDriverNumberAndDateRange(
        @Param("driverNumber") String driverNumber,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find charges by customer ID
     */
    @Query("SELECT lcc FROM LegacyCustomerCharge lcc " +
           "WHERE lcc.customer.id = :customerId " +
           "ORDER BY lcc.date DESC")
    List<LegacyCustomerCharge> findByCustomerId(@Param("customerId") Long customerId);
}
