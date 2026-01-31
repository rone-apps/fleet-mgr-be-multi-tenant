package com.taxi.domain.account.service;

import com.taxi.domain.account.model.AccountCustomer;
import com.taxi.domain.account.model.AccountCustomerWithBalance;
import com.taxi.domain.account.repository.AccountChargeRepository;
import com.taxi.domain.account.repository.AccountCustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountCustomerService {

    private final AccountCustomerRepository accountCustomerRepository;
    private final AccountChargeRepository accountChargeRepository;

    // Create new customer
    public AccountCustomer createCustomer(AccountCustomer customer) {
        // Validate account_id is provided
        if (customer.getAccountId() == null || customer.getAccountId().trim().isEmpty()) {
            throw new IllegalArgumentException("Account ID is required");
        }
        
        // Check if company name already exists
        if (accountCustomerRepository.existsByCompanyNameIgnoreCase(customer.getCompanyName())) {
            throw new IllegalStateException("A customer with this company name already exists");
        }
        
        return accountCustomerRepository.save(customer);
    }

    // Update existing customer
    public AccountCustomer updateCustomer(Long id, AccountCustomer customerData) {
        AccountCustomer existing = getCustomerById(id);

        // Don't allow changing account_id
        if (!existing.getAccountId().equals(customerData.getAccountId())) {
            throw new IllegalStateException("Cannot change account ID for existing customer");
        }

        // Check email uniqueness only if email is provided and different from current
        if (customerData.getEmail() != null && !customerData.getEmail().isEmpty() &&
            !customerData.getEmail().equalsIgnoreCase(existing.getEmail() != null ? existing.getEmail() : "")) {
            // Check if this email is already used by another customer
            List<AccountCustomer> emailConflicts = accountCustomerRepository.findAllByEmailIgnoreCase(customerData.getEmail());
            for (AccountCustomer conflict : emailConflicts) {
                if (!conflict.getId().equals(id)) {
                    throw new IllegalStateException("Email already in use by another customer");
                }
            }
        }
        
        // Update fields
        existing.setCompanyName(customerData.getCompanyName());
        existing.setContactPerson(customerData.getContactPerson());
        existing.setStreetAddress(customerData.getStreetAddress());
        existing.setCity(customerData.getCity());
        existing.setProvince(customerData.getProvince());
        existing.setPostalCode(customerData.getPostalCode());
        existing.setCountry(customerData.getCountry());
        existing.setPhoneNumber(customerData.getPhoneNumber());
        existing.setEmail(customerData.getEmail());
        existing.setBillingPeriod(customerData.getBillingPeriod());
        existing.setCreditLimit(customerData.getCreditLimit());
        existing.setNotes(customerData.getNotes());
        if (customerData.getAccountType() != null) {
            existing.setAccountType(customerData.getAccountType());
        }

        return accountCustomerRepository.save(existing);
    }

    // Get customer by ID
    public AccountCustomer getCustomerById(Long id) {
        return accountCustomerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with id: " + id));
    }

    // Get all customers
    public List<AccountCustomer> getAllCustomers() {
        return accountCustomerRepository.findAll();
    }

    // Get customers by account_id
    public List<AccountCustomer> getCustomersByAccountId(String accountId) {
        return accountCustomerRepository.findByAccountId(accountId);
    }

    // Get active customers by account_id
    public List<AccountCustomer> getActiveCustomersByAccountId(String accountId) {
        return accountCustomerRepository.findByAccountIdAndActive(accountId, true);
    }

    // Get active customers only
    public List<AccountCustomer> getActiveCustomers() {
        return accountCustomerRepository.findByActiveTrue();
    }

    // Get customers by city
    public List<AccountCustomer> getCustomersByCity(String city) {
        return accountCustomerRepository.findByCityIgnoreCase(city);
    }

    // Get customers by province
    public List<AccountCustomer> getCustomersByProvince(String province) {
        return accountCustomerRepository.findByProvinceIgnoreCase(province);
    }

    // Search customers by company name
    public List<AccountCustomer> searchByCompanyName(String name) {
        return accountCustomerRepository.searchByCompanyName(name);
    }

    // Activate customer
    public AccountCustomer activateCustomer(Long id) {
        AccountCustomer customer = getCustomerById(id);
        customer.setActive(true);
        return accountCustomerRepository.save(customer);
    }

    // Deactivate customer
    public AccountCustomer deactivateCustomer(Long id) {
        AccountCustomer customer = getCustomerById(id);
        
        // Check for unpaid charges
        long unpaidCount = accountChargeRepository.countByAccountCustomerIdAndPaidFalse(id);
        if (unpaidCount > 0) {
            throw new IllegalStateException(
                    "Cannot deactivate customer with unpaid charges. Customer has " + 
                    unpaidCount + " unpaid charge(s).");
        }

        customer.setActive(false);
        return accountCustomerRepository.save(customer);
    }

    // Get customers with outstanding balance
    public List<AccountCustomer> getCustomersWithOutstandingBalance() {
        return accountCustomerRepository.findCustomersWithOutstandingBalance();
    }

    // Get customers with outstanding balance and amount
    public List<AccountCustomerWithBalance> getCustomersWithOutstandingBalanceAndAmount() {
        List<AccountCustomer> customers = accountCustomerRepository.findAllCustomersWithOutstandingBalanceJoined();

        return customers.stream()
            .map(customer -> {
                // Get total outstanding amount using existing query method
                BigDecimal totalOutstanding = accountChargeRepository.calculateUnpaidTotal(customer.getId());

                if (totalOutstanding != null && totalOutstanding.compareTo(BigDecimal.ZERO) > 0) {
                    return new AccountCustomerWithBalance(
                        customer.getId(),
                        customer.getAccountId(),
                        customer.getCompanyName(),
                        customer.getContactPerson(),
                        customer.getStreetAddress(),
                        customer.getCity(),
                        customer.getProvince(),
                        customer.getPostalCode(),
                        customer.getCountry(),
                        customer.getPhoneNumber(),
                        customer.getEmail(),
                        customer.getBillingPeriod(),
                        customer.getCreditLimit(),
                        customer.getNotes(),
                        customer.getAccountType() != null ? customer.getAccountType().toString() : null,
                        customer.isActive(),
                        totalOutstanding
                    );
                }

                return null;
            })
            .filter(c -> c != null)
            .collect(Collectors.toList());
    }

    // Get customers by billing period
    public List<AccountCustomer> getCustomersByBillingPeriod(String billingPeriod) {
        return accountCustomerRepository.findByBillingPeriod(billingPeriod);
    }

    // Bulk operations - Activate multiple customers by account_id
    public List<AccountCustomer> activateCustomersByAccountId(String accountId) {
        List<AccountCustomer> customers = accountCustomerRepository.findByAccountIdAndActive(accountId, false);
        customers.forEach(customer -> customer.setActive(true));
        return accountCustomerRepository.saveAll(customers);
    }

    // Bulk operations - Deactivate multiple customers by account_id (if no unpaid charges)
    public List<AccountCustomer> deactivateCustomersByAccountId(String accountId) {
        List<AccountCustomer> customers = accountCustomerRepository.findByAccountIdAndActive(accountId, true);
        
        // Check each customer for unpaid charges
        for (AccountCustomer customer : customers) {
            long unpaidCount = accountChargeRepository.countByAccountCustomerIdAndPaidFalse(customer.getId());
            if (unpaidCount > 0) {
                throw new IllegalStateException(
                        "Cannot deactivate customer '" + customer.getCompanyName() + 
                        "' - has " + unpaidCount + " unpaid charge(s)");
            }
        }
        
        customers.forEach(customer -> customer.setActive(false));
        return accountCustomerRepository.saveAll(customers);
    }
}