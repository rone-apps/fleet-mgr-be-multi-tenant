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
 * Allows cab owners to set custom rates that override default lease rates
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LeaseRateOverrideService {

    private final LeaseRateOverrideRepository leaseRateOverrideRepository;
    private final DriverRepository driverRepository;

    /**
     * Get the applicable lease rate for a specific shift
     *
     * Logic:
     * 1. Find all matching overrides for the owner/cab/shift/day combination
     * 2. First check if the override is active in the date range, then check owner (its always there), then check if cab number,
     *  if null, then all cabs for this owner, and then shift, if shift defined then for that shfit, if shift null then both shifts.
     * the check day of week, if null then all days.
     * 2. Return the highest priority override's rate
     * 3. If no override found, return null (caller should use default rate)
     *
     * @param ownerDriverNumber The cab owner
     * @param cabNumber The cab number
     * @param shiftType "DAY" or "NIGHT"
     * @param date The date of the shift
     * @return Custom lease rate if override exists, null otherwise
     */
    @Transactional(readOnly = true)
    public BigDecimal getApplicableLeaseRate(
            String ownerDriverNumber,
            String cabNumber,
            String shiftType,
            LocalDate date) {

        if (ownerDriverNumber == null || ownerDriverNumber.isEmpty() ||
            cabNumber == null || cabNumber.isEmpty() ||
            shiftType == null || shiftType.isEmpty() ||
            date == null) {
            log.warn("Invalid parameters for lease rate lookup: owner={}, cab={}, shift={}, date={}",
                ownerDriverNumber, cabNumber, shiftType, date);
            return null;
        }

        // Get day of week
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        String dayOfWeekStr = dayOfWeek.toString(); // "MONDAY", "TUESDAY", etc.

        log.info("=== LEASE RATE LOOKUP START ===");
        log.info("Searching for: owner='{}', cab='{}', shift='{}', date='{}' ({})",
            ownerDriverNumber, cabNumber, shiftType, date, dayOfWeekStr);

        // Strategy: Get all overrides for the owner and filter manually
        // This ensures we can properly handle null/wildcard fields
        List<LeaseRateOverride> allOwnerOverrides = leaseRateOverrideRepository
            .findByOwnerDriverNumberOrderByPriorityDescCreatedAtDesc(ownerDriverNumber);

        if (allOwnerOverrides.isEmpty()) {
            log.warn("No overrides found for owner: '{}'", ownerDriverNumber);
            return null;
        }

        log.info("Found {} total overrides for owner '{}'. Starting filter checks...", allOwnerOverrides.size(), ownerDriverNumber);
        for (LeaseRateOverride ov : allOwnerOverrides) {
            log.info("  Override {}: cab='{}', shift='{}', day='{}', rate={}, startDate='{}', endDate='{}', isActive={}",
                ov.getId(),
                ov.getCabNumber() != null ? ov.getCabNumber() : "NULL",
                ov.getShiftType() != null ? ov.getShiftType() : "NULL",
                ov.getDayOfWeek() != null ? ov.getDayOfWeek() : "NULL",
                ov.getLeaseRate(),
                ov.getStartDate(),
                ov.getEndDate() != null ? ov.getEndDate() : "NULL",
                ov.getIsActive());
        }

        // Filter overrides according to the business rules:
        // 1. Must be active (isActive = true)
        // 2. Must be active on the given date (startDate <= date AND (endDate IS NULL OR endDate >= date))
        // 3. Cab must match: either override.cabNumber is null (all cabs) OR override.cabNumber == cabNumber
        // 4. Shift must match: either override.shiftType is null (all shifts) OR override.shiftType == shiftType
        // 5. Day of week must match: either override.dayOfWeek is null (all days) OR override.dayOfWeek == dayOfWeek
        // Return the first one (already ordered by priority DESC)

        for (LeaseRateOverride override : allOwnerOverrides) {
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
            // Override matches if: cabNumber is null (all cabs) OR cabNumber matches exactly
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
            // Override matches if: shiftType is null (all shifts) OR shiftType matches exactly
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
            // Override matches if: dayOfWeek is null (all days) OR dayOfWeek matches exactly
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

            // All checks passed! This is the highest priority match (already sorted by priority DESC)
            log.info("✓✓✓ MATCH FOUND! Override id={}, rate={}, priority={}, cab={}, shift={}, day={}",
                override.getId(), override.getLeaseRate(), override.getPriority(),
                override.getCabNumber() != null ? override.getCabNumber() : "ALL",
                override.getShiftType() != null ? override.getShiftType() : "ALL",
                override.getDayOfWeek() != null ? override.getDayOfWeek() : "ALL");
            log.info("=== LEASE RATE LOOKUP END (FOUND) ===");

            return override.getLeaseRate();
        }

        log.warn("✗✗✗ NO MATCHING OVERRIDE FOUND after filtering all {} overrides for owner='{}', cab='{}', shift='{}', day='{}'",
            allOwnerOverrides.size(), ownerDriverNumber, cabNumber, shiftType, dayOfWeekStr);
        log.info("=== LEASE RATE LOOKUP END (NOT FOUND) ===");
        return null;
    }

    /**
     * Create a new lease rate override
     */
    @Transactional
    public LeaseRateOverride createOverride(LeaseRateOverride override) {
        log.info("Creating lease rate override: owner={}, cab={}, shift={}, day={}, rate={}", 
            override.getOwnerDriverNumber(), 
            override.getCabNumber() != null ? override.getCabNumber() : "ALL",
            override.getShiftType() != null ? override.getShiftType() : "ALL",
            override.getDayOfWeek() != null ? override.getDayOfWeek() : "ALL",
            override.getLeaseRate());
        
        // Validate owner exists and is actually an owner
        validateOwner(override.getOwnerDriverNumber());
        
        // Auto-calculate priority if not set
        if (override.getPriority() == null || override.getPriority() == 0) {
            override.calculatePriority();
        }
        
        // Set default values
        if (override.getIsActive() == null) {
            override.setIsActive(true);
        }
        
        if (override.getStartDate() == null) {
            override.setStartDate(LocalDate.now());
        }
        
        return leaseRateOverrideRepository.save(override);
    }

    /**
     * Update an existing override
     */
    @Transactional
    public LeaseRateOverride updateOverride(Long id, LeaseRateOverride updates) {
        LeaseRateOverride existing = leaseRateOverrideRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Override not found: " + id));
        
        log.info("Updating lease rate override: id={}", id);
        
        // Update fields
        if (updates.getCabNumber() != null) {
            existing.setCabNumber(updates.getCabNumber());
        }
        if (updates.getShiftType() != null) {
            existing.setShiftType(updates.getShiftType());
        }
        if (updates.getDayOfWeek() != null) {
            existing.setDayOfWeek(updates.getDayOfWeek());
        }
        if (updates.getLeaseRate() != null) {
            existing.setLeaseRate(updates.getLeaseRate());
        }
        if (updates.getStartDate() != null) {
            existing.setStartDate(updates.getStartDate());
        }
        if (updates.getEndDate() != null) {
            existing.setEndDate(updates.getEndDate());
        }
        if (updates.getIsActive() != null) {
            existing.setIsActive(updates.getIsActive());
        }
        if (updates.getNotes() != null) {
            existing.setNotes(updates.getNotes());
        }
        if (updates.getUpdatedBy() != null) {
            existing.setUpdatedBy(updates.getUpdatedBy());
        }
        
        // Recalculate priority
        existing.calculatePriority();
        
        return leaseRateOverrideRepository.save(existing);
    }

    /**
     * Delete an override
     */
    @Transactional
    public void deleteOverride(Long id) {
        log.info("Deleting lease rate override: id={}", id);
        leaseRateOverrideRepository.deleteById(id);
    }

    /**
     * Deactivate an override (soft delete)
     */
    @Transactional
    public LeaseRateOverride deactivateOverride(Long id) {
        LeaseRateOverride override = leaseRateOverrideRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Override not found: " + id));
        
        log.info("Deactivating lease rate override: id={}", id);
        override.setIsActive(false);
        return leaseRateOverrideRepository.save(override);
    }

    /**
     * Activate an override
     */
    @Transactional
    public LeaseRateOverride activateOverride(Long id) {
        LeaseRateOverride override = leaseRateOverrideRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Override not found: " + id));
        
        log.info("Activating lease rate override: id={}", id);
        override.setIsActive(true);
        return leaseRateOverrideRepository.save(override);
    }

    /**
     * Get all overrides for an owner
     */
    @Transactional(readOnly = true)
    public List<LeaseRateOverride> getOwnerOverrides(String ownerDriverNumber) {
        return leaseRateOverrideRepository
            .findByOwnerDriverNumberOrderByPriorityDescCreatedAtDesc(ownerDriverNumber);
    }

    /**
     * Get all overrides (all owners)
     */
    @Transactional(readOnly = true)
    public List<LeaseRateOverride> getAllOverrides() {
        return leaseRateOverrideRepository.findAllByOrderByOwnerDriverNumberAscPriorityDescCreatedAtDesc();
    }

    /**
     * Get all active overrides for an owner
     */
    @Transactional(readOnly = true)
    public List<LeaseRateOverride> getActiveOwnerOverrides(String ownerDriverNumber) {
        return leaseRateOverrideRepository
            .findByOwnerDriverNumberAndIsActiveTrue(ownerDriverNumber);
    }

    /**
     * Get overrides expiring in the next N days
     */
    @Transactional(readOnly = true)
    public List<LeaseRateOverride> getExpiringSoon(int days) {
        LocalDate now = LocalDate.now();
        LocalDate futureDate = now.plusDays(days);
        return leaseRateOverrideRepository.findExpiringSoon(now, futureDate);
    }

    /**
     * Get all currently active overrides
     */
    @Transactional(readOnly = true)
    public List<LeaseRateOverride> getAllActiveOverrides() {
        return leaseRateOverrideRepository.findAllActiveOverrides(LocalDate.now());
    }

    /**
     * Validate that the driver is actually an owner
     */
    private void validateOwner(String driverNumber) {
        driverRepository.findByDriverNumber(driverNumber)
            .filter(driver -> driver.getIsOwner() != null && driver.getIsOwner())
            .orElseThrow(() -> new RuntimeException(
                "Driver " + driverNumber + " is not found or not an owner"));
    }

    /**
     * Bulk create overrides for a specific pattern
     * Example: Create overrides for all weekdays with one rate, weekends with another
     */
    @Transactional
    public List<LeaseRateOverride> createBulkOverrides(
            String ownerDriverNumber,
            String cabNumber,
            String shiftType,
            List<String> daysOfWeek,
            BigDecimal leaseRate,
            LocalDate startDate,
            LocalDate endDate,
            String notes) {
        
        validateOwner(ownerDriverNumber);
        
        List<LeaseRateOverride> overrides = daysOfWeek.stream()
            .map(day -> {
                LeaseRateOverride override = LeaseRateOverride.builder()
                    .ownerDriverNumber(ownerDriverNumber)
                    .cabNumber(cabNumber)
                    .shiftType(shiftType)
                    .dayOfWeek(day)
                    .leaseRate(leaseRate)
                    .startDate(startDate != null ? startDate : LocalDate.now())
                    .endDate(endDate)
                    .isActive(true)
                    .notes(notes)
                    .build();
                
                override.calculatePriority();
                return override;
            })
            .toList();
        
        log.info("Creating {} bulk overrides for owner {}", overrides.size(), ownerDriverNumber);
        return leaseRateOverrideRepository.saveAll(overrides);
    }
}