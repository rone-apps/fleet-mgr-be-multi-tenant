package com.taxi.domain.expense.service;

import com.taxi.domain.cab.model.Cab;
import com.taxi.domain.cab.model.CabAttributeValue;
import com.taxi.domain.cab.repository.CabAttributeValueRepository;
import com.taxi.domain.cab.repository.CabRepository;
import com.taxi.domain.expense.model.MatchingCriteria;
import com.taxi.domain.expense.model.MatchingCriteria.DynamicAttributeRule;
import com.taxi.domain.shift.model.CabShift;
import com.taxi.domain.shift.repository.CabShiftRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CabMatchingService - Core logic to find cabs matching attribute criteria
 *
 * REFACTORED (Phase 1): Now matches based on shift-level attributes instead of cab-level
 * Supports both static attributes (shareType, cabType, airportLicense, etc.)
 * and dynamic attributes (custom cab attributes with temporal tracking)
 *
 * Key Change: Attributes are now checked at the shift level.
 * Returns cabs whose shifts match the criteria (at least one matching shift).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CabMatchingService {

    private final CabRepository cabRepository;
    private final CabShiftRepository cabShiftRepository;
    private final CabAttributeValueRepository cabAttributeValueRepository;

    /**
     * Find all cabs matching the given criteria
     *
     * REFACTORED: Now finds cabs whose shifts match the criteria
     * Returns a cab if ANY of its shifts match the criteria.
     */
    public List<Cab> findMatchingCabs(MatchingCriteria criteria) {
        log.info("Finding cabs matching criteria (shift-level attributes): {}", criteria);

        // Get all cabs and their shifts
        List<Cab> allCabs = cabRepository.findAll();

        // Filter cabs that have at least one shift matching criteria
        return allCabs.stream()
            .filter(cab -> cabHasMatchingShift(cab, criteria, LocalDate.now()))
            .collect(Collectors.toList());
    }

    /**
     * Check if a cab has at least one matching shift
     *
     * REFACTORED: Now checks shifts instead of cab attributes
     */
    public boolean cabHasMatchingShift(Cab cab, MatchingCriteria criteria, LocalDate date) {
        List<CabShift> shifts = cabShiftRepository.findByCab(cab);
        return shifts.stream()
            .anyMatch(shift -> shiftMatchesCriteria(shift, criteria, date));
    }

    /**
     * Find all shifts matching the given criteria
     *
     * REFACTORED: New method to find matching shifts directly
     */
    public List<CabShift> findMatchingShifts(MatchingCriteria criteria) {
        log.info("Finding shifts matching criteria: {}", criteria);

        List<CabShift> allShifts = cabShiftRepository.findAll();

        return allShifts.stream()
            .filter(shift -> shiftMatchesCriteria(shift, criteria, LocalDate.now()))
            .collect(Collectors.toList());
    }

    /**
     * Check if a shift matches criteria
     *
     * REFACTORED: Now validates shift-level attributes
     */
    public boolean shiftMatchesCriteria(CabShift shift, MatchingCriteria criteria, LocalDate date) {
        // Validate shift-level static attributes
        if (!validateShiftStaticAttributes(shift, criteria)) {
            return false;
        }

        // Validate dynamic attributes (cab-level, requires DB query)
        if (!validateDynamicAttributes(shift.getCab(), criteria, date)) {
            return false;
        }

        return true;
    }

    /**
     * Get preview of matching cabs with count
     */
    public MatchingCabsPreview getMatchingPreview(MatchingCriteria criteria) {
        List<Cab> matchingCabs = findMatchingCabs(criteria);

        // Return first 10 + total count
        List<Cab> sampleCabs = matchingCabs.stream()
            .limit(10)
            .collect(Collectors.toList());

        return new MatchingCabsPreview(
            matchingCabs.size(),
            sampleCabs,
            matchingCabs.size() > 10
        );
    }

    /**
     * Validate static shift attributes against criteria
     *
     * REFACTORED: Now validates shift-level attributes instead of cab-level
     * Attributes: cabType, shareType, airportLicense (all now at shift level)
     */
    private boolean validateShiftStaticAttributes(CabShift shift, MatchingCriteria criteria) {
        // Share type check (now at shift level)
        if (criteria.getShareType() != null) {
            MatchingCriteria.ShareTypeRule rule = criteria.getShareType();
            boolean matches = shift.getShareType() == rule.getValue();
            if (rule.getNegate() != null && rule.getNegate()) {
                matches = !matches;
            }
            if (!matches) {
                return false;
            }
        }

        // Airport license check (now at shift level)
        if (criteria.getHasAirportLicense() != null) {
            if (!criteria.getHasAirportLicense().equals(shift.getHasAirportLicense())) {
                return false;
            }
        }

        // Cab type check (now at shift level)
        if (criteria.getCabType() != null) {
            MatchingCriteria.CabTypeRule rule = criteria.getCabType();
            boolean matches = shift.getCabType() == rule.getValue();
            if (rule.getNegate() != null && rule.getNegate()) {
                matches = !matches;
            }
            if (!matches) {
                return false;
            }
        }

        // NOTE: CabShiftType and Status checks removed (no longer applicable)
        // - cabShiftType: Every cab now has exactly 2 shifts (DAY and NIGHT)
        // - status: Status is now tracked per-shift via shift_status_history
        //           Use ShiftStatusService to filter by historical active status if needed
        if (criteria.getCabShiftType() != null) {
            log.warn("Criteria contains cabShiftType which is no longer applicable. " +
                    "Every cab now has exactly 2 shifts (DAY and NIGHT). Ignoring this criterion.");
        }

        if (criteria.getStatus() != null) {
            log.warn("Criteria contains cab status which is no longer applicable at cab level. " +
                    "Status is now tracked per-shift. Use ShiftStatusService for historical status queries.");
        }

        return true;
    }

    /**
     * Validate dynamic attributes with temporal awareness
     */
    private boolean validateDynamicAttributes(Cab cab, MatchingCriteria criteria, LocalDate date) {
        if (criteria.getDynamicAttributes() == null || criteria.getDynamicAttributes().isEmpty()) {
            return true;
        }

        // Get all cab attributes active on the specified date
        List<CabAttributeValue> cabAttributes = cabAttributeValueRepository
            .findByCabAndActiveOn(cab, date);

        // Check each dynamic attribute rule
        for (DynamicAttributeRule rule : criteria.getDynamicAttributes()) {
            boolean hasAttribute = cabAttributes.stream()
                .anyMatch(attr -> attr.getAttributeType().getId().equals(rule.getAttributeTypeId())
                    && (rule.getExpectedValue() == null ||
                        rule.getExpectedValue().equals(attr.getAttributeValue())));

            // Check if attribute requirement is met
            if (rule.getMustHave() != null && rule.getMustHave() && !hasAttribute) {
                return false; // Required attribute missing
            }
            if (rule.getMustHave() != null && !rule.getMustHave() && hasAttribute) {
                return false; // Excluded attribute present
            }
        }

        return true;
    }

    /**
     * Preview of matching cabs
     */
    public static class MatchingCabsPreview {
        public final int totalCount;
        public final List<Cab> sampleCabs;
        public final boolean hasMore;

        public MatchingCabsPreview(int totalCount, List<Cab> sampleCabs, boolean hasMore) {
            this.totalCount = totalCount;
            this.sampleCabs = sampleCabs;
            this.hasMore = hasMore;
        }
    }
}
