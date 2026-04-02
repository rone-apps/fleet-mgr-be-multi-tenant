package com.taxi.web.controller;

import com.taxi.domain.driver.model.Driver;
import com.taxi.domain.driver.repository.DriverRepository;
import com.taxi.domain.eft.model.BankAccount;
import com.taxi.domain.eft.model.EftConfig;
import com.taxi.domain.eft.model.EftFileGeneration;
import com.taxi.domain.eft.repository.BankAccountRepository;
import com.taxi.domain.eft.repository.EftConfigRepository;
import com.taxi.domain.eft.service.EftFileGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/eft")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
public class EftController {

    private final EftFileGenerationService eftService;
    private final EftConfigRepository eftConfigRepository;
    private final BankAccountRepository bankAccountRepository;
    private final DriverRepository driverRepository;

    // ==================== EFT Configuration ====================

    @GetMapping("/config")
    public ResponseEntity<EftConfig> getConfig() {
        return eftConfigRepository.findByIsActiveTrue()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EftConfig> saveConfig(@RequestBody EftConfig config) {
        // Deactivate existing configs
        eftConfigRepository.findByIsActiveTrue().ifPresent(existing -> {
            if (config.getId() == null || !config.getId().equals(existing.getId())) {
                existing.setIsActive(false);
                eftConfigRepository.save(existing);
            }
        });
        config.setIsActive(true);
        EftConfig saved = eftConfigRepository.save(config);
        return ResponseEntity.ok(saved);
    }

    // ==================== Bank Accounts ====================

    @GetMapping("/bank-accounts")
    public ResponseEntity<List<BankAccount>> getAllBankAccounts() {
        return ResponseEntity.ok(bankAccountRepository.findByIsActiveTrue());
    }

    @GetMapping("/bank-accounts/driver/{driverId}")
    public ResponseEntity<?> getBankAccount(@PathVariable Long driverId) {
        return bankAccountRepository.findByDriverIdAndIsActiveTrue(driverId)
                .map(ba -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("id", ba.getId());
                    result.put("driverId", ba.getDriver().getId());
                    result.put("accountHolderName", ba.getAccountHolderName());
                    result.put("institutionNumber", ba.getInstitutionNumber());
                    result.put("transitNumber", ba.getTransitNumber());
                    result.put("maskedAccountNumber", ba.getMaskedAccountNumber());
                    result.put("accountType", ba.getAccountType());
                    result.put("isVerified", ba.getIsVerified());
                    result.put("routingNumber", ba.getRoutingNumber());
                    return ResponseEntity.ok((Object) result);
                })
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/bank-accounts")
    public ResponseEntity<Map<String, Object>> saveBankAccount(@RequestBody Map<String, Object> request) {
        Long driverId = Long.valueOf(request.get("driverId").toString());

        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found: " + driverId));

        // Deactivate existing bank account for this driver
        bankAccountRepository.findByDriverIdAndIsActiveTrue(driverId)
                .ifPresent(existing -> {
                    existing.setIsActive(false);
                    bankAccountRepository.save(existing);
                });

        BankAccount bankAccount = BankAccount.builder()
                .driver(driver)
                .accountHolderName((String) request.get("accountHolderName"))
                .institutionNumber((String) request.get("institutionNumber"))
                .transitNumber((String) request.get("transitNumber"))
                .accountNumber((String) request.get("accountNumber"))
                .accountType(request.getOrDefault("accountType", "CHEQUING").toString())
                .isActive(true)
                .isVerified(false)
                .build();

        bankAccountRepository.save(bankAccount);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", bankAccount.getId());
        result.put("driverId", driverId);
        result.put("accountHolderName", bankAccount.getAccountHolderName());
        result.put("maskedAccountNumber", bankAccount.getMaskedAccountNumber());
        result.put("routingNumber", bankAccount.getRoutingNumber());
        result.put("accountType", bankAccount.getAccountType());
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/bank-accounts/{id}")
    public ResponseEntity<Void> deactivateBankAccount(@PathVariable Long id) {
        bankAccountRepository.findById(id).ifPresent(ba -> {
            ba.setIsActive(false);
            bankAccountRepository.save(ba);
        });
        return ResponseEntity.ok().build();
    }

    // ==================== EFT File Generation ====================

    @PostMapping("/generate/{batchId}")
    public ResponseEntity<?> generateEftFile(@PathVariable Long batchId, Authentication authentication) {
        try {
            String user = authentication != null ? authentication.getName() : "system";
            EftFileGenerationService.EftGenerationResult result = eftService.generateEftFile(batchId, user);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("fileName", result.fileName);
            response.put("recordCount", result.recordCount);
            response.put("totalAmount", result.totalAmount);
            response.put("warnings", result.warnings);
            response.put("generationId", result.generationId);
            response.put("fileContent", result.fileContent);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/download/{batchId}")
    public ResponseEntity<?> downloadEftFile(@PathVariable Long batchId, Authentication authentication) {
        try {
            String user = authentication != null ? authentication.getName() : "system";
            EftFileGenerationService.EftGenerationResult result = eftService.generateEftFile(batchId, user);

            byte[] fileBytes = result.fileContent.getBytes(StandardCharsets.UTF_8);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDisposition(ContentDisposition.attachment().filename(result.fileName).build());
            headers.setContentLength(fileBytes.length);

            return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/history/{batchId}")
    public ResponseEntity<List<EftFileGeneration>> getGenerationHistory(@PathVariable Long batchId) {
        return ResponseEntity.ok(eftService.getGenerationHistory(batchId));
    }
}
