package com.taxi.domain.cab.service;

import com.taxi.domain.cab.model.Cab;
import com.taxi.domain.cab.model.CabAttributeType;
import com.taxi.domain.cab.model.CabAttributeValue;
import com.taxi.domain.cab.repository.CabAttributeValueRepository;
import com.taxi.domain.cab.repository.CabRepository;
import com.taxi.domain.cab.repository.CabAttributeTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

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
}
