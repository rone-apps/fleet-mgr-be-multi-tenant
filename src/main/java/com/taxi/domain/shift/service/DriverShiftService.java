package com.taxi.domain.shift.service;

import com.taxi.domain.shift.dto.*;
import com.taxi.domain.shift.model.DriverShift;
import com.taxi.domain.shift.repository.DriverShiftRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverShiftService {

    private final DriverShiftRepository driverShiftRepository;

    /**
     * Log on a driver - creates a new active shift
     */
    @Transactional
    public ShiftResponse logonDriver(DriverLogonRequest request) {
        log.info("Logging on driver: {}", request.getDriverNumber());

        // Check if driver already has an active shift
        Optional<DriverShift> existingShift = driverShiftRepository
            .findByDriverNumberAndStatus(request.getDriverNumber(), "ACTIVE");

        if (existingShift.isPresent()) {
            throw new IllegalStateException("Driver already has an active shift");
        }

        // Create new shift
        DriverShift shift = new DriverShift();
        shift.setDriverNumber(request.getDriverNumber());
        shift.setCabNumber(request.getCabNumber());
        shift.setLogonTime(LocalDateTime.now());
        shift.setStatus("ACTIVE");
        shift.setCreatedBy(request.getCreatedBy());

        // Set primary shift type based on logon time
        int hour = shift.getLogonTime().getHour();
        shift.setPrimaryShiftType(hour >= 0 && hour < 12 ? "DAY" : "NIGHT");

        DriverShift savedShift = driverShiftRepository.save(shift);
        log.info("Driver {} logged on successfully. Shift ID: {}", request.getDriverNumber(), savedShift.getId());

        return convertToResponse(savedShift);
    }

    /**
     * Log off a driver - completes the shift and calculates hours/shifts
     */
    @Transactional
    public ShiftResponse logoffDriver(Long shiftId, DriverLogoffRequest request) {
        log.info("Logging off shift: {}", shiftId);

        DriverShift shift = driverShiftRepository.findById(shiftId)
            .orElseThrow(() -> new IllegalArgumentException("Shift not found"));

        if (!"ACTIVE".equals(shift.getStatus())) {
            throw new IllegalStateException("Shift is not active");
        }

        // Set logoff time
        LocalDateTime logoffTime = LocalDateTime.now();
        shift.setLogoffTime(logoffTime);

        // Calculate total hours
        Duration duration = Duration.between(shift.getLogonTime(), logoffTime);
        double hours = duration.toMinutes() / 60.0;
        shift.setTotalHours(BigDecimal.valueOf(hours).setScale(2, RoundingMode.HALF_UP));

        // Calculate shift counts based on business rules
        calculateShiftCounts(shift, hours);

        // Update performance metrics
        shift.setTotalTrips(request.getTotalTrips());
        shift.setTotalRevenue(request.getTotalRevenue());
        shift.setTotalDistance(request.getTotalDistance());
        shift.setNotes(request.getNotes());
        shift.setUpdatedBy(request.getUpdatedBy());
        shift.setStatus("COMPLETED");

        DriverShift savedShift = driverShiftRepository.save(shift);
        log.info("Shift {} completed. Total hours: {}, Day shifts: {}, Night shifts: {}",
                shiftId, savedShift.getTotalHours(), savedShift.getTotalDayShifts(), savedShift.getTotalNightShifts());

        return convertToResponse(savedShift);
    }

    /**
     * Calculate shift counts based on hours worked
     * Rules:
     * - Up to 12 hours = 1 primary shift
     * - 12-15 hours = 1 primary + 0.25 secondary
     * - 15-18 hours = 1 primary + 0.5 secondary
     * - 18+ hours = 1 primary + 1 full secondary
     */
    private void calculateShiftCounts(DriverShift shift, double hours) {
        int logonHour = shift.getLogonTime().getHour();

        // Determine primary and secondary shift types
        String primaryType;
        String secondaryType;
        if (logonHour >= 0 && logonHour < 12) {
            primaryType = "DAY";
            secondaryType = "NIGHT";
        } else {
            primaryType = "NIGHT";
            secondaryType = "DAY";
        }

        shift.setPrimaryShiftType(primaryType);

        if (hours <= 12) {
            // Simple case: one shift
            shift.setPrimaryShiftCount(BigDecimal.ONE);
            shift.setSecondaryShiftType(null);
            shift.setSecondaryShiftCount(BigDecimal.ZERO);
        } else {
            // Extended shift: calculate secondary shift
            shift.setPrimaryShiftCount(BigDecimal.ONE);
            shift.setSecondaryShiftType(secondaryType);

            double extraHours = hours - 12;
            if (extraHours < 3) {
                // 12-15 hours: 0.25 secondary shift
                shift.setSecondaryShiftCount(new BigDecimal("0.25"));
            } else if (extraHours < 6) {
                // 15-18 hours: 0.5 secondary shift
                shift.setSecondaryShiftCount(new BigDecimal("0.5"));
            } else {
                // 18+ hours: 1 full secondary shift
                shift.setSecondaryShiftCount(BigDecimal.ONE);
            }
        }
    }

    /**
     * Get all active shifts
     */
    public List<ActiveShiftResponse> getActiveShifts() {
        List<DriverShift> shifts = driverShiftRepository.findByStatus("ACTIVE");
        return shifts.stream()
            .map(this::convertToActiveResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get active shift for a driver
     */
    public Optional<ShiftResponse> getActiveShiftForDriver(String driverNumber) {
        return driverShiftRepository.findByDriverNumberAndStatus(driverNumber, "ACTIVE")
            .map(this::convertToResponse);
    }

    /**
     * Get all shifts for a driver
     */
    public List<ShiftResponse> getDriverShifts(String driverNumber) {
        List<DriverShift> shifts = driverShiftRepository.findByDriverNumberOrderByLogonTimeDesc(driverNumber);
        return shifts.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get shifts for a driver in date range
     */
    public List<ShiftResponse> getDriverShiftsInRange(String driverNumber, LocalDate startDate, LocalDate endDate) {
        List<DriverShift> shifts = driverShiftRepository
            .findByDriverNumberAndDateRange(driverNumber, startDate, endDate);
        return shifts.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get all shifts in date range
     */
    public List<ShiftResponse> getShiftsInRange(LocalDate startDate, LocalDate endDate) {
        List<DriverShift> shifts = driverShiftRepository.findByDateRange(startDate, endDate);
        return shifts.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get shift summary for a driver
     */
    public ShiftSummary getDriverShiftSummary(String driverNumber, LocalDate startDate, LocalDate endDate) {
        Object[] result = driverShiftRepository.getDriverShiftSummary(driverNumber, startDate, endDate);

        ShiftSummary summary = new ShiftSummary();
        summary.setDriverNumber(driverNumber);

        if (result != null && result.length > 0) {
            summary.setTotalHours((BigDecimal) result[0]);
            summary.setDayShifts((BigDecimal) result[1]);
            summary.setNightShifts((BigDecimal) result[2]);
            summary.setTotalShifts(summary.getDayShifts().add(summary.getNightShifts()));
            summary.setTotalTrips((Integer) result[3]);
            summary.setTotalRevenue((BigDecimal) result[4]);
            summary.setTotalDistance((BigDecimal) result[5]);
        }

        return summary;
    }

    /**
     * Cancel a shift
     */
    @Transactional
    public ShiftResponse cancelShift(Long shiftId, Long updatedBy) {
        DriverShift shift = driverShiftRepository.findById(shiftId)
            .orElseThrow(() -> new IllegalArgumentException("Shift not found"));

        if (!"ACTIVE".equals(shift.getStatus())) {
            throw new IllegalStateException("Only active shifts can be cancelled");
        }

        shift.setStatus("CANCELLED");
        shift.setUpdatedBy(updatedBy);
        shift.setLogoffTime(LocalDateTime.now());

        DriverShift savedShift = driverShiftRepository.save(shift);
        return convertToResponse(savedShift);
    }

    /**
     * Get shift by ID
     */
    public ShiftResponse getShiftById(Long shiftId) {
        DriverShift shift = driverShiftRepository.findById(shiftId)
            .orElseThrow(() -> new IllegalArgumentException("Shift not found"));
        return convertToResponse(shift);
    }

    // Conversion methods
    private ShiftResponse convertToResponse(DriverShift shift) {
        ShiftResponse response = new ShiftResponse();
        response.setId(shift.getId());
        response.setDriverNumber(shift.getDriverNumber());
        response.setDriverName(shift.getDriverName());
        response.setCabNumber(shift.getCabNumber());
        response.setLogonTime(shift.getLogonTime());
        response.setLogoffTime(shift.getLogoffTime());
        response.setTotalHours(shift.getTotalHours());
        response.setPrimaryShiftType(shift.getPrimaryShiftType());
        response.setPrimaryShiftCount(shift.getPrimaryShiftCount());
        response.setSecondaryShiftType(shift.getSecondaryShiftType());
        response.setSecondaryShiftCount(shift.getSecondaryShiftCount());
        response.setDayShifts(shift.getTotalDayShifts());
        response.setNightShifts(shift.getTotalNightShifts());
        response.setStatus(shift.getStatus());
        response.setTotalTrips(shift.getTotalTrips());
        response.setTotalRevenue(shift.getTotalRevenue());
        response.setTotalDistance(shift.getTotalDistance());
        response.setNotes(shift.getNotes());
        response.setCreatedAt(shift.getCreatedAt());
        response.setUpdatedAt(shift.getUpdatedAt());
        return response;
    }

    private ActiveShiftResponse convertToActiveResponse(DriverShift shift) {
        ActiveShiftResponse response = new ActiveShiftResponse();
        response.setId(shift.getId());
        response.setDriverNumber(shift.getDriverNumber());
        response.setDriverName(shift.getDriverName());
        response.setCabNumber(shift.getCabNumber());
        response.setLogonTime(shift.getLogonTime());
        response.setPrimaryShiftType(shift.getPrimaryShiftType());

        // Calculate hours so far
        Duration duration = Duration.between(shift.getLogonTime(), LocalDateTime.now());
        response.setHoursSoFar(duration.toHours());

        response.setTotalTrips(shift.getTotalTrips());
        response.setTotalRevenue(shift.getTotalRevenue());
        response.setTotalDistance(shift.getTotalDistance());
        return response;
    }
}