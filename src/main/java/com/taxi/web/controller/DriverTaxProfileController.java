package com.taxi.web.controller;

import com.taxi.domain.tax.service.TaxRatesConfigService;
import com.taxi.web.dto.tax.DriverTaxProfileDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/driver-tax-profiles")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class DriverTaxProfileController {

    private final TaxRatesConfigService taxRatesConfigService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DISPATCHER')")
    public ResponseEntity<DriverTaxProfileDTO> getProfile(
            @RequestParam Long driverId,
            @RequestParam Integer taxYear) {
        try {
            DriverTaxProfileDTO profile = taxRatesConfigService.getOrCreateProfile(driverId, taxYear);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            log.error("Error fetching tax profile", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DISPATCHER')")
    public ResponseEntity<DriverTaxProfileDTO> saveProfile(@RequestBody DriverTaxProfileDTO request) {
        try {
            DriverTaxProfileDTO saved = taxRatesConfigService.saveProfile(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            log.error("Error saving tax profile", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DISPATCHER')")
    public ResponseEntity<DriverTaxProfileDTO> updateProfile(
            @PathVariable Long id,
            @RequestBody DriverTaxProfileDTO request) {
        try {
            request.setId(id);
            DriverTaxProfileDTO saved = taxRatesConfigService.saveProfile(request);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            log.error("Error updating tax profile", e);
            return ResponseEntity.badRequest().build();
        }
    }
}
