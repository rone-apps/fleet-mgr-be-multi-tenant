package com.taxi.web.controller;

import com.taxi.domain.drivertrip.dto.DriverTripDTO;
import com.taxi.domain.drivertrip.model.DriverTrip;
import com.taxi.domain.drivertrip.service.DriverTripService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/driver-trips")
@RequiredArgsConstructor
public class DriverTripController {

    private final DriverTripService driverTripService;

    /**
     * Search driver trips with filters and pagination
     * GET /api/driver-trips?page=0&size=50&sortBy=tripDate&sortDir=desc&startDate=...&endDate=...
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<Map<String, Object>> searchTrips(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "tripDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String driverUsername,
            @RequestParam(required = false) Long cabId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String driverName) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Page<DriverTrip> tripPage = driverTripService.searchTrips(
                driverUsername, cabId, startDate, endDate, driverName,
                PageRequest.of(page, size, sort));

        Map<String, Object> response = new HashMap<>();
        response.put("content", tripPage.getContent().stream().map(DriverTripDTO::fromEntity).toList());
        response.put("currentPage", tripPage.getNumber());
        response.put("totalItems", tripPage.getTotalElements());
        response.put("totalPages", tripPage.getTotalPages());
        response.put("pageSize", tripPage.getSize());
        response.put("hasNext", tripPage.hasNext());
        response.put("hasPrevious", tripPage.hasPrevious());

        return ResponseEntity.ok(response);
    }

    /**
     * Update account number on a driver trip
     * PUT /api/driver-trips/{id}/account-number
     */
    @PutMapping("/{id}/account-number")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> updateAccountNumber(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String accountNumber = body.get("accountNumber");
        DriverTripDTO updated = driverTripService.updateAccountNumber(id, accountNumber);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Account number updated successfully");
        response.put("data", updated);

        return ResponseEntity.ok(response);
    }

    /**
     * Convert a driver trip to an account charge
     * POST /api/driver-trips/{id}/convert-to-charge
     */
    @PostMapping("/{id}/convert-to-charge")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> convertToAccountCharge(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            driverTripService.convertToAccountCharge(id);
            response.put("success", true);
            response.put("message", "Trip successfully converted to account charge");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
