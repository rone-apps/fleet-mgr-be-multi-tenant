package com.taxi.domain.shift.service;

import com.taxi.domain.shift.model.CabShift;
import com.taxi.domain.shift.model.ShiftOwnership;
import com.taxi.domain.shift.model.ShiftType;
import com.taxi.domain.shift.repository.CabShiftRepository;
import com.taxi.domain.shift.repository.ShiftOwnershipRepository;
import com.taxi.domain.cab.model.Cab;
import com.taxi.domain.cab.repository.CabRepository;
import com.taxi.domain.driver.model.Driver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Domain service for managing shift ownership
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShiftOwnershipService {

    private final ShiftOwnershipRepository ownershipRepository;
    private final CabShiftRepository cabShiftRepository;
    private final CabRepository cabRepository;

    /**
     * Transfer ownership of a shift to a new owner
     * 
     * @param shift The shift being transferred
     * @param newOwner The new owner
     * @param transferDate The date of transfer
     * @param salePrice The sale price (optional)
     */
    @Transactional
    public void transferOwnership(CabShift shift, Driver newOwner, LocalDate transferDate, BigDecimal salePrice) {
        log.info("Transferring ownership of shift {} to driver {} on {}", 
            shift.getId(), newOwner.getDriverNumber(), transferDate);

        // Validate
        if (shift.getCurrentOwner().getId().equals(newOwner.getId())) {
            throw new OwnershipException("Cannot transfer ownership to the same owner");
        }

        // Close current ownership
        ShiftOwnership currentOwnership = ownershipRepository.findCurrentOwnership(shift.getId())
            .orElseThrow(() -> new OwnershipException("No current ownership found for shift"));

        currentOwnership.close(transferDate.minusDays(1), newOwner, salePrice);
        ownershipRepository.save(currentOwnership);

        log.debug("Closed previous ownership - Owner: {}, End Date: {}, Sale Price: {}", 
            currentOwnership.getOwner().getDriverNumber(), currentOwnership.getEndDate(), salePrice);

        // Create new ownership
        ShiftOwnership newOwnership = ShiftOwnership.builder()
            .shift(shift)
            .owner(newOwner)
            .startDate(transferDate)
            .endDate(null)  // Current ownership
            .acquisitionType(ShiftOwnership.AcquisitionType.TRANSFER)
            .acquisitionPrice(salePrice)
            .build();

        ownershipRepository.save(newOwnership);

        // Update shift's current owner
        shift.transferOwnership(newOwner);
        cabShiftRepository.save(shift);

        log.info("Ownership transfer completed - New owner: {}, Start Date: {}", 
            newOwner.getDriverNumber(), transferDate);
    }

    /**
     * Get the owner of a shift on a specific date (for historical reports)
     */
    @Transactional(readOnly = true)
    public Driver getOwnerOnDate(CabShift shift, LocalDate date) {
        return ownershipRepository.findOwnershipOnDate(shift.getId(), date)
            .map(ShiftOwnership::getOwner)
            .orElseThrow(() -> new OwnershipException(
                "No ownership found for shift " + shift.getId() + " on date " + date));
    }

    /**
     * Get complete ownership history for a shift
     */
    @Transactional(readOnly = true)
    public List<ShiftOwnership> getOwnershipHistory(CabShift shift) {
        return ownershipRepository.findByShiftOrderByStartDateDesc(shift);
    }

    /**
     * Get all current ownerships for a driver
     */
    @Transactional(readOnly = true)
    public List<ShiftOwnership> getCurrentOwnerships(Driver driver) {
        return ownershipRepository.findCurrentOwnershipsByDriver(driver.getId());
    }

    /**
     * Get all historical ownerships for a driver
     */
    @Transactional(readOnly = true)
    public List<ShiftOwnership> getOwnershipHistory(Driver driver) {
        return ownershipRepository.findByOwnerOrderByStartDateDesc(driver);
    }

    /**
     * Get ownership records within a date range
     */
    @Transactional(readOnly = true)
    public List<ShiftOwnership> getOwnershipInRange(CabShift shift, LocalDate startDate, LocalDate endDate) {
        return ownershipRepository.findOwnershipInRange(shift.getId(), startDate, endDate);
    }

    /**
     * Check if a driver owns a shift on a specific date
     */
    @Transactional(readOnly = true)
    public boolean isOwnerOnDate(Driver driver, CabShift shift, LocalDate date) {
        return ownershipRepository.findOwnershipOnDate(shift.getId(), date)
            .map(ownership -> ownership.getOwner().getId().equals(driver.getId()))
            .orElse(false);
    }

    /**
     * Create initial ownership for a new shift
     */
    @Transactional
    public ShiftOwnership createInitialOwnership(CabShift shift, Driver owner, LocalDate startDate) {
        log.info("Creating initial ownership for shift {} with owner {}", 
            shift.getId(), owner.getDriverNumber());

        ShiftOwnership ownership = ShiftOwnership.builder()
            .shift(shift)
            .owner(owner)
            .startDate(startDate)
            .endDate(null)  // Current ownership
            .acquisitionType(ShiftOwnership.AcquisitionType.INITIAL_ASSIGNMENT)
            .build();

        return ownershipRepository.save(ownership);
    }

    /**
     * Exception for ownership-related errors
     */
    public static class OwnershipException extends RuntimeException {
        public OwnershipException(String message) {
            super(message);
        }

        public OwnershipException(String message, Throwable cause) {
            super(message, cause);
        }
    }

/**
 * Add this method to ShiftOwnershipService.java
 */

/**
 * Find the CabShift by cab number and shift type
 * 
 * @param cabNumber The cab number (e.g., "CAB-001")
 * @param shiftType The shift type ("DAY" or "NIGHT")
 * @return The matching CabShift, or null if not found
 */
public CabShift findCabShiftByCabNumberAndShiftType(String cabNumber, String shiftType) {
    try {
        // First, find the cab
        Cab cab = cabRepository.findByCabNumber(cabNumber)
                .orElseThrow(() -> new IllegalArgumentException("Cab not found: " + cabNumber));
        
        // Convert string shift type to enum
        ShiftType shiftTypeEnum = ShiftType.valueOf(shiftType);
        
        // Find the cab shift matching this cab and shift type
        return cabShiftRepository.findByCabAndShiftType(cab, shiftTypeEnum)
                .orElse(null);
        
    } catch (Exception e) {
        log.error("Error finding cab shift for cab {} shift {}: {}", 
                cabNumber, shiftType, e.getMessage());
        return null;
    }
}

}
