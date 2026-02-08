package com.taxi.domain.shift.service;

import com.taxi.domain.shift.model.CabShift;
import com.taxi.domain.shift.model.ShiftStatusHistory;
import com.taxi.domain.shift.repository.CabShiftRepository;
import com.taxi.domain.shift.repository.ShiftStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Domain service for managing shift active/inactive status with historical tracking
 *
 * Purpose: Manage when shifts become active or inactive, with full audit trail
 * for historical reporting and accurate expense calculations.
 *
 * Key Responsibilities:
 * - Activate/deactivate shifts with effective dates
 * - Maintain historical status change records
 * - Query shift status at any point in time (for reports)
 * - Ensure only one current status (effective_to = NULL) per shift
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShiftStatusService {

    private final ShiftStatusHistoryRepository statusHistoryRepository;
    private final CabShiftRepository cabShiftRepository;

    /**
     * Activate a shift with effective date and audit trail
     *
     * Creates a new status history record marking the shift as active.
     * Closes the previous status record (if any) by setting effective_to.
     *
     * @param shiftId The shift ID to activate
     * @param effectiveFrom The date when this activation becomes effective
     * @param changedBy The user making the change (for audit trail)
     * @param reason Optional reason for the status change
     * @throws IllegalArgumentException if shift not found
     */
    @Transactional
    public void activateShift(Long shiftId, LocalDate effectiveFrom, String changedBy, String reason) {
        log.info("Activating shift {} effective from {} by {}", shiftId, effectiveFrom, changedBy);

        CabShift shift = cabShiftRepository.findById(shiftId)
            .orElseThrow(() -> {
                log.error("Shift not found: {}", shiftId);
                return new IllegalArgumentException("Shift not found: " + shiftId);
            });

        // Check if already active on the effective date
        Optional<ShiftStatusHistory> existingActive = statusHistoryRepository.findActiveStatusOn(shiftId, effectiveFrom);
        if (existingActive.isPresent()) {
            log.warn("Shift {} is already active on {}", shiftId, effectiveFrom);
            return;
        }

        // Close the current status record if it exists
        Optional<ShiftStatusHistory> currentStatus = statusHistoryRepository.findCurrentStatus(shiftId);
        if (currentStatus.isPresent()) {
            ShiftStatusHistory previous = currentStatus.get();
            previous.setEffectiveTo(effectiveFrom.minusDays(1));
            statusHistoryRepository.save(previous);
            log.debug("Closed previous status for shift {} - was: {} until {}",
                shiftId, previous.getIsActive(), previous.getEffectiveTo());
        }

        // Create new active status record
        ShiftStatusHistory newStatus = ShiftStatusHistory.builder()
            .shift(shift)
            .isActive(true)
            .effectiveFrom(effectiveFrom)
            .effectiveTo(null)  // NULL means current/ongoing
            .reason(reason)
            .changedBy(changedBy)
            .build();

        ShiftStatusHistory saved = statusHistoryRepository.save(newStatus);
        log.info("Successfully activated shift {} - record ID: {}, effective from: {}",
            shiftId, saved.getId(), effectiveFrom);
    }

    /**
     * Deactivate a shift with effective date and audit trail
     *
     * Creates a new status history record marking the shift as inactive.
     * Closes the previous status record (if any) by setting effective_to.
     *
     * @param shiftId The shift ID to deactivate
     * @param effectiveFrom The date when this deactivation becomes effective
     * @param changedBy The user making the change (for audit trail)
     * @param reason Optional reason for the status change
     * @throws IllegalArgumentException if shift not found
     */
    @Transactional
    public void deactivateShift(Long shiftId, LocalDate effectiveFrom, String changedBy, String reason) {
        log.info("Deactivating shift {} effective from {} by {}", shiftId, effectiveFrom, changedBy);

        CabShift shift = cabShiftRepository.findById(shiftId)
            .orElseThrow(() -> {
                log.error("Shift not found: {}", shiftId);
                return new IllegalArgumentException("Shift not found: " + shiftId);
            });

        // Check if already inactive on the effective date
        Optional<ShiftStatusHistory> inactiveStatus = statusHistoryRepository.findCurrentStatus(shiftId);
        if (inactiveStatus.isPresent() && !inactiveStatus.get().getIsActive()) {
            log.warn("Shift {} is already inactive on {}", shiftId, effectiveFrom);
            return;
        }

        // Close the current status record if it exists
        Optional<ShiftStatusHistory> currentStatus = statusHistoryRepository.findCurrentStatus(shiftId);
        if (currentStatus.isPresent()) {
            ShiftStatusHistory previous = currentStatus.get();
            previous.setEffectiveTo(effectiveFrom.minusDays(1));
            statusHistoryRepository.save(previous);
            log.debug("Closed previous status for shift {} - was: {} until {}",
                shiftId, previous.getIsActive(), previous.getEffectiveTo());
        }

        // Create new inactive status record
        ShiftStatusHistory newStatus = ShiftStatusHistory.builder()
            .shift(shift)
            .isActive(false)
            .effectiveFrom(effectiveFrom)
            .effectiveTo(null)  // NULL means current/ongoing
            .reason(reason)
            .changedBy(changedBy)
            .build();

        ShiftStatusHistory saved = statusHistoryRepository.save(newStatus);
        log.info("Successfully deactivated shift {} - record ID: {}, effective from: {}",
            shiftId, saved.getId(), effectiveFrom);
    }

    /**
     * Check if a shift was active on a specific date
     *
     * Queries the status history to determine if the shift was active at any point
     * on the given date. This is used for historical reporting.
     *
     * @param shiftId The shift ID
     * @param date The date to check
     * @return true if shift was active on that date, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean wasShiftActiveOn(Long shiftId, LocalDate date) {
        Optional<ShiftStatusHistory> activeStatus = statusHistoryRepository.findActiveStatusOn(shiftId, date);
        boolean result = activeStatus.isPresent();
        log.debug("Shift {} active on {}: {}", shiftId, date, result);
        return result;
    }

    /**
     * Get the complete status history for a shift
     *
     * Returns all status records for the shift, ordered by most recent first.
     * Includes both historical and current status records.
     *
     * @param shiftId The shift ID
     * @return List of status history records
     */
    @Transactional(readOnly = true)
    public List<ShiftStatusHistory> getStatusHistory(Long shiftId) {
        List<ShiftStatusHistory> history = statusHistoryRepository.findByShiftIdOrderByEffectiveFromDesc(shiftId);
        log.debug("Retrieved {} status history records for shift {}", history.size(), shiftId);
        return history;
    }

    /**
     * Get the current status of a shift
     *
     * Returns the status record where effective_to is NULL, which represents
     * the current active/inactive status.
     *
     * @param shiftId The shift ID
     * @return The current status record, or empty if not found
     */
    @Transactional(readOnly = true)
    public Optional<ShiftStatusHistory> getCurrentStatus(Long shiftId) {
        return statusHistoryRepository.findCurrentStatus(shiftId);
    }

    /**
     * Get status history for a shift within a date range
     *
     * Useful for reports that need to understand shift status changes
     * during a specific period.
     *
     * @param shiftId The shift ID
     * @param fromDate Start date (inclusive)
     * @param toDate End date (inclusive)
     * @return List of status records that overlap with the date range
     */
    @Transactional(readOnly = true)
    public List<ShiftStatusHistory> getStatusHistoryInRange(Long shiftId, LocalDate fromDate, LocalDate toDate) {
        List<ShiftStatusHistory> history = statusHistoryRepository.findStatusHistoryInDateRange(shiftId, fromDate, toDate);
        log.debug("Retrieved {} status history records for shift {} between {} and {}",
            history.size(), shiftId, fromDate, toDate);
        return history;
    }

    /**
     * Check if shift is currently active
     *
     * Convenience method that checks the current status without needing
     * to look at the shift entity itself.
     *
     * @param shiftId The shift ID
     * @return true if currently active, false if inactive or no status found
     */
    @Transactional(readOnly = true)
    public boolean isCurrentlyActive(Long shiftId) {
        Optional<ShiftStatusHistory> currentStatus = statusHistoryRepository.findCurrentStatus(shiftId);
        return currentStatus.map(ShiftStatusHistory::getIsActive).orElse(false);
    }

    /**
     * Get all shifts with a specific active status
     *
     * @param shiftId The shift ID
     * @return true if at least one status history record exists
     */
    @Transactional(readOnly = true)
    public boolean hasStatusHistory(Long shiftId) {
        return !statusHistoryRepository.findByShiftIdOrderByEffectiveFromDesc(shiftId).isEmpty();
    }
}
