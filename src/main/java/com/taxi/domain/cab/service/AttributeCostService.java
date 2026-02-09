package com.taxi.domain.cab.service;

import com.taxi.domain.cab.model.AttributeCost;
import com.taxi.domain.cab.model.CabAttributeType;
import com.taxi.domain.cab.repository.AttributeCostRepository;
import com.taxi.domain.cab.repository.CabAttributeTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing attribute costs
 * Handles pricing rules for custom attributes assigned to shifts
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AttributeCostService {

    private final AttributeCostRepository attributeCostRepository;
    private final CabAttributeTypeRepository attributeTypeRepository;

    /**
     * Create a new attribute cost
     */
    public AttributeCost create(Long attributeTypeId, BigDecimal price, AttributeCost.BillingUnit billingUnit,
                                LocalDate effectiveFrom, LocalDate effectiveTo, String createdBy) {
        log.info("Creating attribute cost: attributeTypeId={}, price={}, billingUnit={}, effectiveFrom={}",
                attributeTypeId, price, billingUnit, effectiveFrom);

        CabAttributeType attributeType = attributeTypeRepository.findById(attributeTypeId)
                .orElseThrow(() -> new RuntimeException("Attribute type not found: " + attributeTypeId));

        // Validate: effectiveTo must be after effectiveFrom
        if (effectiveTo != null && effectiveTo.isBefore(effectiveFrom)) {
            throw new RuntimeException("Effective to date must be after effective from date");
        }

        AttributeCost cost = AttributeCost.builder()
                .attributeType(attributeType)
                .price(price)
                .billingUnit(billingUnit)
                .effectiveFrom(effectiveFrom)
                .effectiveTo(effectiveTo)
                .createdBy(createdBy)
                .build();

        return attributeCostRepository.save(cost);
    }

    /**
     * Get attribute cost by ID
     */
    @Transactional(readOnly = true)
    public AttributeCost getById(Long id) {
        return attributeCostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Attribute cost not found: " + id));
    }

    /**
     * Get all costs for an attribute, ordered by date descending
     */
    @Transactional(readOnly = true)
    public List<AttributeCost> getCostsByAttributeType(Long attributeTypeId) {
        log.info("Getting costs for attribute type: {}", attributeTypeId);
        return attributeCostRepository.findByAttributeTypeIdOrderByEffectiveFromDesc(attributeTypeId);
    }

    /**
     * Get the active cost for an attribute on a specific date
     */
    @Transactional(readOnly = true)
    public Optional<AttributeCost> getActiveOn(Long attributeTypeId, LocalDate date) {
        log.info("Getting active cost for attribute type {} on date {}", attributeTypeId, date);
        return attributeCostRepository.findActiveOn(attributeTypeId, date);
    }

    /**
     * Get all currently active costs
     */
    @Transactional(readOnly = true)
    public List<AttributeCost> getAllCurrentlyActive() {
        log.info("Getting all currently active costs");
        return attributeCostRepository.findAllCurrentlActive();
    }

    /**
     * Get active costs for multiple attributes on a specific date
     */
    @Transactional(readOnly = true)
    public List<AttributeCost> getActiveCostsOnDate(List<Long> attributeTypeIds, LocalDate date) {
        log.info("Getting active costs for {} attributes on date {}", attributeTypeIds.size(), date);
        if (attributeTypeIds.isEmpty()) {
            return List.of();
        }
        return attributeCostRepository.findActiveCostsOnDate(attributeTypeIds, date);
    }

    /**
     * Update an attribute cost
     * Note: effectiveFrom cannot be changed (it's the primary key for temporal data)
     */
    public AttributeCost update(Long id, BigDecimal price, AttributeCost.BillingUnit billingUnit,
                               LocalDate effectiveTo, String updatedBy) {
        log.info("Updating attribute cost: id={}, price={}, billingUnit={}, effectiveTo={}",
                id, price, billingUnit, effectiveTo);

        AttributeCost cost = getById(id);

        // Validate: effectiveTo must be after effectiveFrom
        if (effectiveTo != null && effectiveTo.isBefore(cost.getEffectiveFrom())) {
            throw new RuntimeException("Effective to date must be after effective from date");
        }

        cost.setPrice(price);
        cost.setBillingUnit(billingUnit);
        cost.setEffectiveTo(effectiveTo);
        cost.setUpdatedBy(updatedBy);

        return attributeCostRepository.save(cost);
    }

    /**
     * Delete an attribute cost
     * Only allow deletion of future costs, not historical ones
     */
    public void delete(Long id) {
        log.info("Deleting attribute cost: {}", id);

        AttributeCost cost = getById(id);

        // Don't allow deleting past costs (data integrity)
        if (cost.getEffectiveFrom().isBefore(LocalDate.now())) {
            throw new RuntimeException("Cannot delete historical attribute costs. Set an effective_to date instead.");
        }

        attributeCostRepository.delete(cost);
    }

    /**
     * End an active cost by setting its effective_to date
     * Creates audit trail for historical tracking
     */
    public AttributeCost endCost(Long id, LocalDate endingDate, String updatedBy) {
        log.info("Ending attribute cost: id={}, endingDate={}", id, endingDate);

        AttributeCost cost = getById(id);

        if (endingDate.isBefore(cost.getEffectiveFrom())) {
            throw new RuntimeException("Ending date cannot be before effective from date");
        }

        cost.setEffectiveTo(endingDate);
        cost.setUpdatedBy(updatedBy);

        return attributeCostRepository.save(cost);
    }

    /**
     * Check if an attribute has a chargeable cost
     */
    @Transactional(readOnly = true)
    public boolean hasChargeablePrice(Long attributeTypeId, LocalDate date) {
        return attributeCostRepository.findActiveOn(attributeTypeId, date).isPresent();
    }

    /**
     * Get cost for an attribute on a date
     * Returns 0 if no active cost found
     */
    @Transactional(readOnly = true)
    public BigDecimal getCostAmount(Long attributeTypeId, LocalDate date) {
        return attributeCostRepository.findActiveOn(attributeTypeId, date)
                .map(AttributeCost::getPrice)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Get billing unit for an attribute on a date
     */
    @Transactional(readOnly = true)
    public AttributeCost.BillingUnit getBillingUnit(Long attributeTypeId, LocalDate date) {
        return attributeCostRepository.findActiveOn(attributeTypeId, date)
                .map(AttributeCost::getBillingUnit)
                .orElse(AttributeCost.BillingUnit.MONTHLY);
    }
}
