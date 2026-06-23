package com.taxi.domain.lease.service;

import com.taxi.domain.lease.model.LeaseRateOverride;
import com.taxi.domain.lease.repository.LeaseRateOverrideRepository;
import com.taxi.domain.driver.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing custom lease rate overrides
 *
 * Allows cab owners to set custom rates that override default lease rates.
 * Also supports driver-specific (beneficiary) overrides for exemptions and arrangements.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LeaseRateOverrideService {

    private final LeaseRateOverrideRepository leaseRateOverrideRepository;
    private final DriverRepository driverRepository;

    /**
     * Result object for lease rate override lookup
     * Contains both base and mileage components to support structured rates
     */
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class OverrideRateResult {
        private BigDecimal baseRate;
        private BigDecimal mileageRate;
        private boolean isStructured; // true = base+mileage, false = flat rate
        private Long overrideId;

        /**
         * Create a flat rate result (no mileage component)
         */
        public static OverrideRateResult flatRate(BigDecimal totalRate, Long overrideId) {
            return OverrideRateResult.builder()
                .baseRate(totalRate)
                .mileageRate(BigDecimal.ZERO)
                .isStructured(false)
                .overrideId(overrideId)
                .build();
        }

        /**
         * Create a structured rate result (base + mileage)
         */
        public static OverrideRateResult structured(BigDecimal baseRate, BigDecimal mileageRate, Long overrideId) {
            return OverrideRateResult.builder()
                .baseRate(baseRate)
                .mileageRate(mileageRate)
                .isStructured(true)
                .overrideId(overrideId)
                .build();
        }
    }

    /**
     * Get the applicable lease rate for a specific shift
     *
     * Logic (two-tier priority):
     * 1. Check for driver-specific (beneficiary) overrides first
     *    (e.g., "Owner A grants Driver B $0 on this shift")
     * 2. If no beneficiary override, check for owner-level overrides
     *    (e.g., "Owner A sets $50 for all drivers on this shift")
     * 3. Return the first (highest priority) match found
     * 4. If no override found, return null (caller should use default rate)
     *
     * @param ownerDriverNumber The cab owner
     * @param workingDriverNumber The driver actually working the shift (for beneficiary check)
     * @param cabNumber The cab number
     * @param shiftType "DAY" or "NIGHT"
     * @param date The date of the shift
     * @return OverrideRateResult with base and mileage components, or null if no override
     */
    @Transactional(readOnly = true)
    public OverrideRateResult getApplicableLeaseRate(
            String ownerDriverNumber,
            String workingDriverNumber,
            String cabNumber,
            String shiftType,
            LocalDate date) {

        if (ownerDriverNumber == null || ownerDriverNumber.isEmpty() ||
            cabNumber == null || cabNumber.isEmpty() ||
            shiftType == null || shiftType.isEmpty() ||
            date == null) {
            log.warn("Invalid parameters for lease rate lookup: owner={}, driver={}, cab={}, shift={}, date={}",
                ownerDriverNumber, workingDriverNumber, cabNumber, shiftType, date);
            return null;
        }

        DayOfWeek dayOfWeek = date.getDayOfWeek();
        String dayOfWeekStr = dayOfWeek.toString();

        log.debug("Searching for lease rate: owner='{}', driver='{}', cab='{}', shift='{}', date='{}'",
            ownerDriverNumber, workingDriverNumber, cabNumber, shiftType, date);

        // ✅ STEP 1: Check for driver-specific (beneficiary) overrides FIRST
        if (workingDriverNumber != null && !workingDriverNumber.isEmpty()) {
            List<LeaseRateOverride> beneficiaryOverrides = leaseRateOverrideRepository.findBeneficiaryOverrides(
                ownerDriverNumber, workingDriverNumber, cabNumber, shiftType, dayOfWeekStr, date);

            if (!beneficiaryOverrides.isEmpty()) {
                LeaseRateOverride matched = beneficiaryOverrides.get(0);
                log.debug("Beneficiary override found: id={}, beneficiary={}, structured={}, priority={}",
                    matched.getId(), matched.getBeneficiaryDriverNumber(), matched.isStructuredMode(), matched.getPriority());
                return convertToResult(matched);
            }
        }

        // ✅ STEP 2: Check for owner-level (non-beneficiary) overrides
        List<LeaseRateOverride> allOwnerOverrides = leaseRateOverrideRepository
            .findByOwnerDriverNumberOrderByPriorityDescCreatedAtDesc(ownerDriverNumber);

        if (allOwnerOverrides.isEmpty()) {
            log.debug("No overrides found for owner: '{}'", ownerDriverNumber);
            return null;
        }

        log.debug("Found {} total overrides for owner '{}', checking for owner-level matches",
            allOwnerOverrides.size(), ownerDriverNumber);

        for (LeaseRateOverride override : allOwnerOverrides) {
            // Skip beneficiary-specific overrides (already checked in STEP 1)
            if (override.getBeneficiaryDriverNumber() != null && !override.getBeneficiaryDriverNumber().isEmpty()) {
                log.debug("  Override {}: beneficiary-specific, skip in owner-level search", override.getId());
                continue;
            }

            log.info("  Checking Override {}...", override.getId());

            // Check 1: Is active flag
            if (!override.getIsActive()) {
                log.info("    ✗ Check 1 (Active): isActive={} - SKIP", override.getIsActive());
                continue;
            }
            log.info("    ✓ Check 1 (Active): isActive=true");

            // Check 2: Date range validation
            if (date.isBefore(override.getStartDate())) {
                log.info("    ✗ Check 2 (Date Range): requested date {} is BEFORE startDate {} - SKIP",
                    date, override.getStartDate());
                continue;
            }
            log.info("    ✓ Check 2a (Date Range): requested date {} is >= startDate {}", date, override.getStartDate());

            if (override.getEndDate() != null && date.isAfter(override.getEndDate())) {
                log.info("    ✗ Check 2 (Date Range): requested date {} is AFTER endDate {} - SKIP",
                    date, override.getEndDate());
                continue;
            }
            if (override.getEndDate() != null) {
                log.info("    ✓ Check 2b (Date Range): requested date {} is <= endDate {}", date, override.getEndDate());
            } else {
                log.info("    ✓ Check 2b (Date Range): endDate is NULL (no expiry)");
            }

            // Check 3: Cab number match
            if (override.getCabNumber() != null && !override.getCabNumber().isEmpty()) {
                if (!override.getCabNumber().equalsIgnoreCase(cabNumber)) {
                    log.info("    ✗ Check 3 (Cab): override cab='{}' does NOT match requested cab='{}' - SKIP",
                        override.getCabNumber(), cabNumber);
                    continue;
                }
                log.info("    ✓ Check 3 (Cab): override cab='{}' matches requested cab='{}'",
                    override.getCabNumber(), cabNumber);
            } else {
                log.info("    ✓ Check 3 (Cab): override cabNumber is NULL (matches all cabs)");
            }

            // Check 4: Shift type match
            if (override.getShiftType() != null && !override.getShiftType().isEmpty()) {
                if (!override.getShiftType().equalsIgnoreCase(shiftType)) {
                    log.info("    ✗ Check 4 (Shift): override shift='{}' does NOT match requested shift='{}' - SKIP",
                        override.getShiftType(), shiftType);
                    continue;
                }
                log.info("    ✓ Check 4 (Shift): override shift='{}' matches requested shift='{}'",
                    override.getShiftType(), shiftType);
            } else {
                log.info("    ✓ Check 4 (Shift): override shiftType is NULL (matches all shifts)");
            }

            // Check 5: Day of week match
            if (override.getDayOfWeek() != null && !override.getDayOfWeek().isEmpty()) {
                if (!override.getDayOfWeek().equalsIgnoreCase(dayOfWeekStr)) {
                    log.info("    ✗ Check 5 (Day): override day='{}' does NOT match requested day='{}' - SKIP",
                        override.getDayOfWeek(), dayOfWeekStr);
                    continue;
                }
                log.info("    ✓ Check 5 (Day): override day='{}' matches requested day='{}'",
                    override.getDayOfWeek(), dayOfWeekStr);
            } else {
                log.info("    ✓ Check 5 (Day): override dayOfWeek is NULL (matches all days)");
            }

            // All checks passed! This is the highest priority match
            log.info("✓✓✓ MATCH FOUND! Override id={}, structured={}, priority={}, cab={}, shift={}, day={}",
                override.getId(), override.isStructuredMode(), override.getPriority(),
                override.getCabNumber() != null ? override.getCabNumber() : "ALL",
                override.getShiftType() != null ? override.getShiftType() : "ALL",
                override.getDayOfWeek() != null ? override.getDayOfWeek() : "ALL");
            log.info("=== LEASE RATE LOOKUP END (OWNER-LEVEL MATCH) ===");

            return convertToResult(override);
        }

        log.info("✗✗✗ NO MATCHING OVERRIDE FOUND after checking all {} overrides for owner='{}', driver='{}', cab='{}', shift='{}', day='{}'",
            allOwnerOverrides.size(), ownerDriverNumber, workingDriverNumber, cabNumber, shiftType, dayOfWeekStr);
        log.info("=== LEASE RATE LOOKUP END (NOT FOUND) ===");
        return null;
    }

    /**
     * Convert LeaseRateOverride entity to result object
     */
    private OverrideRateResult convertToResult(LeaseRateOverride override) {
        if (override.isStructuredMode()) {
            log.debug("Converting override {} to STRUCTURED result: base={}, mileage={}",
                override.getId(), override.getBaseRateOverride(), override.getMileageRateOverride());
            return OverrideRateResult.structured(
                override.getBaseRateOverride(),
                override.getMileageRateOverride(),
                override.getId()
            );
        } else {
            log.debug("Converting override {} to FLAT RATE result: total={}",
                override.getId(), override.getLeaseRate());
            return OverrideRateResult.flatRate(
                override.getLeaseRate(),
                override.getId()
            );
        }
    }

    /**
     * Create a new lease rate override
     */
    @Transactional
    public LeaseRateOverride createOverride(LeaseRateOverride override) {
        String rateInfo = override.isStructuredMode()
            ? String.format("base=%s, mileage=%s", override.getBaseRateOverride(), override.getMileageRateOverride())
            : String.format("flat=%s", override.getLeaseRate());

        log.info("Creating lease rate override: owner={}, beneficiary={}, cab={}, shift={}, day={}, {}",
            override.getOwnerDriverNumber(),
            override.getBeneficiaryDriverNumber() != null ? override.getBeneficiaryDriverNumber() : "ALL",
            override.getCabNumber() != null ? override.getCabNumber() : "ALL",
            override.getShiftType() != null ? override.getShiftType() : "ALL",
            override.getDayOfWeek() != null ? override.getDayOfWeek() : "ALL",
            rateInfo);

        // Validate override configuration
        override.validate();

        // Validate owner exists and is actually an owner
        validateOwner(override.getOwnerDriverNumber());

        // Auto-calculate priority if not set
        if (override.getPriority() == null || override.getPriority() == 0) {
            override.calculatePriority();
        }

        return leaseRateOverrideRepository.save(override);
    }

    /**
     * Update an existing override
     */
    @Transactional
    public LeaseRateOverride updateOverride(Long id, LeaseRateOverride updates) {
        LeaseRateOverride existing = leaseRateOverrideRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Override not found: " + id));

        if (updates.getOwnerDriverNumber() != null)
            existing.setOwnerDriverNumber(updates.getOwnerDriverNumber());
        if (updates.getBeneficiaryDriverNumber() != null)
            existing.setBeneficiaryDriverNumber(updates.getBeneficiaryDriverNumber());
        if (updates.getCabNumber() != null)
            existing.setCabNumber(updates.getCabNumber());
        if (updates.getShiftType() != null)
            existing.setShiftType(updates.getShiftType());
        if (updates.getDayOfWeek() != null)
            existing.setDayOfWeek(updates.getDayOfWeek());

        // ALWAYS update rate fields (including null) to support mode switching
        // When switching from flat to structured (or vice versa), we MUST clear the opposing fields
        existing.setLeaseRate(updates.getLeaseRate());
        existing.setBaseRateOverride(updates.getBaseRateOverride());
        existing.setMileageRateOverride(updates.getMileageRateOverride());

        if (updates.getStartDate() != null)
            existing.setStartDate(updates.getStartDate());
        if (updates.getEndDate() != null)
            existing.setEndDate(updates.getEndDate());
        if (updates.getIsActive() != null)
            existing.setIsActive(updates.getIsActive());
        if (updates.getNotes() != null)
            existing.setNotes(updates.getNotes());

        // Validate updated override
        existing.validate();

        existing.calculatePriority();
        return leaseRateOverrideRepository.save(existing);
    }

    /**
     * Delete an override
     */
    @Transactional
    public void deleteOverride(Long id) {
        leaseRateOverrideRepository.deleteById(id);
    }

    /**
     * Deactivate an override (soft delete)
     */
    @Transactional
    public LeaseRateOverride deactivateOverride(Long id) {
        LeaseRateOverride override = leaseRateOverrideRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Override not found: " + id));
        override.setIsActive(false);
        return leaseRateOverrideRepository.save(override);
    }

    /**
     * Activate an override
     */
    @Transactional
    public LeaseRateOverride activateOverride(Long id) {
        LeaseRateOverride override = leaseRateOverrideRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Override not found: " + id));
        override.setIsActive(true);
        return leaseRateOverrideRepository.save(override);
    }

    /**
     * Get all overrides for an owner
     */
    @Transactional(readOnly = true)
    public List<LeaseRateOverride> getOwnerOverrides(String ownerDriverNumber) {
        return leaseRateOverrideRepository.findByOwnerDriverNumberOrderByPriorityDescCreatedAtDesc(ownerDriverNumber);
    }

    /**
     * Get all active overrides
     */
    @Transactional(readOnly = true)
    public List<LeaseRateOverride> getAllActiveOverrides(LocalDate date) {
        return leaseRateOverrideRepository.findAllActiveOverrides(date);
    }

    /**
     * Get all overrides
     */
    @Transactional(readOnly = true)
    public List<LeaseRateOverride> getAllOverrides() {
        return leaseRateOverrideRepository.findAllByOrderByOwnerDriverNumberAscPriorityDescCreatedAtDesc();
    }

    /**
     * Get overrides expiring soon
     */
    @Transactional(readOnly = true)
    public List<LeaseRateOverride> getExpiringSoon(LocalDate startDate, LocalDate endDate) {
        return leaseRateOverrideRepository.findExpiringSoon(startDate, endDate);
    }

    /**
     * Create multiple overrides in bulk
     */
    @Transactional
    public List<LeaseRateOverride> createBulkOverrides(
            String ownerDriverNumber,
            String cabNumber,
            String shiftType,
            List<String> daysOfWeek,
            java.math.BigDecimal leaseRate,
            LocalDate startDate,
            LocalDate endDate,
            String notes) {

        validateOwner(ownerDriverNumber);

        java.util.List<LeaseRateOverride> overrides = new java.util.ArrayList<>();

        for (String day : daysOfWeek) {
            LeaseRateOverride override = LeaseRateOverride.builder()
                .ownerDriverNumber(ownerDriverNumber)
                .beneficiaryDriverNumber(null)
                .cabNumber(cabNumber)
                .shiftType(shiftType)
                .dayOfWeek(day)
                .leaseRate(leaseRate)
                .startDate(startDate)
                .endDate(endDate)
                .isActive(true)
                .notes(notes)
                .build();

            override.calculatePriority();
            overrides.add(leaseRateOverrideRepository.save(override));
        }

        return overrides;
    }

    /**
     * Validate that a driver exists and is marked as an owner
     */
    private void validateOwner(String driverNumber) {
        Optional<com.taxi.domain.driver.model.Driver> driver = driverRepository.findByDriverNumber(driverNumber);
        if (driver.isEmpty()) {
            throw new IllegalArgumentException("Driver not found: " + driverNumber);
        }
        if (!driver.get().getIsOwner()) {
            throw new IllegalArgumentException("Driver is not marked as an owner: " + driverNumber);
        }
    }
}
