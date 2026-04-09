package com.taxi.web.controller;

import com.taxi.domain.tax.service.TaxCalculationService;
import com.taxi.web.dto.tax.TaxCalculationResultDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tax-calculations")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class TaxCalculationController {

    private final TaxCalculationService taxCalculationService;

    @PostMapping("/calculate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DISPATCHER')")
    public ResponseEntity<TaxCalculationResultDTO> calculateTax(
            @RequestParam Long driverId,
            @RequestParam Integer taxYear) {
        try {
            TaxCalculationResultDTO result = taxCalculationService.calculateTax(driverId, taxYear);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error calculating tax", e);
            return ResponseEntity.badRequest().build();
        }
    }
}
