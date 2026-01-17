package com.taxi.web.controller;

import com.taxi.domain.financial.Merchant2CabDTO;
import com.taxi.domain.financial.Merchant2CabService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/financial/merchant2cab")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT', 'MANAGER')")
public class Merchant2CabController {
    
    private final Merchant2CabService merchant2CabService;
    
    @GetMapping
    public ResponseEntity<List<Merchant2CabDTO>> getAllMappings() {
        return ResponseEntity.ok(merchant2CabService.getAllMappings());
    }
    
    @GetMapping("/active")
    public ResponseEntity<List<Merchant2CabDTO>> getAllActiveMappings() {
        return ResponseEntity.ok(merchant2CabService.getAllActiveMappings());
    }
    
    @GetMapping("/cab/{cabNumber}")
    public ResponseEntity<List<Merchant2CabDTO>> getMappingsByCabNumber(@PathVariable String cabNumber) {
        return ResponseEntity.ok(merchant2CabService.getMappingsByCabNumber(cabNumber));
    }
    
    @GetMapping("/cab/{cabNumber}/current")
    public ResponseEntity<Merchant2CabDTO> getCurrentMappingByCabNumber(@PathVariable String cabNumber) {
        Merchant2CabDTO mapping = merchant2CabService.getCurrentMappingByCabNumber(cabNumber);
        return mapping != null ? ResponseEntity.ok(mapping) : ResponseEntity.notFound().build();
    }
    
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Merchant2CabDTO> createMapping(
            @Valid @RequestBody Merchant2CabDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        Merchant2CabDTO created = merchant2CabService.createMapping(dto, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Merchant2CabDTO> updateMapping(
            @PathVariable Long id,
            @Valid @RequestBody Merchant2CabDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(merchant2CabService.updateMapping(id, dto, userDetails.getUsername()));
    }
    
    @PatchMapping("/{id}/end")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> endMapping(
            @PathVariable Long id,
            @RequestParam LocalDate endDate,
            @AuthenticationPrincipal UserDetails userDetails) {
        merchant2CabService.endMapping(id, endDate, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteMapping(@PathVariable Long id) {
        merchant2CabService.deleteMapping(id);
        return ResponseEntity.noContent().build();
    }
}
