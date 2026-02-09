package com.taxi.domain.cab.service;

import com.taxi.domain.cab.model.Cab;
import com.taxi.domain.cab.model.CabAttributeType;
import com.taxi.domain.cab.model.CabAttributeValue;
import com.taxi.domain.cab.model.AttributeCost;
import com.taxi.domain.cab.repository.CabAttributeValueRepository;
import com.taxi.domain.cab.repository.CabRepository;
import com.taxi.domain.cab.repository.CabAttributeTypeRepository;
import com.taxi.domain.expense.model.RecurringExpense;
import com.taxi.domain.expense.service.RecurringExpenseService;
import com.taxi.domain.shift.model.CabShift;
import com.taxi.domain.shift.repository.CabShiftRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing cab attribute values (temporal assignments)
 * Following CabService pattern with CabOwnerHistory logic
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CabAttributeValueService {

    private final CabAttributeValueRepository attributeValueRepository;
    private final CabRepository cabRepository;
    private final CabAttributeTypeRepository attributeTypeRepository;
    private final CabShiftRepository cabShiftRepository;
    private final AttributeCostService attributeCostService;
    private final RecurringExpenseService recurringExpenseService;

    /**
     * Assign an attribute to a cab with date range
     * Following createOwnerHistoryRecord pattern from CabService
     */
    @Transactional
    public CabAttributeValue assignAttribute(
            Long cabId,
            Long attributeTypeId,
            String attributeValue,
            LocalDate startDate,
            LocalDate endDate,
            String notes) {

        log.info("Assigning attribute {} to cab {}", attributeTypeId, cabId);

        Cab cab = cabRepository.findById(cabId)
                .orElseThrow(() -> new RuntimeException("Cab not found: " + cabId));

        CabAttributeType attributeType = attributeTypeRepository.findById(attributeTypeId)
                .orElseThrow(() -> new RuntimeException("Attribute type not found: " + attributeTypeId));

        // Validate dates
        if (endDate != null && startDate.isAfter(endDate)) {
            throw new RuntimeException("Start date must be before or equal to end date");
        }

        // Check for overlapping assignments
        List<CabAttributeValue> overlapping = attributeValueRepository.findOverlappingAttributes(
            cab, attributeType, startDate, endDate != null ? endDate : LocalDate.of(9999, 12, 31), -1L);

        if (!overlapping.isEmpty()) {
            throw new RuntimeException(
                "Attribute assignment overlaps with existing assignment(s). " +
                "Please end the current assignment first or choose a different date range.");
        }

        // Validate attribute value if required
        if (attributeType.isRequiresValue() && (attributeValue == null || attributeValue.trim().isEmpty())) {
            throw new RuntimeException("Attribute value is required for: " + attributeType.getAttributeName());
        }

        CabAttributeValue value = CabAttributeValue.builder()
                .cab(cab)
                .attributeType(attributeType)
                .attributeValue(attributeValue)
                .startDate(startDate)
                .endDate(endDate)
                .notes(notes)
                .build();

        return attributeValueRepository.save(value);
    }

    /**
     * Update an existing attribute assignment
     */
    @Transactional
    public CabAttributeValue updateAttributeValue(Long id, String attributeValue,
            LocalDate startDate, LocalDate endDate, String notes) {

        log.info("Updating attribute value ID: {}", id);

        CabAttributeValue value = attributeValueRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Attribute value not found: " + id));

        // Validate dates if provided
        LocalDate newStartDate = startDate != null ? startDate : value.getStartDate();
        LocalDate newEndDate = endDate != null ? endDate : value.getEndDate();

        if (newEndDate != null && newStartDate.isAfter(newEndDate)) {
            throw new RuntimeException("Start date must be before or equal to end date");
        }

        // Check for overlapping assignments (excluding this one)
        List<CabAttributeValue> overlapping = attributeValueRepository.findOverlappingAttributes(
            value.getCab(), value.getAttributeType(), newStartDate,
            newEndDate != null ? newEndDate : LocalDate.of(9999, 12, 31), id);

        if (!overlapping.isEmpty()) {
            throw new RuntimeException("Updated dates overlap with existing assignment(s)");
        }

        if (attributeValue != null) {
            value.setAttributeValue(attributeValue);
        }
        if (startDate != null) {
            value.setStartDate(startDate);
        }
        value.setEndDate(endDate);
        if (notes != null) {
            value.setNotes(notes);
        }

        return attributeValueRepository.save(value);
    }

    /**
     * End an attribute assignment (set end date)
     * Following closeOwnerHistory pattern
     */
    @Transactional
    public void endAttributeAssignment(Long id, LocalDate endDate) {
        log.info("Ending attribute assignment ID: {} on {}", id, endDate);

        CabAttributeValue value = attributeValueRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Attribute value not found: " + id));

        if (endDate.isBefore(value.getStartDate())) {
            throw new RuntimeException("End date cannot be before start date");
        }

        value.setEndDate(endDate);
        attributeValueRepository.save(value);
    }

    /**
     * Get current attributes for a cab
     */
    @Transactional(readOnly = true)
    public List<CabAttributeValue> getCurrentAttributes(Long cabId) {
        log.info("Getting current attributes for cab ID: {}", cabId);
        return attributeValueRepository.findCurrentAttributesByCabId(cabId);
    }

    /**
     * Get attributes active on a specific date
     */
    @Transactional(readOnly = true)
    public List<CabAttributeValue> getAttributesOnDate(Long cabId, LocalDate date) {
        log.info("Getting attributes for cab {} on {}", cabId, date);

        Cab cab = cabRepository.findById(cabId)
                .orElseThrow(() -> new RuntimeException("Cab not found: " + cabId));

        return attributeValueRepository.findAttributesActiveOnDate(cab, date);
    }

    /**
     * Get full attribute history for a cab
     */
    @Transactional(readOnly = true)
    public List<CabAttributeValue> getAttributeHistory(Long cabId) {
        log.info("Getting attribute history for cab ID: {}", cabId);

        Cab cab = cabRepository.findById(cabId)
                .orElseThrow(() -> new RuntimeException("Cab not found: " + cabId));

        return attributeValueRepository.findByCabOrderByStartDateDesc(cab);
    }

    /**
     * Get history for a specific attribute type
     */
    @Transactional(readOnly = true)
    public List<CabAttributeValue> getAttributeHistoryByType(Long cabId, Long attributeTypeId) {
        log.info("Getting attribute history for cab {} and type {}", cabId, attributeTypeId);

        Cab cab = cabRepository.findById(cabId)
                .orElseThrow(() -> new RuntimeException("Cab not found: " + cabId));

        CabAttributeType attributeType = attributeTypeRepository.findById(attributeTypeId)
                .orElseThrow(() -> new RuntimeException("Attribute type not found: " + attributeTypeId));

        return attributeValueRepository.findAttributeHistoryByCabAndType(cab, attributeType);
    }

    /**
     * Check if cab has a specific attribute currently
     */
    @Transactional(readOnly = true)
    public boolean hasAttributeNow(Long cabId, String attributeCode) {
        Cab cab = cabRepository.findById(cabId)
                .orElseThrow(() -> new RuntimeException("Cab not found: " + cabId));

        CabAttributeType attributeType = attributeTypeRepository.findByAttributeCode(attributeCode)
                .orElseThrow(() -> new RuntimeException("Attribute type not found: " + attributeCode));

        return attributeValueRepository.findCurrentAttributeByCabAndType(cab, attributeType).isPresent();
    }

    /**
     * Get all cabs with a specific attribute currently
     */
    @Transactional(readOnly = true)
    public List<CabAttributeValue> getCabsWithAttribute(Long attributeTypeId) {
        CabAttributeType attributeType = attributeTypeRepository.findById(attributeTypeId)
                .orElseThrow(() -> new RuntimeException("Attribute type not found: " + attributeTypeId));

        return attributeValueRepository.findCabsWithCurrentAttribute(attributeType);
    }

    /**
     * Delete an attribute assignment
     */
    @Transactional
    public void deleteAttributeValue(Long id) {
        log.info("Deleting attribute value ID: {}", id);
        attributeValueRepository.deleteById(id);
    }

    // ============================================================================
    // SHIFT-LEVEL ATTRIBUTE OPERATIONS
    // ============================================================================
    // New methods for managing custom attributes at shift level

    /**
     * Assign an attribute to a shift with date range
     */
    @Transactional
    public CabAttributeValue assignAttributeToShift(
            Long shiftId,
            Long attributeTypeId,
            String attributeValue,
            LocalDate startDate,
            LocalDate endDate,
            String notes) {

        log.info("Assigning attribute {} to shift {}", attributeTypeId, shiftId);

        CabShift shift = cabShiftRepository.findByIdWithCab(shiftId)
                .orElseThrow(() -> new RuntimeException("Shift not found: " + shiftId));

        CabAttributeType attributeType = attributeTypeRepository.findById(attributeTypeId)
                .orElseThrow(() -> new RuntimeException("Attribute type not found: " + attributeTypeId));

        // Validate dates
        if (endDate != null && startDate.isAfter(endDate)) {
            throw new RuntimeException("Start date must be before or equal to end date");
        }

        // Check for overlapping assignments
        List<CabAttributeValue> overlapping = attributeValueRepository.findOverlappingAttributesForShift(
            shift, attributeType, startDate, endDate != null ? endDate : LocalDate.of(9999, 12, 31), -1L);

        if (!overlapping.isEmpty()) {
            throw new RuntimeException(
                "Attribute assignment overlaps with existing assignment(s). " +
                "Please end the current assignment first or choose a different date range.");
        }

        // Validate attribute value if required
        if (attributeType.isRequiresValue() && (attributeValue == null || attributeValue.trim().isEmpty())) {
            throw new RuntimeException("Attribute value is required for: " + attributeType.getAttributeName());
        }

        CabAttributeValue value = CabAttributeValue.builder()
                .cab(shift.getCab())  // Set cab from shift
                .shift(shift)
                .attributeType(attributeType)
                .attributeValue(attributeValue)
                .startDate(startDate)
                .endDate(endDate)
                .notes(notes)
                .build();

        CabAttributeValue savedValue = attributeValueRepository.save(value);

        // Auto-create recurring expense if cost exists for this attribute
        autoCreateRecurringExpense(shift, attributeType, startDate, endDate);

        return savedValue;
    }

    /**
     * Auto-create recurring expense when attribute is assigned if active cost exists
     */
    private void autoCreateRecurringExpense(CabShift shift, CabAttributeType attributeType,
                                           LocalDate startDate, LocalDate endDate) {
        try {
            // Check if there's an active cost for this attribute on the start date
            Optional<AttributeCost> costOpt = attributeCostService.getActiveOn(attributeType.getId(), startDate);

            if (costOpt.isEmpty()) {
                log.debug("No active cost found for attribute {} on date {}, skipping recurring expense creation",
                    attributeType.getId(), startDate);
                return;
            }

            AttributeCost cost = costOpt.get();
            log.info("Auto-creating recurring expense for shift {} with attribute {} cost {}",
                shift.getId(), attributeType.getId(), cost.getPrice());

            // Create recurring expense with application type
            RecurringExpense recurringExpense = RecurringExpense.builder()
                    .cab(shift.getCab())
                    .shift(shift)
                    .amount(cost.getPrice())
                    .billingMethod(cost.getBillingUnit().name())
                    .effectiveFrom(startDate)
                    .effectiveTo(endDate)
                    .notes("Auto-created from attribute cost: " + attributeType.getAttributeName())
                    .isActive(true)
                    // Application type fields - charge to this specific shift
                    .applicationTypeEnum(com.taxi.domain.expense.model.ApplicationType.SPECIFIC_SHIFT)
                    .specificShiftId(shift.getId())
                    .createdBy("system")
                    .build();

            recurringExpenseService.create(recurringExpense);
            log.info("Successfully auto-created recurring expense for shift {} attribute {}",
                shift.getId(), attributeType.getId());

        } catch (Exception e) {
            log.warn("Failed to auto-create recurring expense for attribute assignment. " +
                "Will need to create manually. Error: {}", e.getMessage());
            // Don't throw - attribute assignment should succeed even if recurring expense creation fails
        }
    }

    /**
     * Update an existing shift attribute assignment
     */
    @Transactional
    public CabAttributeValue updateShiftAttributeValue(Long id, String attributeValue,
            LocalDate startDate, LocalDate endDate, String notes) {

        log.info("Updating shift attribute value ID: {}", id);

        CabAttributeValue value = attributeValueRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Attribute value not found: " + id));

        if (value.getShift() == null) {
            throw new RuntimeException("This attribute is not a shift-level attribute");
        }

        // Validate dates if provided
        LocalDate newStartDate = startDate != null ? startDate : value.getStartDate();
        LocalDate newEndDate = endDate != null ? endDate : value.getEndDate();

        if (newEndDate != null && newStartDate.isAfter(newEndDate)) {
            throw new RuntimeException("Start date must be before or equal to end date");
        }

        // Check for overlapping assignments (excluding this one)
        List<CabAttributeValue> overlapping = attributeValueRepository.findOverlappingAttributesForShift(
            value.getShift(), value.getAttributeType(), newStartDate,
            newEndDate != null ? newEndDate : LocalDate.of(9999, 12, 31), id);

        if (!overlapping.isEmpty()) {
            throw new RuntimeException("Updated dates overlap with existing assignment(s)");
        }

        if (attributeValue != null) {
            value.setAttributeValue(attributeValue);
        }
        if (startDate != null) {
            value.setStartDate(startDate);
        }
        value.setEndDate(endDate);
        if (notes != null) {
            value.setNotes(notes);
        }

        return attributeValueRepository.save(value);
    }

    /**
     * End a shift attribute assignment (set end date)
     */
    @Transactional
    public void endShiftAttributeAssignment(Long id, LocalDate endDate) {
        log.info("Ending shift attribute assignment ID: {} on {}", id, endDate);

        CabAttributeValue value = attributeValueRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Attribute value not found: " + id));

        if (value.getShift() == null) {
            throw new RuntimeException("This attribute is not a shift-level attribute");
        }

        if (endDate.isBefore(value.getStartDate())) {
            throw new RuntimeException("End date cannot be before start date");
        }

        value.setEndDate(endDate);
        attributeValueRepository.save(value);
    }

    /**
     * Get current attributes for a shift
     */
    @Transactional(readOnly = true)
    public List<CabAttributeValue> getCurrentAttributesByShift(Long shiftId) {
        log.info("Getting current attributes for shift ID: {}", shiftId);
        return attributeValueRepository.findCurrentAttributesByShiftId(shiftId);
    }

    /**
     * Get attributes active on a specific date for a shift
     */
    @Transactional(readOnly = true)
    public List<CabAttributeValue> getAttributesOnDateByShift(Long shiftId, LocalDate date) {
        log.info("Getting attributes for shift {} on {}", shiftId, date);

        CabShift shift = cabShiftRepository.findById(shiftId)
                .orElseThrow(() -> new RuntimeException("Shift not found: " + shiftId));

        return attributeValueRepository.findAttributesActiveOnDateByShift(shift, date);
    }

    /**
     * Get full attribute history for a shift
     */
    @Transactional(readOnly = true)
    public List<CabAttributeValue> getAttributeHistoryByShift(Long shiftId) {
        log.info("Getting attribute history for shift ID: {}", shiftId);

        CabShift shift = cabShiftRepository.findById(shiftId)
                .orElseThrow(() -> new RuntimeException("Shift not found: " + shiftId));

        return attributeValueRepository.findByShiftOrderByStartDateDesc(shift);
    }

    /**
     * Get history for a specific attribute type on a shift
     */
    @Transactional(readOnly = true)
    public List<CabAttributeValue> getAttributeHistoryByShiftAndType(Long shiftId, Long attributeTypeId) {
        log.info("Getting attribute history for shift {} and type {}", shiftId, attributeTypeId);

        CabShift shift = cabShiftRepository.findById(shiftId)
                .orElseThrow(() -> new RuntimeException("Shift not found: " + shiftId));

        CabAttributeType attributeType = attributeTypeRepository.findById(attributeTypeId)
                .orElseThrow(() -> new RuntimeException("Attribute type not found: " + attributeTypeId));

        return attributeValueRepository.findAttributeHistoryByShiftAndType(shift, attributeType);
    }

    /**
     * Check if shift has a specific attribute currently
     */
    @Transactional(readOnly = true)
    public boolean hasShiftAttributeNow(Long shiftId, String attributeCode) {
        CabShift shift = cabShiftRepository.findById(shiftId)
                .orElseThrow(() -> new RuntimeException("Shift not found: " + shiftId));

        CabAttributeType attributeType = attributeTypeRepository.findByAttributeCode(attributeCode)
                .orElseThrow(() -> new RuntimeException("Attribute type not found: " + attributeCode));

        return attributeValueRepository.findCurrentAttributeByShiftAndType(shift, attributeType).isPresent();
    }

    /**
     * Get all shifts with a specific attribute currently
     */
    @Transactional(readOnly = true)
    public List<CabAttributeValue> getShiftsWithAttribute(Long attributeTypeId) {
        CabAttributeType attributeType = attributeTypeRepository.findById(attributeTypeId)
                .orElseThrow(() -> new RuntimeException("Attribute type not found: " + attributeTypeId));

        return attributeValueRepository.findShiftsWithCurrentAttribute(attributeType);
    }

    /**
     * Delete a shift attribute assignment
     */
    @Transactional
    public void deleteShiftAttributeValue(Long id) {
        log.info("Deleting shift attribute value ID: {}", id);

        CabAttributeValue value = attributeValueRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Attribute value not found: " + id));

        if (value.getShift() == null) {
            throw new RuntimeException("This attribute is not a shift-level attribute");
        }

        attributeValueRepository.deleteById(id);
    }
}
