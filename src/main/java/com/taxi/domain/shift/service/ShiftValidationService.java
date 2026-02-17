package com.taxi.domain.shift.service;

import com.taxi.domain.cab.model.Cab;
import com.taxi.domain.shift.model.CabShift;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * SINGLE SOURCE OF TRUTH: SHIFT AND CAB VALIDATION
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * Centralized service for determining if a cab or shift is "active"
 *
 * BUSINESS RULES:
 * - A cab is active if it has at least one active shift
 * - A shift is active if its status is ACTIVE
 * - A cab+shift combination is active only if BOTH are active
 *
 * All financial calculations use these methods to filter active entities
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShiftValidationService {

    /**
     * Determines if a cab is active (has at least one active shift)
     */
    public boolean isCabActive(Cab cab) {
        if (cab == null || cab.getShifts() == null) {
            return false;
        }
        return cab.getShifts().stream()
                .anyMatch(shift -> shift.getStatus() == CabShift.ShiftStatus.ACTIVE);
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
     */
    public boolean isCabShiftActive(CabShift cabShift) {
        if (cabShift == null || cabShift.getCab() == null) {
            return false;
        }
        return isCabActive(cabShift.getCab()) && isShiftActive(cabShift);
    }
}
