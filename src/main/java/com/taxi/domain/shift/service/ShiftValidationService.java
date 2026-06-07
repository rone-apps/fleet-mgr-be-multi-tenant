package com.taxi.domain.shift.service;

import com.taxi.domain.cab.model.Cab;
import com.taxi.domain.cab.model.CabStatus;
import com.taxi.domain.shift.model.CabShift;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * SINGLE SOURCE OF TRUTH: SHIFT AND CAB VALIDATION
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * Centralized service for determining if a cab or shift is "active"
 *
 * BUSINESS RULES:
 * - A cab is active if its status is ACTIVE and it has at least one active shift
 * - A shift is active if its status is ACTIVE
 * - A cab+shift combination is active only if BOTH the cab status AND shift status are ACTIVE
 *
 * All financial calculations use these methods to filter active entities
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShiftValidationService {

    /**
     * Determines if a cab is active (cab-level status is ACTIVE and has at least one active shift)
     * This checks CURRENT status, not historical status.
     */
    public boolean isCabActive(Cab cab) {
        if (cab == null) {
            return false;
        }
        // Check cab-level status first
        if (cab.getStatus() != null && cab.getStatus() != CabStatus.ACTIVE) {
            return false;
        }
        if (cab.getShifts() == null) {
            return false;
        }
        return cab.getShifts().stream()
                .anyMatch(shift -> shift.getStatus() == CabShift.ShiftStatus.ACTIVE);
    }

    /**
     * Determines if a cab was active on a specific date
     * Uses deactivatedDate to check historical status
     *
     * @param cab The cab to check
     * @param date The date to check (typically within report period)
     * @return true if cab was active on the given date
     */
    public boolean wasCabActiveOnDate(Cab cab, LocalDate date) {
        if (cab == null || date == null) {
            return false;
        }

        // Check if cab was added before the check date
        if (cab.getFleetAddedDate() != null && date.isBefore(cab.getFleetAddedDate())) {
            return false; // Cab wasn't in fleet yet
        }

        // Check if cab was deactivated before the check date
        if (cab.getDeactivatedDate() != null && !date.isBefore(cab.getDeactivatedDate())) {
            return false; // Cab was already deactivated
        }

        // Cab was in active status on this date
        return true;
    }

    /**
     * Determines if a specific shift is active
     */
    public boolean isShiftActive(CabShift shift) {
        if (shift == null) {
            return false;
        }
        return shift.getStatus() == CabShift.ShiftStatus.ACTIVE;
    }

    /**
     * Determines if both the cab AND shift are active
     * Used for financial calculations that apply to specific shifts
     * This checks CURRENT status, not historical status.
     */
    public boolean isCabShiftActive(CabShift cabShift) {
        if (cabShift == null || cabShift.getCab() == null) {
            return false;
        }
        return isCabActive(cabShift.getCab()) && isShiftActive(cabShift);
    }

    /**
     * Determines if both the cab AND shift were active during a date range
     * Used for historical reports - includes deactivated cabs/shifts if they were active during the period
     *
     * @param cabShift The cab shift to check
     * @param periodStart Start of the report period
     * @param periodEnd End of the report period
     * @return true if the cab/shift was active for any part of the period
     */
    public boolean wasActiveDuringPeriod(CabShift cabShift, LocalDate periodStart, LocalDate periodEnd) {
        if (cabShift == null || cabShift.getCab() == null || periodStart == null || periodEnd == null) {
            return false;
        }

        Cab cab = cabShift.getCab();

        // Check if cab was added after the period ended
        if (cab.getFleetAddedDate() != null && cab.getFleetAddedDate().isAfter(periodEnd)) {
            return false; // Cab wasn't in fleet during this period
        }

        // Check if cab was deactivated before the period started
        if (cab.getDeactivatedDate() != null && cab.getDeactivatedDate().isBefore(periodStart)) {
            return false; // Cab was already deactivated before period
        }

        // For now, we're checking cab-level deactivation
        // Shift-level status history is tracked in shift_status_history table
        // TODO: Could enhance to check shift_status_history for precise shift activation dates
        return true;
    }
}
