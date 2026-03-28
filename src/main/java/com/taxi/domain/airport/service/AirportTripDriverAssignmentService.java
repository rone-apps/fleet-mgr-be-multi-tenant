package com.taxi.domain.airport.service;

import com.taxi.domain.airport.model.AirportTrip;
import com.taxi.domain.airport.model.AirportTripDriver;
import com.taxi.domain.airport.repository.AirportTripDriverRepository;
import com.taxi.domain.shift.model.CabShift;
import com.taxi.domain.shift.model.DriverShift;
import com.taxi.domain.shift.model.ShiftOwnership;
import com.taxi.domain.shift.repository.CabShiftRepository;
import com.taxi.domain.shift.repository.DriverShiftRepository;
import com.taxi.domain.shift.repository.ShiftOwnershipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Assigns hourly airport trips to drivers based on driver_shifts logon/logoff times.
 *
 * Algorithm per hour with trips > 0:
 * 1. MIDPOINT: find driver whose shift covers hour:30
 * 2. CLOSEST: if no midpoint match, find shift with closest logon/logoff to the hour window
 * 3. OWNER_FALLBACK: if no driver shifts at all, assign to the cab/shift owner
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AirportTripDriverAssignmentService {

    private final DriverShiftRepository driverShiftRepository;
    private final AirportTripDriverRepository airportTripDriverRepository;
    private final CabShiftRepository cabShiftRepository;
    private final ShiftOwnershipRepository shiftOwnershipRepository;

    /**
     * Assign all hourly trips for an AirportTrip to drivers.
     * Call this after saving/updating an AirportTrip during CSV import.
     */
    @Transactional
    public List<AirportTripDriver> assignDrivers(AirportTrip airportTrip) {
        String cabNumber = airportTrip.getCabNumber();
        LocalDate tripDate = airportTrip.getTripDate();

        log.debug("Assigning drivers for cab {} on {}", cabNumber, tripDate);

        // Delete any existing assignments for this trip (supports re-import)
        if (airportTripDriverRepository.existsByAirportTripId(airportTrip.getId())) {
            airportTripDriverRepository.deleteByAirportTripId(airportTrip.getId());
        }

        // Get all driver shifts for this cab on this date
        List<DriverShift> driverShifts = driverShiftRepository.findByCabNumberAndDateRange(
                cabNumber, tripDate, tripDate);

        // Find owner as fallback
        String ownerDriverNumber = findOwnerDriverNumber(cabNumber, tripDate);

        List<AirportTripDriver> assignments = new ArrayList<>();
        int totalAssigned = 0;

        for (int hour = 0; hour <= 23; hour++) {
            int tripCount = airportTrip.getTripsByHour(hour);
            if (tripCount <= 0) continue;

            // Try to assign this hour's trips
            DriverAssignment assignment = resolveDriver(hour, driverShifts, ownerDriverNumber);

            if (assignment != null) {
                AirportTripDriver atd = AirportTripDriver.builder()
                        .airportTrip(airportTrip)
                        .cabNumber(cabNumber)
                        .driverNumber(assignment.driverNumber)
                        .tripDate(tripDate)
                        .hour(hour)
                        .tripCount(tripCount)
                        .assignmentMethod(assignment.method)
                        .build();

                assignments.add(atd);
                totalAssigned += tripCount;
            }
        }

        // Bulk save
        if (!assignments.isEmpty()) {
            // Set totalDailyTrips on each record for convenience
            for (AirportTripDriver atd : assignments) {
                atd.setTotalDailyTrips(totalAssigned);
            }
            airportTripDriverRepository.saveAll(assignments);
        }

        log.debug("Assigned {} trips across {} hour-slots for cab {} on {}",
                totalAssigned, assignments.size(), cabNumber, tripDate);

        return assignments;
    }

    /**
     * Core assignment logic for a single hour.
     */
    private DriverAssignment resolveDriver(int hour, List<DriverShift> driverShifts, String ownerDriverNumber) {
        if (driverShifts.isEmpty()) {
            // No driver shifts at all — fall back to owner
            if (ownerDriverNumber != null) {
                return new DriverAssignment(ownerDriverNumber, AirportTripDriver.AssignmentMethod.OWNER_FALLBACK);
            }
            return null;
        }

        // 1. MIDPOINT: check who was driving at hour:30
        LocalDateTime midpoint = LocalDateTime.of(LocalDate.EPOCH, LocalTime.of(hour, 30));

        for (DriverShift ds : driverShifts) {
            if (ds.getLogonTime() == null || ds.getLogoffTime() == null) continue;

            LocalDateTime logon = ds.getLogonTime();
            LocalDateTime logoff = ds.getLogoffTime();

            // Compare using time-of-day on the trip date
            // DriverShift logon/logoff are full timestamps; extract time portion
            LocalTime logonTime = logon.toLocalTime();
            LocalTime logoffTime = logoff.toLocalTime();

            // Handle shifts that span midnight
            boolean spansMidnight = logoff.toLocalDate().isAfter(logon.toLocalDate());

            LocalTime midpointTime = LocalTime.of(hour, 30);

            if (spansMidnight) {
                // Shift spans midnight: logon 22:00 → logoff 06:00 next day
                // Hour is covered if: hour:30 >= logonTime OR hour:30 <= logoffTime
                if (!midpointTime.isBefore(logonTime) || !midpointTime.isAfter(logoffTime)) {
                    return new DriverAssignment(ds.getDriverNumber(), AirportTripDriver.AssignmentMethod.MIDPOINT);
                }
            } else {
                // Normal shift within same day
                if (!midpointTime.isBefore(logonTime) && !midpointTime.isAfter(logoffTime)) {
                    return new DriverAssignment(ds.getDriverNumber(), AirportTripDriver.AssignmentMethod.MIDPOINT);
                }
            }
        }

        // 2. CLOSEST: find shift with minimum time distance to the hour window
        LocalTime hourStart = LocalTime.of(hour, 0);
        LocalTime hourEnd = LocalTime.of(hour, 59);

        DriverShift closestShift = null;
        long minDistanceMinutes = Long.MAX_VALUE;

        for (DriverShift ds : driverShifts) {
            if (ds.getLogonTime() == null || ds.getLogoffTime() == null) continue;

            LocalTime logonTime = ds.getLogonTime().toLocalTime();
            LocalTime logoffTime = ds.getLogoffTime().toLocalTime();

            // Calculate distance from this shift to the hour window
            long distance = calculateDistanceMinutes(logonTime, logoffTime, hourStart, hourEnd,
                    ds.getLogoffTime().toLocalDate().isAfter(ds.getLogonTime().toLocalDate()));

            if (distance < minDistanceMinutes) {
                minDistanceMinutes = distance;
                closestShift = ds;
            }
        }

        if (closestShift != null) {
            return new DriverAssignment(closestShift.getDriverNumber(), AirportTripDriver.AssignmentMethod.CLOSEST);
        }

        // 3. OWNER_FALLBACK
        if (ownerDriverNumber != null) {
            return new DriverAssignment(ownerDriverNumber, AirportTripDriver.AssignmentMethod.OWNER_FALLBACK);
        }

        return null;
    }

    /**
     * Calculate the time distance in minutes between a shift and an hour window.
     * Returns 0 if the shift overlaps the window.
     */
    private long calculateDistanceMinutes(LocalTime logonTime, LocalTime logoffTime,
                                           LocalTime hourStart, LocalTime hourEnd,
                                           boolean spansMidnight) {
        if (spansMidnight) {
            // For midnight-spanning shifts, check if hour is in either segment
            // Segment 1: logon → 23:59, Segment 2: 00:00 → logoff
            if (!hourStart.isBefore(logonTime) || !hourEnd.isAfter(logoffTime)) {
                return 0; // overlaps
            }
            // Distance to nearest edge
            long distToLogoff = minutesBetween(logoffTime, hourStart);
            long distToLogon = minutesBetween(hourEnd, logonTime);
            return Math.min(distToLogoff, distToLogon);
        }

        // Normal shift — check overlap
        if (!logonTime.isAfter(hourEnd) && !logoffTime.isBefore(hourStart)) {
            return 0; // overlaps
        }

        // Shift ends before hour starts
        if (logoffTime.isBefore(hourStart)) {
            return minutesBetween(logoffTime, hourStart);
        }

        // Shift starts after hour ends
        if (logonTime.isAfter(hourEnd)) {
            return minutesBetween(hourEnd, logonTime);
        }

        return Long.MAX_VALUE;
    }

    private long minutesBetween(LocalTime from, LocalTime to) {
        return Math.abs(Duration.between(from, to).toMinutes());
    }

    /**
     * Find the owner's driver number for a cab on a given date.
     * Looks up CabShift → ShiftOwnership → Driver.driverNumber
     */
    private String findOwnerDriverNumber(String cabNumber, LocalDate date) {
        try {
            List<CabShift> cabShifts = cabShiftRepository.findByCabNumber(cabNumber);
            for (CabShift cabShift : cabShifts) {
                Optional<ShiftOwnership> ownership = shiftOwnershipRepository.findCurrentOwnership(cabShift.getId());
                if (ownership.isPresent() && ownership.get().getOwner() != null) {
                    String driverNumber = ownership.get().getOwner().getDriverNumber();
                    if (driverNumber != null) {
                        return driverNumber;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not find owner for cab {}: {}", cabNumber, e.getMessage());
        }
        return null;
    }

    private record DriverAssignment(String driverNumber, AirportTripDriver.AssignmentMethod method) {}
}
