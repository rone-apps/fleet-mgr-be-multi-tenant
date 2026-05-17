package com.taxi.domain.charges.service;

import com.taxi.domain.charges.dto.CustomerChargeDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Abstraction for customer charge data retrieval.
 * Supports both legacy_customer_charge and modern account_charge systems.
 *
 * This interface defines the contract for charge data that reports need,
 * regardless of underlying storage mechanism.
 */
public interface CustomerChargeDataProvider {

    /**
     * Find all customer charges for a driver in a date range.
     * @param driverId Internal driver ID (person.id)
     * @param startDate Inclusive start date
     * @param endDate Inclusive end date
     * @return List of normalized charge DTOs
     */
    List<CustomerChargeDTO> findChargesByDriverId(
        Long driverId,
        LocalDate startDate,
        LocalDate endDate
    );

    /**
     * Find all customer charges for a driver by driver number in a date range.
     * @param driverNumber Driver identifier (e.g., "D001")
     * @param startDate Inclusive start date
     * @param endDate Inclusive end date
     * @return List of normalized charge DTOs
     */
    List<CustomerChargeDTO> findChargesByDriverNumber(
        String driverNumber,
        LocalDate startDate,
        LocalDate endDate
    );

    /**
     * Calculate total charge revenue for a driver in a date range.
     * @param driverId Internal driver ID
     * @param startDate Inclusive start date
     * @param endDate Inclusive end date
     * @return Total amount (fare + tips)
     */
    BigDecimal calculateTotalCharges(
        Long driverId,
        LocalDate startDate,
        LocalDate endDate
    );

    /**
     * Identify which implementation is being used (for logging/debugging).
     * @return "LEGACY" or "MODERN"
     */
    String getImplementationType();
}
