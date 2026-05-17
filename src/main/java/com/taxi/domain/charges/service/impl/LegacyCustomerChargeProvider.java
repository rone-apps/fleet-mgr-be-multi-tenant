package com.taxi.domain.charges.service.impl;

import com.taxi.domain.charges.dto.CustomerChargeDTO;
import com.taxi.domain.charges.model.LegacyCustomerCharge;
import com.taxi.domain.charges.repository.LegacyCustomerChargeRepository;
import com.taxi.domain.charges.service.CustomerChargeDataProvider;
import com.taxi.domain.driver.model.Driver;
import com.taxi.domain.driver.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Legacy implementation: extracts charge data from legacy_customer_charge table.
 * Provides compatibility for tenants transitioning to modern system.
 * Maps current driver IDs to legacy charges via driver_number (stable business key).
 */
@Service("legacyCustomerChargeProvider")
@RequiredArgsConstructor
@Slf4j
public class LegacyCustomerChargeProvider implements CustomerChargeDataProvider {

    private final LegacyCustomerChargeRepository legacyCustomerChargeRepository;
    private final DriverRepository driverRepository;

    @Override
    public List<CustomerChargeDTO> findChargesByDriverId(Long driverId, LocalDate startDate, LocalDate endDate) {
        log.info("[LEGACY CHARGE PROVIDER] Finding charges for driver ID {} from {} to {}",
                 driverId, startDate, endDate);

        // Get current driver to find their driver_number (stable business key)
        Driver currentDriver = driverRepository.findById(driverId)
            .orElseThrow(() -> new RuntimeException("Driver not found for ID: " + driverId));

        String driverNumber = currentDriver.getDriverNumber();
        log.debug("[LEGACY] Mapping driver ID {} to driver_number {}", driverId, driverNumber);

        // Query legacy charges using driver_number
        List<LegacyCustomerCharge> legacyCharges = legacyCustomerChargeRepository
            .findByDriverNumberAndDateRange(driverNumber, startDate, endDate);

        log.info("[LEGACY] Found {} legacy customer charges for driver {}", legacyCharges.size(), driverNumber);

        return legacyCharges.stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }

    @Override
    public List<CustomerChargeDTO> findChargesByDriverNumber(String driverNumber, LocalDate startDate, LocalDate endDate) {
        log.info("[LEGACY CHARGE PROVIDER] Finding charges for driver_number {} from {} to {}",
                 driverNumber, startDate, endDate);

        List<LegacyCustomerCharge> legacyCharges = legacyCustomerChargeRepository
            .findByDriverNumberAndDateRange(driverNumber, startDate, endDate);

        log.info("[LEGACY] Found {} legacy customer charges for driver_number {}",
                 legacyCharges.size(), driverNumber);

        return legacyCharges.stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }

    @Override
    public BigDecimal calculateTotalCharges(Long driverId, LocalDate startDate, LocalDate endDate) {
        return findChargesByDriverId(driverId, startDate, endDate).stream()
            .map(CustomerChargeDTO::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public String getImplementationType() {
        return "LEGACY";
    }

    /**
     * Map LegacyCustomerCharge entity to normalized CustomerChargeDTO.
     * Driver name is looked up from current Driver table (not legacy_driver).
     */
    private CustomerChargeDTO mapToDTO(LegacyCustomerCharge legacy) {
        BigDecimal amount = BigDecimal.valueOf(legacy.getAmount() != null ? legacy.getAmount() : 0.0);
        BigDecimal payment = BigDecimal.valueOf(legacy.getPayment() != null ? legacy.getPayment() : 0.0);

        // Get driver name from current Driver table via driver_number
        String driverNumber = legacy.getDriver() != null ? legacy.getDriver().getDriverNumber() : null;
        String driverName = null;
        if (driverNumber != null) {
            driverName = driverRepository.findByDriverNumber(driverNumber)
                .map(Driver::getFullName)
                .orElse(null);
        }

        return CustomerChargeDTO.builder()
            .id(legacy.getId())
            .chargeDate(legacy.getDate())
            .startTime(null)  // Not tracked in legacy
            .endTime(null)  // Not tracked in legacy
            .customerName(legacy.getCustomer() != null ? legacy.getCustomer().getName() : null)
            .accountId(legacy.getCustomer() != null ? legacy.getCustomer().getCustomerId() : null)
            .jobCode(null)  // Not tracked in legacy
            .pickupAddress(null)  // Not tracked in legacy
            .dropoffAddress(null)  // Not tracked in legacy
            .passengerName(null)  // Not tracked in legacy
            .cabNumber(null)  // Cab info not available in legacy system
            .driverNumber(driverNumber)
            .driverName(driverName)  // From current Driver table
            .fareAmount(amount)
            .tipAmount(BigDecimal.ZERO)  // Legacy system doesn't separate tips
            .totalAmount(amount)
            .isPaid(payment.compareTo(BigDecimal.ZERO) > 0)  // Has payment = paid
            .paidDate(null)  // Not tracked in legacy
            .invoiceNumber(null)  // Not tracked in legacy
            .sourceSystem("LEGACY")
            .notes(legacy.getNotes())
            .build();
    }
}
