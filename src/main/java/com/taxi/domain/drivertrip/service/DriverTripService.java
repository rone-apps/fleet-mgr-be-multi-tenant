package com.taxi.domain.drivertrip.service;

import com.taxi.domain.account.model.AccountCharge;
import com.taxi.domain.account.model.AccountCustomer;
import com.taxi.domain.account.repository.AccountChargeRepository;
import com.taxi.domain.account.repository.AccountCustomerRepository;
import com.taxi.domain.drivertrip.dto.DriverTripDTO;
import com.taxi.domain.drivertrip.model.DriverTrip;
import com.taxi.domain.drivertrip.repository.DriverTripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverTripService {

    private final DriverTripRepository driverTripRepository;
    private final AccountCustomerRepository accountCustomerRepository;
    private final AccountChargeRepository accountChargeRepository;

    /**
     * Search driver trips with filters and pagination
     */
    public Page<DriverTrip> searchTrips(String driverUsername, Long cabId,
                                         LocalDate startDate, LocalDate endDate,
                                         String driverName, Pageable pageable) {
        return driverTripRepository.searchTrips(driverUsername, cabId, startDate, endDate, driverName, pageable);
    }

    /**
     * Update the account number on a driver trip and create/update the corresponding account charge.
     * If the account number is cleared, any existing charge for this trip is removed.
     */
    @Transactional
    public DriverTripDTO updateAccountNumber(Long tripId, String accountNumber) {
        DriverTrip trip = driverTripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Driver trip not found with id: " + tripId));

        String oldAccountNumber = trip.getAccountNumber();
        trip.setAccountNumber(accountNumber);
        driverTripRepository.save(trip);
        log.info("Updated account number to '{}' for driver trip id: {}", accountNumber, tripId);

        // If account number is set, create or update account charge
        if (accountNumber != null && !accountNumber.trim().isEmpty()) {
            List<AccountCustomer> customers = accountCustomerRepository.findByAccountId(accountNumber.trim());
            if (customers.isEmpty()) {
                throw new RuntimeException("No account customer found for account number: " + accountNumber +
                        ". Please create the customer first or verify the account number.");
            }
            AccountCustomer customer = customers.get(0);

            // Check if a charge already exists for this trip
            Optional<AccountCharge> existing = accountChargeRepository.findByUniqueConstraint(
                    accountNumber,
                    trip.getCab() != null ? trip.getCab().getId() : null,
                    trip.getDriver() != null ? trip.getDriver().getId() : null,
                    trip.getTripDate(),
                    trip.getStartTime(),
                    trip.getJobCode()
            );

            if (existing.isEmpty()) {
                // Also remove any charge linked to the old account number for this trip
                if (oldAccountNumber != null && !oldAccountNumber.trim().isEmpty() && !oldAccountNumber.equals(accountNumber)) {
                    accountChargeRepository.findByUniqueConstraint(
                            oldAccountNumber,
                            trip.getCab() != null ? trip.getCab().getId() : null,
                            trip.getDriver() != null ? trip.getDriver().getId() : null,
                            trip.getTripDate(),
                            trip.getStartTime(),
                            trip.getJobCode()
                    ).ifPresent(oldCharge -> {
                        accountChargeRepository.delete(oldCharge);
                        log.info("Removed old account charge for trip {} (old account: {})", tripId, oldAccountNumber);
                    });
                }

                AccountCharge charge = AccountCharge.builder()
                        .accountId(accountNumber)
                        .accountCustomer(customer)
                        .jobCode(trip.getJobCode())
                        .tripDate(trip.getTripDate())
                        .startTime(trip.getStartTime())
                        .endTime(trip.getEndTime())
                        .pickupAddress(trip.getPickupAddress())
                        .dropoffAddress(trip.getDropoffAddress())
                        .passengerName(trip.getPassengerName())
                        .cab(trip.getCab())
                        .driver(trip.getDriver())
                        .fareAmount(trip.getFareAmount())
                        .tipAmount(trip.getTipAmount())
                        .paid(false)
                        .notes("Converted from driver trip #" + tripId)
                        .build();

                accountChargeRepository.save(charge);
                log.info("Created account charge for driver trip {} under account '{}'", tripId, accountNumber);
            } else {
                log.info("Account charge already exists for driver trip {} under account '{}', skipping", tripId, accountNumber);
            }
        }

        return DriverTripDTO.fromEntity(trip);
    }

    /**
     * Convert a driver trip to an account charge.
     * The trip must have an accountNumber set that matches an existing AccountCustomer.
     */
    @Transactional
    public void convertToAccountCharge(Long tripId) {
        DriverTrip trip = driverTripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Driver trip not found with id: " + tripId));

        String accountNumber = trip.getAccountNumber();
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new RuntimeException("Cannot convert to account charge: no account number set on this trip");
        }

        // Look up customer by account number
        List<AccountCustomer> customers = accountCustomerRepository.findByAccountId(accountNumber.trim());
        if (customers.isEmpty()) {
            throw new RuntimeException("No account customer found for account number: " + accountNumber +
                    ". Please create the customer first or verify the account number.");
        }
        AccountCustomer customer = customers.get(0);

        // Check for duplicate charge
        Optional<AccountCharge> existing = accountChargeRepository.findByUniqueConstraint(
                accountNumber,
                trip.getCab() != null ? trip.getCab().getId() : null,
                trip.getDriver() != null ? trip.getDriver().getId() : null,
                trip.getTripDate(),
                trip.getStartTime(),
                trip.getJobCode()
        );

        if (existing.isPresent()) {
            throw new RuntimeException("An account charge already exists for this trip (Job Code: " +
                    trip.getJobCode() + ", Date: " + trip.getTripDate() + ")");
        }

        // Build AccountCharge from DriverTrip
        AccountCharge charge = AccountCharge.builder()
                .accountId(accountNumber)
                .accountCustomer(customer)
                .jobCode(trip.getJobCode())
                .tripDate(trip.getTripDate())
                .startTime(trip.getStartTime())
                .endTime(trip.getEndTime())
                .pickupAddress(trip.getPickupAddress())
                .dropoffAddress(trip.getDropoffAddress())
                .passengerName(trip.getPassengerName())
                .cab(trip.getCab())
                .driver(trip.getDriver())
                .fareAmount(trip.getFareAmount())
                .tipAmount(trip.getTipAmount())
                .paid(false)
                .notes("Converted from driver trip #" + tripId)
                .build();

        accountChargeRepository.save(charge);
        log.info("Converted driver trip {} to account charge for account '{}'", tripId, accountNumber);
    }
}
