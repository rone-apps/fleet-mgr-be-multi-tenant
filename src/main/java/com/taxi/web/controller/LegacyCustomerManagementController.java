package com.taxi.web.controller;

import com.taxi.domain.charges.model.LegacyAccountCustomer;
import com.taxi.domain.charges.model.LegacyCustomerCharge;
import com.taxi.domain.charges.repository.LegacyAccountCustomerRepository;
import com.taxi.domain.charges.repository.LegacyCustomerChargeRepository;
import com.taxi.domain.driver.repository.DriverRepository;
import com.taxi.web.dto.LegacyCustomerChargeDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for managing legacy customer accounts and charges.
 * Provides read-only access to legacy data for migration purposes.
 */
@RestController
@RequestMapping("/legacy-customers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
@Slf4j
public class LegacyCustomerManagementController {

    private final LegacyAccountCustomerRepository legacyAccountCustomerRepository;
    private final LegacyCustomerChargeRepository legacyCustomerChargeRepository;
    private final DriverRepository driverRepository;

    /**
     * Get all legacy customers
     */
    @GetMapping
    public ResponseEntity<List<LegacyAccountCustomer>> getAllLegacyCustomers() {
        log.info("Fetching all legacy customers");
        List<LegacyAccountCustomer> customers = legacyAccountCustomerRepository.findAll();
        log.info("Found {} legacy customers", customers.size());
        return ResponseEntity.ok(customers);
    }

    /**
     * Get all active drivers (for autocomplete)
     */
    @GetMapping("/drivers")
    public ResponseEntity<List<DriverOption>> getAllDrivers() {
        log.info("Fetching all active drivers");
        List<DriverOption> drivers = driverRepository.findAllActiveDrivers().stream()
                .map(driver -> new DriverOption(
                        driver.getDriverNumber(),
                        driver.getFirstName() + " " + driver.getLastName()
                ))
                .sorted((a, b) -> a.driverNumber.compareTo(b.driverNumber))
                .collect(Collectors.toList());
        log.info("Found {} active drivers", drivers.size());
        return ResponseEntity.ok(drivers);
    }

    /**
     * Get unique customers from legacy charges (for autocomplete)
     */
    @GetMapping("/customers-with-charges")
    public ResponseEntity<List<CustomerOption>> getCustomersWithCharges() {
        log.info("Fetching customers with legacy charges");
        List<CustomerOption> customers = legacyCustomerChargeRepository.findAll().stream()
                .filter(charge -> charge.getCustomer() != null)
                .map(charge -> new CustomerOption(
                        charge.getCustomer().getDbId(),
                        charge.getCustomer().getCustomerId(),
                        charge.getCustomer().getName()
                ))
                .distinct()
                .sorted((a, b) -> a.name.compareTo(b.name))
                .collect(Collectors.toList());
        log.info("Found {} unique customers", customers.size());
        return ResponseEntity.ok(customers);
    }

    /**
     * DTO for driver autocomplete options
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DriverOption {
        private String driverNumber;
        private String driverName;
    }

    /**
     * DTO for customer autocomplete options
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CustomerOption {
        private Long dbId;
        private String customerId;
        private String name;
    }

    /**
     * Get legacy customer by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<LegacyAccountCustomer> getLegacyCustomerById(@PathVariable Long id) {
        log.info("Fetching legacy customer with ID: {}", id);
        return legacyAccountCustomerRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get legacy charges by customer ID
     */
    @GetMapping("/{customerId}/charges")
    public ResponseEntity<List<LegacyCustomerChargeDTO>> getLegacyChargesByCustomer(@PathVariable Long customerId) {
        log.info("Fetching legacy charges for customer ID: {}", customerId);
        List<LegacyCustomerCharge> charges = legacyCustomerChargeRepository
                .findByCustomerId(customerId);
        log.info("Found {} legacy charges for customer {}", charges.size(), customerId);
        return ResponseEntity.ok(charges.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList()));
    }

    /**
     * Get all legacy charges with optional filters and pagination
     * Defaults to last 30 days if no date range specified
     */
    @GetMapping("/charges/all")
    public ResponseEntity<Page<LegacyCustomerChargeDTO>> getAllLegacyCharges(
            @RequestParam(required = false) Long customerDbId,
            @RequestParam(required = false) String driverNumber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size
    ) {
        // Default to last 30 days if no date range specified
        final LocalDate effectiveStartDate;
        final LocalDate effectiveEndDate;

        if (startDate == null && endDate == null) {
            effectiveEndDate = LocalDate.now();
            effectiveStartDate = effectiveEndDate.minusDays(30);
            log.info("No date range specified, defaulting to last 30 days: {} to {}", effectiveStartDate, effectiveEndDate);
        } else {
            effectiveStartDate = startDate;
            effectiveEndDate = endDate;
        }

        log.info("Fetching legacy charges with filters - customer: {}, driver: {}, dates: {} to {}, page: {}, size: {}",
                customerDbId, driverNumber, effectiveStartDate, effectiveEndDate, page, size);

        List<LegacyCustomerCharge> charges = legacyCustomerChargeRepository.findAll();

        // Apply filters
        List<LegacyCustomerChargeDTO> filteredCharges = charges.stream()
                .filter(charge -> customerDbId == null ||
                        (charge.getCustomer() != null && customerDbId.equals(charge.getCustomer().getDbId())))
                .filter(charge -> driverNumber == null ||
                        (charge.getDriver() != null && driverNumber.equals(charge.getDriver().getDriverNumber())))
                .filter(charge -> effectiveStartDate == null ||
                        (charge.getDate() != null && !charge.getDate().isBefore(effectiveStartDate)))
                .filter(charge -> effectiveEndDate == null ||
                        (charge.getDate() != null && !charge.getDate().isAfter(effectiveEndDate)))
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        // Calculate pagination
        int total = filteredCharges.size();
        int start = page * size;
        int end = Math.min(start + size, total);

        List<LegacyCustomerChargeDTO> pageContent = start < total
                ? filteredCharges.subList(start, end)
                : List.of();

        Page<LegacyCustomerChargeDTO> pageResult = new PageImpl<>(
                pageContent,
                PageRequest.of(page, size),
                total
        );

        log.info("Returning page {} of {} (total {} charges)", page, pageResult.getTotalPages(), total);
        return ResponseEntity.ok(pageResult);
    }

    /**
     * Map entity to DTO (avoiding lazy loading issues)
     */
    private LegacyCustomerChargeDTO mapToDTO(LegacyCustomerCharge charge) {
        String driverName = null;
        if (charge.getDriver() != null && charge.getDriver().getDriverNumber() != null) {
            driverName = driverRepository.findByDriverNumber(charge.getDriver().getDriverNumber())
                    .map(d -> d.getFirstName() + " " + d.getLastName())
                    .orElse(null);
        }

        return LegacyCustomerChargeDTO.builder()
                .id(charge.getId())
                .amount(charge.getAmount())
                .date(charge.getDate())
                .payment(charge.getPayment())
                .cabId(charge.getCabId())
                .notes(charge.getNotes())
                .type(charge.getType())
                .customerDbId(charge.getCustomer() != null ? charge.getCustomer().getDbId() : null)
                .customerId(charge.getCustomer() != null ? charge.getCustomer().getCustomerId() : null)
                .customerName(charge.getCustomer() != null ? charge.getCustomer().getName() : null)
                .driverDbId(charge.getDriver() != null ? charge.getDriver().getId() : null)
                .driverNumber(charge.getDriver() != null ? charge.getDriver().getDriverNumber() : null)
                .driverName(driverName)
                .build();
    }
}
