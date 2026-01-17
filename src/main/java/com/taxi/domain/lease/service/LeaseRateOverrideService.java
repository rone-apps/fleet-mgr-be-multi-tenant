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
        
        // Get day of week
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        String dayOfWeekStr = dayOfWeek.toString(); // "MONDAY", "TUESDAY", etc.
        
        log.debug("Looking for lease rate override: owner={}, cab={}, shift={}, day={}, date={}", 
            ownerDriverNumber, cabNumber, shiftType, dayOfWeekStr, date);
        
        // Find matching overrides (ordered by priority)
        List<LeaseRateOverride> overrides = leaseRateOverrideRepository.findMatchingOverrides(
            ownerDriverNumber, cabNumber, shiftType, dayOfWeekStr, date
        );
        
        if (overrides.isEmpty()) {
            log.debug("No override found, will use default rate");
            return null;
        }
        
        // Return the highest priority override (first in list)
        LeaseRateOverride bestMatch = overrides.get(0);
        log.info("Found lease rate override: id={}, rate={}, priority={}, cab={}, shift={}, day={}", 
            bestMatch.getId(), bestMatch.getLeaseRate(), bestMatch.getPriority(),
            bestMatch.getCabNumber() != null ? bestMatch.getCabNumber() : "ALL",
            bestMatch.getShiftType() != null ? bestMatch.getShiftType() : "ALL",
            bestMatch.getDayOfWeek() != null ? bestMatch.getDayOfWeek() : "ALL");
        
        return bestMatch.getLeaseRate();
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