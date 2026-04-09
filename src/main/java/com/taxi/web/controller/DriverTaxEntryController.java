package com.taxi.web.controller;

import com.taxi.domain.tax.service.DriverTaxEntryService;
import com.taxi.web.dto.tax.DriverTaxEntryDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/driver-tax-entries")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class DriverTaxEntryController {

    private final DriverTaxEntryService driverTaxEntryService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DISPATCHER')")
    public ResponseEntity<List<DriverTaxEntryDTO>> getEntries(
            @RequestParam Long driverId,
            @RequestParam Integer taxYear) {
        try {
            List<DriverTaxEntryDTO> entries = driverTaxEntryService.getEntriesForDriver(driverId, taxYear);
            return ResponseEntity.ok(entries);
        } catch (Exception e) {
            log.error("Error fetching tax entries", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/by-type")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DISPATCHER')")
    public ResponseEntity<List<DriverTaxEntryDTO>> getEntriesByType(
            @RequestParam Long driverId,
            @RequestParam Integer taxYear,
            @RequestParam String entryType) {
        try {
            List<DriverTaxEntryDTO> entries = driverTaxEntryService.getEntriesForDriverByType(driverId, taxYear, entryType);
            return ResponseEntity.ok(entries);
        } catch (Exception e) {
            log.error("Error fetching tax entries by type", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DISPATCHER')")
    public ResponseEntity<DriverTaxEntryDTO> createEntry(@RequestBody DriverTaxEntryDTO request) {
        try {
            DriverTaxEntryDTO created = driverTaxEntryService.createEntry(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            log.error("Error creating tax entry", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DISPATCHER')")
    public ResponseEntity<DriverTaxEntryDTO> updateEntry(
            @PathVariable Long id,
            @RequestBody DriverTaxEntryDTO request) {
        try {
            DriverTaxEntryDTO updated = driverTaxEntryService.updateEntry(id, request);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Error updating tax entry", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'DISPATCHER')")
    public ResponseEntity<Void> deleteEntry(@PathVariable Long id) {
        try {
            driverTaxEntryService.deleteEntry(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting tax entry", e);
            return ResponseEntity.badRequest().build();
        }
    }
}
