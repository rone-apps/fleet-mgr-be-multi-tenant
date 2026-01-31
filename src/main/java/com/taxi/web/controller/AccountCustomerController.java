package com.taxi.web.controller;

import com.taxi.domain.account.model.AccountCustomer;
import com.taxi.domain.account.model.AccountCustomerWithBalance;
import com.taxi.domain.account.service.AccountCustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/account-customers")
@RequiredArgsConstructor
public class AccountCustomerController {

    private final AccountCustomerService accountCustomerService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<AccountCustomer> createCustomer(@RequestBody AccountCustomer customer) {
        AccountCustomer created = accountCustomerService.createCustomer(customer);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<AccountCustomer> updateCustomer(
            @PathVariable Long id, 
            @RequestBody AccountCustomer customer) {
        AccountCustomer updated = accountCustomerService.updateCustomer(id, customer);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DISPATCHER')")
    public ResponseEntity<AccountCustomer> getCustomer(@PathVariable Long id) {
        AccountCustomer customer = accountCustomerService.getCustomerById(id);
        return ResponseEntity.ok(customer);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DISPATCHER')")
    public ResponseEntity<List<AccountCustomer>> getAllCustomers() {
        List<AccountCustomer> customers = accountCustomerService.getAllCustomers();
        return ResponseEntity.ok(customers);
    }

    // Get customers by account_id
    @GetMapping("/account/{accountId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DISPATCHER')")
    public ResponseEntity<List<AccountCustomer>> getCustomersByAccountId(@PathVariable String accountId) {
        List<AccountCustomer> customers = accountCustomerService.getCustomersByAccountId(accountId);
        return ResponseEntity.ok(customers);
    }

    // Get active customers by account_id
    @GetMapping("/account/{accountId}/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DISPATCHER')")
    public ResponseEntity<List<AccountCustomer>> getActiveCustomersByAccountId(@PathVariable String accountId) {
        List<AccountCustomer> customers = accountCustomerService.getActiveCustomersByAccountId(accountId);
        return ResponseEntity.ok(customers);
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DISPATCHER')")
    public ResponseEntity<List<AccountCustomer>> getActiveCustomers() {
        List<AccountCustomer> customers = accountCustomerService.getActiveCustomers();
        return ResponseEntity.ok(customers);
    }

    @GetMapping("/by-city/{city}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<AccountCustomer>> getCustomersByCity(@PathVariable String city) {
        List<AccountCustomer> customers = accountCustomerService.getCustomersByCity(city);
        return ResponseEntity.ok(customers);
    }

    @GetMapping("/by-province/{province}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<AccountCustomer>> getCustomersByProvince(@PathVariable String province) {
        List<AccountCustomer> customers = accountCustomerService.getCustomersByProvince(province);
        return ResponseEntity.ok(customers);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DISPATCHER')")
    public ResponseEntity<List<AccountCustomer>> searchByCompanyName(@RequestParam String name) {
        List<AccountCustomer> customers = accountCustomerService.searchByCompanyName(name);
        return ResponseEntity.ok(customers);
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<AccountCustomer> activateCustomer(@PathVariable Long id) {
        AccountCustomer customer = accountCustomerService.activateCustomer(id);
        return ResponseEntity.ok(customer);
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<AccountCustomer> deactivateCustomer(@PathVariable Long id) {
        AccountCustomer customer = accountCustomerService.deactivateCustomer(id);
        return ResponseEntity.ok(customer);
    }

    @GetMapping("/with-outstanding-balance")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<AccountCustomer>> getCustomersWithOutstandingBalance() {
        List<AccountCustomer> customers = accountCustomerService.getCustomersWithOutstandingBalance();
        return ResponseEntity.ok(customers);
    }

    @GetMapping("/with-outstanding-balance/amount")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<AccountCustomerWithBalance>> getCustomersWithOutstandingBalanceAndAmount() {
        List<AccountCustomerWithBalance> customers = accountCustomerService.getCustomersWithOutstandingBalanceAndAmount();
        return ResponseEntity.ok(customers);
    }

    @GetMapping("/by-billing-period/{billingPeriod}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<AccountCustomer>> getCustomersByBillingPeriod(@PathVariable String billingPeriod) {
        List<AccountCustomer> customers = accountCustomerService.getCustomersByBillingPeriod(billingPeriod);
        return ResponseEntity.ok(customers);
    }

    // Bulk activate customers by account_id
    @PutMapping("/account/{accountId}/activate-all")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<AccountCustomer>> activateCustomersByAccountId(@PathVariable String accountId) {
        List<AccountCustomer> customers = accountCustomerService.activateCustomersByAccountId(accountId);
        return ResponseEntity.ok(customers);
    }

    // Bulk deactivate customers by account_id
    @PutMapping("/account/{accountId}/deactivate-all")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<AccountCustomer>> deactivateCustomersByAccountId(@PathVariable String accountId) {
        List<AccountCustomer> customers = accountCustomerService.deactivateCustomersByAccountId(accountId);
        return ResponseEntity.ok(customers);
    }

    // No delete endpoint - customers cannot be deleted
}