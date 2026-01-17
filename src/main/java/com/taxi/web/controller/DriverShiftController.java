package com.taxi.web.controller;

import com.taxi.domain.shift.dto.ActiveShiftResponse;
import com.taxi.domain.shift.dto.DriverLogonRequest;
import com.taxi.domain.shift.dto.DriverLogoffRequest;
import com.taxi.domain.shift.dto.ShiftResponse;
import com.taxi.domain.shift.dto.ShiftSummary;
import com.taxi.domain.shift.service.DriverShiftService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/shifts")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // Configure appropriately for production
public class DriverShiftController {

    private final DriverShiftService driverShiftService;

    /**
     * Log on a driver - creates new active shift
     * POST /api/shifts/logon
     */
    @PostMapping("/logon")
    public ResponseEntity<Map<String, Object>> logonDriver(@Valid @RequestBody DriverLogonRequest request) {
        try {
            log.info("Logon request for driver: {}", request.getDriverNumber());
            ShiftResponse shift = driverShiftService.logonDriver(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Driver logged on successfully");
            response.put("shiftId", shift.getId());
            response.put("shift", shift);

            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            log.error("Logon failed: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Logon error", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to log on driver: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Log off a driver - completes shift and calculates hours/shifts
     * PUT /api/shifts/{shiftId}/logoff
     */
    @PutMapping("/{shiftId}/logoff")
    public ResponseEntity<Map<String, Object>> logoffDriver(
            @PathVariable Long shiftId,
            @RequestBody DriverLogoffRequest request) {
        try {
            log.info("Logoff request for shift: {}", shiftId);
            ShiftResponse shift = driverShiftService.logoffDriver(shiftId, request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Driver logged off successfully");
            response.put("shift", shift);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Logoff failed: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Logoff error", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to log off driver: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get all active shifts
     * GET /api/shifts/active
     */
    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> getActiveShifts() {
        try {
            List<ActiveShiftResponse> shifts = driverShiftService.getActiveShifts();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", shifts);
            response.put("count", shifts.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching active shifts", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to fetch active shifts: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get active shift for a specific driver
     * GET /api/shifts/driver/{driverId}/active
     */
    @GetMapping("/driver/{driverNumber}/active")
    public ResponseEntity<Map<String, Object>> getActiveShiftForDriver(@PathVariable String driverNumber) {
        try {
            Optional<ShiftResponse> shift = driverShiftService.getActiveShiftForDriver(driverNumber);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("hasActiveShift", shift.isPresent());
            shift.ifPresent(s -> response.put("shift", s));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching active shift for driver", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to fetch active shift: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get all shifts for a driver
     * GET /api/shifts/driver/{driverId}
     */
    @GetMapping("/driver/{driverNumber}")
    public ResponseEntity<Map<String, Object>> getDriverShifts(@PathVariable String driverNumber) {
        try {
            List<ShiftResponse> shifts = driverShiftService.getDriverShifts(driverNumber);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", shifts);
            response.put("count", shifts.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching driver shifts", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to fetch shifts: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get shifts for a driver in date range
     * GET /api/shifts/driver/{driverId}/range?startDate=2025-12-01&endDate=2025-12-31
     */
    @GetMapping("/driver/{driverNumber}/range")
    public ResponseEntity<Map<String, Object>> getDriverShiftsInRange(
            @PathVariable String driverNumber,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<ShiftResponse> shifts = driverShiftService.getDriverShiftsInRange(driverNumber, startDate, endDate);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", shifts);
            response.put("count", shifts.size());
            response.put("startDate", startDate);
            response.put("endDate", endDate);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching driver shifts in range", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to fetch shifts: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get all shifts in date range
     * GET /api/shifts/range?startDate=2025-12-01&endDate=2025-12-31
     */
    @GetMapping("/range")
    public ResponseEntity<Map<String, Object>> getShiftsInRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<ShiftResponse> shifts = driverShiftService.getShiftsInRange(startDate, endDate);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", shifts);
            response.put("count", shifts.size());
            response.put("startDate", startDate);
            response.put("endDate", endDate);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching shifts in range", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to fetch shifts: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get shift summary for a driver
     * GET /api/shifts/driver/{driverId}/summary?startDate=2025-12-01&endDate=2025-12-31
     */
    @GetMapping("/driver/{driverNumber}/summary")
    public ResponseEntity<Map<String, Object>> getDriverShiftSummary(
            @PathVariable String driverNumber,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            ShiftSummary summary = driverShiftService.getDriverShiftSummary(driverNumber, startDate, endDate);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", summary);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching shift summary", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to fetch summary: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get shift by ID
     * GET /api/shifts/{shiftId}
     */
    @GetMapping("/{shiftId}")
    public ResponseEntity<Map<String, Object>> getShiftById(@PathVariable Long shiftId) {
        try {
            ShiftResponse shift = driverShiftService.getShiftById(shiftId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", shift);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("Error fetching shift", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to fetch shift: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Cancel a shift
     * PUT /api/shifts/{shiftId}/cancel
     */
    @PutMapping("/{shiftId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelShift(
            @PathVariable Long shiftId,
            @RequestParam Long updatedBy) {
        try {
            ShiftResponse shift = driverShiftService.cancelShift(shiftId, updatedBy);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Shift cancelled successfully");
            response.put("shift", shift);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Error cancelling shift", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to cancel shift: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
