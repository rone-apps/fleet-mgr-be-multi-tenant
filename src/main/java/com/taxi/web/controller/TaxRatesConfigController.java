package com.taxi.web.controller;

import com.taxi.domain.tax.service.TaxRatesConfigService;
import com.taxi.web.dto.tax.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tax-rates-config")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class TaxRatesConfigController {

    private final TaxRatesConfigService taxRatesConfigService;

    // ===== Tax Brackets =====
    @GetMapping("/brackets")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DISPATCHER')")
    public ResponseEntity<List<IncomeTaxBracketDTO>> getBrackets(
            @RequestParam Integer taxYear,
            @RequestParam String jurisdiction) {
        try {
            List<IncomeTaxBracketDTO> brackets = taxRatesConfigService.getBrackets(taxYear, jurisdiction);
            return ResponseEntity.ok(brackets);
        } catch (Exception e) {
            log.error("Error fetching brackets", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/brackets")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<IncomeTaxBracketDTO> saveBracket(@RequestBody IncomeTaxBracketDTO request) {
        try {
            IncomeTaxBracketDTO saved = taxRatesConfigService.saveBracket(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            log.error("Error saving bracket", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/brackets/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<IncomeTaxBracketDTO> updateBracket(
            @PathVariable Long id,
            @RequestBody IncomeTaxBracketDTO request) {
        try {
            request.setId(id);
            IncomeTaxBracketDTO saved = taxRatesConfigService.saveBracket(request);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            log.error("Error updating bracket", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/brackets/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteBracket(@PathVariable Long id) {
        try {
            taxRatesConfigService.deleteBracket(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting bracket", e);
            return ResponseEntity.badRequest().build();
        }
    }

    // ===== Tax Credits =====
    @GetMapping("/credits")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DISPATCHER')")
    public ResponseEntity<List<TaxCreditDTO>> getCredits(
            @RequestParam Integer taxYear,
            @RequestParam String jurisdiction) {
        try {
            List<TaxCreditDTO> credits = taxRatesConfigService.getCredits(taxYear, jurisdiction);
            return ResponseEntity.ok(credits);
        } catch (Exception e) {
            log.error("Error fetching credits", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/credits")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<TaxCreditDTO> saveCredit(@RequestBody TaxCreditDTO request) {
        try {
            TaxCreditDTO saved = taxRatesConfigService.saveCredit(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            log.error("Error saving credit", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/credits/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<TaxCreditDTO> updateCredit(
            @PathVariable Long id,
            @RequestBody TaxCreditDTO request) {
        try {
            request.setId(id);
            TaxCreditDTO saved = taxRatesConfigService.saveCredit(request);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            log.error("Error updating credit", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/credits/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCredit(@PathVariable Long id) {
        try {
            taxRatesConfigService.deleteCredit(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting credit", e);
            return ResponseEntity.badRequest().build();
        }
    }

    // ===== CPP/EI Rates =====
    @GetMapping("/cpp-ei")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DISPATCHER')")
    public ResponseEntity<CppEiRateDTO> getRates(@RequestParam Integer taxYear) {
        try {
            CppEiRateDTO rates = taxRatesConfigService.getRates(taxYear);
            return ResponseEntity.ok(rates);
        } catch (Exception e) {
            log.error("Error fetching CPP/EI rates", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/cpp-ei")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<CppEiRateDTO> saveRates(@RequestBody CppEiRateDTO request) {
        try {
            CppEiRateDTO saved = taxRatesConfigService.saveRates(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            log.error("Error saving CPP/EI rates", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/cpp-ei/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<CppEiRateDTO> updateRates(
            @PathVariable Long id,
            @RequestBody CppEiRateDTO request) {
        try {
            request.setId(id);
            CppEiRateDTO saved = taxRatesConfigService.saveRates(request);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            log.error("Error updating CPP/EI rates", e);
            return ResponseEntity.badRequest().build();
        }
    }
}
