package com.taxi.web.controller;

import com.taxi.domain.account.dto.AccountChargeDTO;
import com.taxi.domain.account.dto.BulkUpdateTipRequest;
import com.taxi.domain.account.model.AccountCharge;
import com.taxi.domain.account.model.AccountCustomer;
import com.taxi.domain.account.service.AccountChargeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/account-charges")
@RequiredArgsConstructor
public class AccountChargeController {

    private final AccountChargeService accountChargeService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DISPATCHER')")
    public ResponseEntity<AccountChargeDTO> createCharge(@RequestBody AccountCharge charge) {
        AccountCharge created = accountChargeService.createCharge(charge);
        return ResponseEntity.ok(AccountChargeDTO.fromEntity(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<AccountChargeDTO> updateCharge(@PathVariable Long id, @RequestBody AccountCharge charge) {
        AccountCharge updated = accountChargeService.updateCharge(id, charge);
        return ResponseEntity.ok(AccountChargeDTO.fromEntity(updated));
    }

    @PutMapping("/bulk-update-tips")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<Map<String, Object>> bulkUpdateTips(@RequestBody List<BulkUpdateTipRequest> updates) {
        List<AccountCharge> updatedCharges = accountChargeService.bulkUpdateTips(updates);
        List<Long> updatedIds = updatedCharges.stream()
                .map(AccountCharge::getId)
                .collect(Collectors.toList());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "updatedCount", updatedIds.size(),
                "updatedIds", updatedIds
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DISPATCHER')")
    public ResponseEntity<AccountChargeDTO> getCharge(@PathVariable Long id) {
        AccountCharge charge = accountChargeService.getChargeById(id);
        return ResponseEntity.ok(AccountChargeDTO.fromEntity(charge));
    }

    // UPDATED: Add pagination support
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<Map<String, Object>> getAllCharges(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "tripDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) Long cabId,
            @RequestParam(required = false) Long driverId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        Sort sort = sortDir.equalsIgnoreCase("asc") ? 
            Sort.by(sortBy).ascending() : 
            Sort.by(sortBy).descending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<AccountCharge> chargePage = accountChargeService.getAllCharges(pageable, customerName, cabId, driverId, startDate, endDate);
        
        return ResponseEntity.ok(createPageResponse(chargePage));
    }

    // Get charges by account_id
    @GetMapping("/account/{accountId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<AccountChargeDTO>> getChargesByAccountId(@PathVariable String accountId) {
        List<AccountCharge> charges = accountChargeService.getChargesByAccountId(accountId);
        List<AccountChargeDTO> dtos = charges.stream()
                .map(AccountChargeDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    // Get unpaid charges by account_id
    @GetMapping("/account/{accountId}/unpaid")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<AccountChargeDTO>> getUnpaidChargesByAccountId(@PathVariable String accountId) {
        List<AccountCharge> charges = accountChargeService.getUnpaidChargesByAccountId(accountId);
        List<AccountChargeDTO> dtos = charges.stream()
                .map(AccountChargeDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    // Get charges by account_id and date range
    @GetMapping("/account/{accountId}/between")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<AccountChargeDTO>> getChargesByAccountIdAndDateRange(
            @PathVariable String accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<AccountCharge> charges = accountChargeService.getChargesByAccountIdAndDateRange(accountId, startDate, endDate);
        List<AccountChargeDTO> dtos = charges.stream()
                .map(AccountChargeDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    // UPDATED: Add pagination to customer charges
    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DISPATCHER')")
    public ResponseEntity<Map<String, Object>> getChargesByCustomer(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "tripDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("asc") ? 
            Sort.by(sortBy).ascending() : 
            Sort.by(sortBy).descending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<AccountCharge> chargePage = accountChargeService.getChargesByCustomer(customerId, pageable);
        
        return ResponseEntity.ok(createPageResponse(chargePage));
    }

    @GetMapping("/customer/{customerId}/unpaid")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<AccountChargeDTO>> getUnpaidChargesByCustomer(@PathVariable Long customerId) {
        List<AccountCharge> charges = accountChargeService.getUnpaidChargesByCustomer(customerId);
        List<AccountChargeDTO> dtos = charges.stream()
                .map(AccountChargeDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/customer/{customerId}/between")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<AccountChargeDTO>> getChargesByCustomerAndDateRange(
            @PathVariable Long customerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<AccountCharge> charges = accountChargeService.getChargesByCustomerAndDateRange(customerId, startDate, endDate);
        List<AccountChargeDTO> dtos = charges.stream()
                .map(AccountChargeDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/customer/{customerId}/unpaid/between")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<AccountChargeDTO>> getUnpaidChargesByCustomerAndDateRange(
            @PathVariable Long customerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<AccountCharge> charges = accountChargeService.getUnpaidChargesByCustomerAndDateRange(customerId, startDate, endDate);
        List<AccountChargeDTO> dtos = charges.stream()
                .map(AccountChargeDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/between")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<AccountChargeDTO>> getChargesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<AccountCharge> charges = accountChargeService.getChargesByDateRange(startDate, endDate);
        List<AccountChargeDTO> dtos = charges.stream()
                .map(AccountChargeDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<AccountChargeDTO>> getOverdueCharges() {
        List<AccountCharge> charges = accountChargeService.getOverdueCharges();
        List<AccountChargeDTO> dtos = charges.stream()
                .map(AccountChargeDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/job-code/{jobCode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DISPATCHER')")
    public ResponseEntity<List<AccountChargeDTO>> getChargesByJobCode(@PathVariable String jobCode) {
        List<AccountCharge> charges = accountChargeService.getChargesByJobCode(jobCode);
        List<AccountChargeDTO> dtos = charges.stream()
                .map(AccountChargeDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/{id}/mark-paid")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<AccountChargeDTO> markChargePaid(@PathVariable Long id, @RequestParam String invoiceNumber) {
        AccountCharge charge = accountChargeService.markChargeAsPaid(id, invoiceNumber);
        return ResponseEntity.ok(AccountChargeDTO.fromEntity(charge));
    }

    @PostMapping("/{id}/mark-unpaid")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<AccountChargeDTO> markChargeUnpaid(@PathVariable Long id) {
        AccountCharge charge = accountChargeService.markChargeAsUnpaid(id);
        return ResponseEntity.ok(AccountChargeDTO.fromEntity(charge));
    }

    @PostMapping("/mark-paid-batch")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<AccountChargeDTO>> markChargesPaid(
            @RequestParam List<Long> chargeIds,
            @RequestParam String invoiceNumber) {
        List<AccountCharge> charges = accountChargeService.markChargesAsPaid(chargeIds, invoiceNumber);
        List<AccountChargeDTO> dtos = charges.stream()
                .map(AccountChargeDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    // Charges cannot be deleted - only updated
    // No delete endpoint provided

    @GetMapping("/customer/{customerId}/billing-summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<Map<String, Object>> getBillingSummary(
            @PathVariable Long customerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Map<String, Object> summary = accountChargeService.generateBillingSummary(customerId, startDate, endDate);
        
        // Convert charges in summary to DTOs
        @SuppressWarnings("unchecked")
        List<AccountCharge> charges = (List<AccountCharge>) summary.get("charges");
        if (charges != null) {
            List<AccountChargeDTO> dtos = charges.stream()
                    .map(AccountChargeDTO::fromEntity)
                    .collect(Collectors.toList());
            summary.put("charges", dtos);
        }
        
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/for-billing")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<Map<Long, List<AccountChargeDTO>>> getChargesForBilling(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Map<AccountCustomer, List<AccountCharge>> chargesByCustomer = accountChargeService.getChargesGroupedByCustomer(startDate, endDate);
        
        // Convert Map<AccountCustomer, List<AccountCharge>> to Map<Long, List<AccountChargeDTO>>
        Map<Long, List<AccountChargeDTO>> result = chargesByCustomer.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().getId(),  // Convert AccountCustomer to Long (customer ID)
                        entry -> entry.getValue().stream()
                                .map(AccountChargeDTO::fromEntity)
                                .collect(Collectors.toList())
                ));
        
        return ResponseEntity.ok(result);
    }

    /**
     * Helper method to create paginated response
     */
    private Map<String, Object> createPageResponse(Page<AccountCharge> chargePage) {
        Map<String, Object> response = new HashMap<>();
        
        List<AccountChargeDTO> content = chargePage.getContent().stream()
                .map(AccountChargeDTO::fromEntity)
                .collect(Collectors.toList());
        
        response.put("content", content);
        response.put("currentPage", chargePage.getNumber());
        response.put("totalItems", chargePage.getTotalElements());
        response.put("totalPages", chargePage.getTotalPages());
        response.put("pageSize", chargePage.getSize());
        response.put("hasNext", chargePage.hasNext());
        response.put("hasPrevious", chargePage.hasPrevious());
        response.put("isFirst", chargePage.isFirst());
        response.put("isLast", chargePage.isLast());
        
        return response;
    }
}