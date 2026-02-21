package com.taxi.domain.account.service;

import com.taxi.domain.account.model.AccountCharge;
import com.taxi.domain.account.model.AccountCustomer;
import com.taxi.domain.account.model.Invoice;
import com.taxi.domain.account.dto.BulkUpdateTipRequest;
import com.taxi.domain.account.repository.AccountChargeRepository;
import com.taxi.domain.account.repository.AccountCustomerRepository;
import com.taxi.domain.account.repository.InvoiceRepository;
import com.taxi.domain.cab.model.Cab;
import com.taxi.domain.cab.repository.CabRepository;
import com.taxi.domain.driver.model.Driver;
import com.taxi.domain.driver.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AccountChargeService {

    private final AccountChargeRepository accountChargeRepository;
    private final AccountCustomerRepository accountCustomerRepository;
    private final InvoiceRepository invoiceRepository;
    private final CabRepository cabRepository;
    private final DriverRepository driverRepository;

    // Create new charge
    public AccountCharge createCharge(AccountCharge charge) {
        // Validate customer exists
        if (charge.getAccountCustomer() == null || charge.getAccountCustomer().getId() == null) {
            throw new IllegalArgumentException("Customer is required for charge");
        }
        
        AccountCustomer customer = accountCustomerRepository.findById(charge.getAccountCustomer().getId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
        
        // Set the account_id from the customer
        charge.setAccountId(customer.getAccountId());
        charge.setAccountCustomer(customer);
        
        // Fetch full Cab entity if cab is provided
        if (charge.getCab() != null && charge.getCab().getId() != null) {
            Cab cab = cabRepository.findById(charge.getCab().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Cab not found with id: " + charge.getCab().getId()));
            charge.setCab(cab);
        }
        
        // Fetch full Driver entity if driver is provided
        if (charge.getDriver() != null && charge.getDriver().getId() != null) {
            Driver driver = driverRepository.findById(charge.getDriver().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Driver not found with id: " + charge.getDriver().getId()));
            charge.setDriver(driver);
        }
        
        return accountChargeRepository.save(charge);
    }

    // Update existing charge
    // Fix for AccountChargeService.java - updateCharge method
// Replace the existing updateCharge method with this:

@Transactional
public AccountCharge updateCharge(Long id, AccountCharge updatedCharge) {
    AccountCharge existingCharge = accountChargeRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Charge not found with id: " + id));
    
    // Update fields that can be changed
    existingCharge.setJobCode(updatedCharge.getJobCode());
    existingCharge.setPassengerName(updatedCharge.getPassengerName());
    existingCharge.setPickupAddress(updatedCharge.getPickupAddress());
    existingCharge.setDropoffAddress(updatedCharge.getDropoffAddress());
    existingCharge.setFareAmount(updatedCharge.getFareAmount());
    existingCharge.setTipAmount(updatedCharge.getTipAmount());
    existingCharge.setTripDate(updatedCharge.getTripDate());
    
    // Handle accountCustomer - preserve existing if not provided in update
    if (updatedCharge.getAccountCustomer() != null) {
        // If customer ID is provided, fetch and set the full customer object
        if (updatedCharge.getAccountCustomer().getId() != null) {
            AccountCustomer customer = accountCustomerRepository.findById(
                updatedCharge.getAccountCustomer().getId()
            ).orElseThrow(() -> new IllegalArgumentException(
                "Customer not found with id: " + updatedCharge.getAccountCustomer().getId()
            ));
            existingCharge.setAccountCustomer(customer);
        }
    }
    // If accountCustomer is null in update, preserve existing customer
    
    // Handle cab - preserve existing if not provided
    if (updatedCharge.getCab() != null && updatedCharge.getCab().getId() != null) {
        Cab cab = cabRepository.findById(updatedCharge.getCab().getId())
                .orElseThrow(() -> new IllegalArgumentException(
                    "Cab not found with id: " + updatedCharge.getCab().getId()
                ));
        existingCharge.setCab(cab);
    }
    
    // Handle driver - preserve existing if not provided
    if (updatedCharge.getDriver() != null && updatedCharge.getDriver().getId() != null) {
        Driver driver = driverRepository.findById(updatedCharge.getDriver().getId())
                .orElseThrow(() -> new IllegalArgumentException(
                    "Driver not found with id: " + updatedCharge.getDriver().getId()
                ));
        existingCharge.setDriver(driver);
    }
    
    // Update account ID fields if provided
    if (updatedCharge.getAccountId() != null) {
        existingCharge.setAccountId(updatedCharge.getAccountId());
    }
    if (updatedCharge.getSubAccount() != null) {
        existingCharge.setSubAccount(updatedCharge.getSubAccount());
    }
    
    // Payment fields - only update if explicitly set
    if (updatedCharge.isPaid() != existingCharge.isPaid()) {
        existingCharge.setPaid(updatedCharge.isPaid());
    }
    if (updatedCharge.getInvoiceNumber() != null) {
        existingCharge.setInvoiceNumber(updatedCharge.getInvoiceNumber());
    }
    if (updatedCharge.getPaidDate() != null) {
        existingCharge.setPaidDate(updatedCharge.getPaidDate());
    }
    
    return accountChargeRepository.save(existingCharge);
}
    // Get charge by ID
    public AccountCharge getChargeById(Long id) {
        return accountChargeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Charge not found with id: " + id));
    }

    public List<AccountCharge> bulkUpdateTips(List<BulkUpdateTipRequest> updates) {
        if (updates == null || updates.isEmpty()) {
            throw new IllegalArgumentException("No charges provided for bulk update");
        }

        List<Long> ids = updates.stream()
                .map(BulkUpdateTipRequest::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (ids.size() != updates.size()) {
            throw new IllegalArgumentException("All charges must include a unique id");
        }

        List<AccountCharge> charges = accountChargeRepository.findAllById(ids);
        if (charges.size() != ids.size()) {
            throw new IllegalArgumentException("One or more charges were not found");
        }

        Map<Long, AccountCharge> byId = charges.stream()
                .collect(Collectors.toMap(AccountCharge::getId, c -> c));

        for (BulkUpdateTipRequest req : updates) {
            AccountCharge charge = byId.get(req.getId());
            if (charge == null) {
                throw new IllegalArgumentException("Charge not found with id: " + req.getId());
            }

            BigDecimal tip = req.getTipAmount() != null ? req.getTipAmount() : BigDecimal.ZERO;
            charge.setTipAmount(tip);
        }

        return accountChargeRepository.saveAll(charges);
    }

    // Get all charges
    public List<AccountCharge> getAllCharges() {
        return accountChargeRepository.findAll();
    }

    // Get charges by account_id
    public List<AccountCharge> getChargesByAccountId(String accountId) {
        return accountChargeRepository.findByAccountId(accountId);
    }

    // Get unpaid charges by account_id
    public List<AccountCharge> getUnpaidChargesByAccountId(String accountId) {
        return accountChargeRepository.findByAccountIdAndPaidFalse(accountId);
    }

    // Get charges by account_id and date range
    public List<AccountCharge> getChargesByAccountIdAndDateRange(
            String accountId, LocalDate startDate, LocalDate endDate) {
        return accountChargeRepository.findByAccountIdAndTripDateBetween(accountId, startDate, endDate);
    }

    // Get charges by customer
    public List<AccountCharge> getChargesByCustomer(Long customerId) {
        return accountChargeRepository.findByAccountCustomerId(customerId);
    }

    // Get unpaid charges by customer
    public List<AccountCharge> getUnpaidChargesByCustomer(Long customerId) {
        return accountChargeRepository.findByAccountCustomerIdAndPaidFalse(customerId);
    }

    // Get charges by customer and date range
    public List<AccountCharge> getChargesByCustomerAndDateRange(
            Long customerId, LocalDate startDate, LocalDate endDate) {
        return accountChargeRepository.findByAccountCustomerIdAndTripDateBetween(
                customerId, startDate, endDate);
    }

    // Get unpaid charges by customer and date range
    public List<AccountCharge> getUnpaidChargesByCustomerAndDateRange(
            Long customerId, LocalDate startDate, LocalDate endDate) {
        return accountChargeRepository.findByAccountCustomerIdAndPaidFalseAndTripDateBetween(
                customerId, startDate, endDate);
    }

    // Get charges by date range
    public List<AccountCharge> getChargesByDateRange(LocalDate startDate, LocalDate endDate) {
        return accountChargeRepository.findByTripDateBetween(startDate, endDate);
    }

    // Get charges by job code
    public List<AccountCharge> getChargesByJobCode(String jobCode) {
        return accountChargeRepository.findByJobCodeIgnoreCase(jobCode);
    }

    // Get overdue charges (unpaid and older than 30 days)
    public List<AccountCharge> getOverdueCharges() {
        LocalDate cutoffDate = LocalDate.now().minusDays(30);
        return accountChargeRepository.findOverdueCharges(cutoffDate);
    }

    // Get charges by driver
    public List<AccountCharge> getChargesByDriver(Long driverId) {
        return accountChargeRepository.findByDriverId(driverId);
    }

    // Get charges by cab
    public List<AccountCharge> getChargesByCab(Long cabId) {
        return accountChargeRepository.findByCabId(cabId);
    }

    // Helper: Recalculate invoice totals based on paid charges
    private void updateInvoiceTotalsFromCharges(Long invoiceId) {
        if (invoiceId == null) return;

        try {
            Invoice invoice = invoiceRepository.findById(invoiceId).orElse(null);
            if (invoice == null) {
                log.warn("Invoice not found: {}", invoiceId);
                return;
            }

            // Get all charges for this invoice
            List<AccountCharge> allCharges = accountChargeRepository.findByInvoiceId(invoiceId);

            // Calculate total paid from all paid charges
            BigDecimal totalPaid = allCharges.stream()
                    .filter(AccountCharge::isPaid)
                    .map(AccountCharge::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Update invoice with new amounts
            invoice.setAmountPaid(totalPaid);
            invoice.setBalanceDue(invoice.getTotalAmount().subtract(totalPaid));

            // Update status based on balance due
            invoice.updateStatus();

            invoiceRepository.save(invoice);

            log.info("Updated invoice {} totals: amountPaid={}, balanceDue={}, status={}",
                    invoiceId, totalPaid, invoice.getBalanceDue(), invoice.getStatus());

        } catch (Exception e) {
            log.error("Error updating invoice totals for invoice: {}", invoiceId, e);
        }
    }

    // Mark charge as paid
    @Transactional
    public AccountCharge markChargeAsPaid(Long id, String invoiceNumber) {
        AccountCharge charge = getChargeById(id);
        Long invoiceId = charge.getInvoiceId();
        charge.markAsPaid(invoiceNumber);
        AccountCharge saved = accountChargeRepository.save(charge);
        log.info("Marked account charge {} as paid with invoice number: {}", id, invoiceNumber);

        // Recalculate invoice totals
        updateInvoiceTotalsFromCharges(invoiceId);

        return saved;
    }

    // Mark charge as unpaid
    @Transactional
    public AccountCharge markChargeAsUnpaid(Long id) {
        AccountCharge charge = getChargeById(id);
        Long invoiceId = charge.getInvoiceId();
        charge.markAsUnpaid();
        AccountCharge saved = accountChargeRepository.save(charge);
        log.info("Marked account charge {} as unpaid", id);

        // Recalculate invoice totals
        updateInvoiceTotalsFromCharges(invoiceId);

        return saved;
    }

    // Mark multiple charges as paid
    @Transactional
    public List<AccountCharge> markChargesAsPaid(List<Long> chargeIds, String invoiceNumber) {
        List<AccountCharge> charges = chargeIds.stream()
                .map(this::getChargeById)
                .collect(Collectors.toList());

        // Get invoice IDs to update later
        java.util.Set<Long> invoiceIds = charges.stream()
                .map(AccountCharge::getInvoiceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        charges.forEach(charge -> charge.markAsPaid(invoiceNumber));
        List<AccountCharge> saved = accountChargeRepository.saveAll(charges);
        log.info("Marked {} account charges as paid with invoice number: {}", chargeIds.size(), invoiceNumber);

        // Recalculate invoice totals for all affected invoices
        invoiceIds.forEach(this::updateInvoiceTotalsFromCharges);

        return saved;
    }

    // Calculate unpaid total for customer
    public BigDecimal calculateUnpaidTotalForCustomer(Long customerId) {
        return accountChargeRepository.calculateUnpaidTotal(customerId);
    }

    // Calculate total for period
    public BigDecimal calculateTotalForPeriod(Long customerId, LocalDate startDate, LocalDate endDate) {
        return accountChargeRepository.calculateTotalForPeriod(customerId, startDate, endDate);
    }

    // Generate billing summary for customer
    public Map<String, Object> generateBillingSummary(Long customerId, LocalDate startDate, LocalDate endDate) {
        List<AccountCharge> charges = getChargesByCustomerAndDateRange(customerId, startDate, endDate);
        
        BigDecimal totalAmount = charges.stream()
                .map(AccountCharge::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal unpaidAmount = charges.stream()
                .filter(c -> !c.isPaid())
                .map(AccountCharge::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        long totalCount = charges.size();
        long unpaidCount = charges.stream().filter(c -> !c.isPaid()).count();
        
        return Map.of(
                "customerId", customerId,
                "startDate", startDate,
                "endDate", endDate,
                "charges", charges,
                "totalAmount", totalAmount,
                "unpaidAmount", unpaidAmount,
                "totalCount", totalCount,
                "unpaidCount", unpaidCount
        );
    }

    // Get charges grouped by customer for billing
    public Map<AccountCustomer, List<AccountCharge>> getChargesGroupedByCustomer(
            LocalDate startDate, LocalDate endDate) {
        List<AccountCharge> charges = accountChargeRepository.findChargesGroupedByCustomerForBilling(
                startDate, endDate);
        
        return charges.stream()
                .collect(Collectors.groupingBy(AccountCharge::getAccountCustomer));
    }


/**
 * Get all charges with pagination
 */
public Page<AccountCharge> getAllCharges(Pageable pageable) {
    return accountChargeRepository.findAllBy(pageable);
}

public Page<AccountCharge> getAllCharges(Pageable pageable, String customerName) {
    if (customerName != null && !customerName.trim().isEmpty()) {
        return accountChargeRepository.findByAccountCustomerCompanyNameContainingIgnoreCase(customerName.trim(), pageable);
    }
    return getAllCharges(pageable);
}

public Page<AccountCharge> getAllCharges(Pageable pageable, String customerName, Long cabId, Long driverId) {
    return getAllCharges(pageable, customerName, cabId, driverId, null, null);
}

public Page<AccountCharge> getAllCharges(Pageable pageable, String customerName, Long cabId, Long driverId, LocalDate startDate, LocalDate endDate) {
    String normalizedCustomerName = (customerName != null && !customerName.trim().isEmpty()) ? customerName.trim() : null;
    return accountChargeRepository.searchCharges(normalizedCustomerName, cabId, driverId, startDate, endDate, pageable);
}

/**
 * Get charges by customer with pagination
 */
public Page<AccountCharge> getChargesByCustomer(Long customerId, Pageable pageable) {
    return accountChargeRepository.findByAccountCustomerId(customerId, pageable);
}

/**
 * Get summary statistics for charges with filters
 */
public Map<String, Object> getChargesSummary(String customerName, Long cabId, Long driverId, LocalDate startDate, LocalDate endDate) {
    String normalizedCustomerName = (customerName != null && !customerName.trim().isEmpty()) ? customerName.trim() : null;
    Map<String, Object> stats = accountChargeRepository.getSummaryStatistics(normalizedCustomerName, cabId, driverId, startDate, endDate);

    // Ensure all fields have proper types and defaults
    Map<String, Object> result = new java.util.HashMap<>();
    result.put("outstandingBalance", stats.getOrDefault("outstandingBalance", BigDecimal.ZERO));

    // Safely handle null values from database query
    Object unpaidCountObj = stats.getOrDefault("unpaidChargesCount", 0L);
    result.put("unpaidChargesCount", unpaidCountObj != null && unpaidCountObj instanceof Number ? ((Number) unpaidCountObj).longValue() : 0L);

    Object totalCountObj = stats.getOrDefault("totalChargesCount", 0L);
    result.put("totalChargesCount", totalCountObj != null && totalCountObj instanceof Number ? ((Number) totalCountObj).longValue() : 0L);

    return result;
}

/**
 * Get fare and tip totals for charges with filters
 */
public Map<String, Object> getChargesTotals(String customerName, Long cabId, Long driverId, LocalDate startDate, LocalDate endDate) {
    List<AccountCharge> charges = accountChargeRepository.searchChargesNoPaging(
            (customerName != null && !customerName.trim().isEmpty()) ? customerName.trim() : null,
            cabId, driverId, startDate, endDate);

    // Calculate totals for all charges
    BigDecimal totalFareAmount = charges.stream()
            .map(AccountCharge::getFareAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal totalTipAmount = charges.stream()
            .map(AccountCharge::getTipAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    // Calculate totals for unpaid charges only
    BigDecimal unpaidFareAmount = charges.stream()
            .filter(c -> !c.isPaid())
            .map(AccountCharge::getFareAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal unpaidTipAmount = charges.stream()
            .filter(c -> !c.isPaid())
            .map(AccountCharge::getTipAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal totalAmount = totalFareAmount.add(totalTipAmount);
    BigDecimal unpaidAmount = unpaidFareAmount.add(unpaidTipAmount);
    BigDecimal paidAmount = totalAmount.subtract(unpaidAmount);

    long totalCount = charges.size();
    long unpaidCount = charges.stream().filter(c -> !c.isPaid()).count();
    long paidCount = totalCount - unpaidCount;

    Map<String, Object> result = new java.util.HashMap<>();
    result.put("totalFareAmount", totalFareAmount);
    result.put("totalTipAmount", totalTipAmount);
    result.put("totalAmount", totalAmount);
    result.put("unpaidFareAmount", unpaidFareAmount);
    result.put("unpaidTipAmount", unpaidTipAmount);
    result.put("unpaidAmount", unpaidAmount);
    result.put("paidFareAmount", totalFareAmount.subtract(unpaidFareAmount));
    result.put("paidTipAmount", totalTipAmount.subtract(unpaidTipAmount));
    result.put("paidAmount", paidAmount);
    result.put("chargeCount", totalCount);
    result.put("unpaidCount", unpaidCount);
    result.put("paidCount", paidCount);

    return result;
}

    // Charges cannot be deleted - only updated
    // No delete methods provided
}