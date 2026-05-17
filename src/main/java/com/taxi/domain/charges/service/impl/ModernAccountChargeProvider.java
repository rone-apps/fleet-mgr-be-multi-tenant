package com.taxi.domain.charges.service.impl;

import com.taxi.domain.account.model.AccountCharge;
import com.taxi.domain.account.repository.AccountChargeRepository;
import com.taxi.domain.charges.dto.CustomerChargeDTO;
import com.taxi.domain.charges.service.CustomerChargeDataProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Modern implementation: uses dedicated AccountCharge entities.
 * Provides full customer master data and billing capabilities.
 */
@Service("modernAccountChargeProvider")
@RequiredArgsConstructor
@Slf4j
public class ModernAccountChargeProvider implements CustomerChargeDataProvider {

    private final AccountChargeRepository accountChargeRepository;

    @Override
    public List<CustomerChargeDTO> findChargesByDriverId(Long driverId, LocalDate startDate, LocalDate endDate) {
        log.info("[MODERN CHARGE PROVIDER] Finding charges for driver ID {} from {} to {}",
                 driverId, startDate, endDate);

        List<AccountCharge> accountCharges = accountChargeRepository
            .findByDriverIdAndDateRange(driverId, startDate, endDate);

        log.debug("[MODERN] Found {} account charges", accountCharges.size());

        return accountCharges.stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }

    @Override
    public List<CustomerChargeDTO> findChargesByDriverNumber(String driverNumber, LocalDate startDate, LocalDate endDate) {
        log.info("[MODERN CHARGE PROVIDER] Finding charges for driver {} from {} to {}",
                 driverNumber, startDate, endDate);

        List<AccountCharge> accountCharges = accountChargeRepository
            .findByDriverNumberAndDateRange(driverNumber, startDate, endDate);

        return accountCharges.stream()
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
        return "MODERN";
    }

    /**
     * Map AccountCharge entity to normalized CustomerChargeDTO
     */
    private CustomerChargeDTO mapToDTO(AccountCharge charge) {
        BigDecimal fareAmount = charge.getFareAmount() != null ? charge.getFareAmount() : BigDecimal.ZERO;
        BigDecimal tipAmount = charge.getTipAmount() != null ? charge.getTipAmount() : BigDecimal.ZERO;

        return CustomerChargeDTO.builder()
            .id(charge.getId())
            .chargeDate(charge.getTripDate())
            .startTime(charge.getStartTime())
            .endTime(charge.getEndTime())
            .customerName(charge.getAccountCustomer() != null ? charge.getAccountCustomer().getCompanyName() : null)
            .accountId(charge.getAccountId())
            .jobCode(charge.getJobCode())
            .pickupAddress(charge.getPickupAddress())
            .dropoffAddress(charge.getDropoffAddress())
            .passengerName(charge.getPassengerName())
            .cabNumber(charge.getCab() != null ? charge.getCab().getCabNumber() : null)
            .driverNumber(charge.getDriver() != null ? charge.getDriver().getDriverNumber() : null)
            .driverName(charge.getDriver() != null ? charge.getDriver().getFullName() : null)
            .fareAmount(fareAmount)
            .tipAmount(tipAmount)
            .totalAmount(fareAmount.add(tipAmount))
            .isPaid(charge.isPaid())
            .paidDate(charge.getPaidDate())
            .invoiceNumber(charge.getInvoiceNumber())
            .sourceSystem("MODERN")
            .notes(charge.getNotes())
            .build();
    }
}
