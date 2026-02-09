package com.taxi.domain.shift.service;

import com.taxi.domain.cab.model.AttributeCost;
import com.taxi.domain.cab.model.CabAttributeValue;
import com.taxi.domain.cab.service.AttributeCostService;
import com.taxi.domain.cab.service.CabAttributeValueService;
import com.taxi.domain.shift.model.CabShift;
import com.taxi.domain.shift.repository.CabShiftRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for calculating shift charges based on attributes
 * Determines what a shift should be charged for during a period
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ShiftChargeCalculationService {

    private final CabShiftRepository cabShiftRepository;
    private final CabAttributeValueService attributeValueService;
    private final AttributeCostService attributeCostService;

    /**
     * Calculate total charges for a shift over a date range
     * Returns breakdown of charges by attribute
     */
    public ShiftChargeResult calculateShiftCharges(Long shiftId, LocalDate startDate, LocalDate endDate) {
        log.info("Calculating charges for shift {} from {} to {}", shiftId, startDate, endDate);

        CabShift shift = cabShiftRepository.findById(shiftId)
                .orElseThrow(() -> new RuntimeException("Shift not found: " + shiftId));

        // Get attributes active during this period
        List<CabAttributeValue> attributes = attributeValueService.getAttributeHistoryByShift(shiftId);

        List<ChargeLineItem> lineItems = new ArrayList<>();
        BigDecimal totalCharges = BigDecimal.ZERO;

        // Calculate charge for each attribute active during period
        for (CabAttributeValue attr : attributes) {
            // Check if attribute overlaps with the period
            if (!overlaps(attr.getStartDate(), attr.getEndDate(), startDate, endDate)) {
                continue;
            }

            // Calculate effective date range for this attribute during the charge period
            LocalDate attrStart = maxDate(attr.getStartDate(), startDate);
            LocalDate attrEnd = attr.getEndDate() != null ? minDate(attr.getEndDate(), endDate) : endDate;

            // Get cost on the start date of the attribute during this period
            AttributeCost cost = attributeCostService.getActiveOn(
                    attr.getAttributeType().getId(),
                    attrStart
            ).orElse(null);

            if (cost != null) {
                // Calculate charge for this line item
                BigDecimal lineItemCharge = calculateAttributeCharge(
                        cost,
                        attrStart,
                        attrEnd
                );

                lineItems.add(ChargeLineItem.builder()
                        .attributeCode(attr.getAttributeType().getAttributeCode())
                        .attributeName(attr.getAttributeType().getAttributeName())
                        .attributeValue(attr.getAttributeValue())
                        .price(cost.getPrice())
                        .billingUnit(cost.getBillingUnit().name())
                        .chargeStartDate(attrStart)
                        .chargeEndDate(attrEnd)
                        .daysActive(ChronoUnit.DAYS.between(attrStart, attrEnd) + 1)
                        .charge(lineItemCharge)
                        .build());

                totalCharges = totalCharges.add(lineItemCharge);
            }
        }

        return ShiftChargeResult.builder()
                .shiftId(shiftId)
                .shiftNumber(shift.getCab().getCabNumber())
                .shiftType(shift.getShiftType().name())
                .periodStartDate(startDate)
                .periodEndDate(endDate)
                .lineItems(lineItems)
                .totalCharges(totalCharges)
                .build();
    }

    /**
     * Get list of shifts affected by an attribute cost
     * Shows which shifts would be charged for an attribute cost
     */
    public List<AffectedShiftInfo> getAffectedShifts(Long attributeTypeId, LocalDate asOfDate) {
        log.info("Getting shifts affected by attribute type {} as of {}", attributeTypeId, asOfDate);

        // Get cost active on this date
        AttributeCost cost = attributeCostService.getActiveOn(attributeTypeId, asOfDate)
                .orElseThrow(() -> new RuntimeException("No active cost found for attribute on date: " + asOfDate));

        // Get all shifts with this attribute
        List<CabAttributeValue> attributeAssignments = new ArrayList<>();
        // This would require a query to find all shifts with specific attribute
        // For now, we'll use a simpler approach - query from repository

        // Get all shifts and filter those that have the attribute
        return new ArrayList<>(); // Placeholder - will implement via repository query
    }

    /**
     * Calculate charge for a single attribute during a period
     */
    private BigDecimal calculateAttributeCharge(AttributeCost cost, LocalDate startDate, LocalDate endDate) {
        if (cost.getBillingUnit() == AttributeCost.BillingUnit.MONTHLY) {
            // For monthly billing, count number of months
            return calculateMonthlyCharge(cost.getPrice(), startDate, endDate);
        } else {
            // For daily billing, count number of days
            long daysCount = ChronoUnit.DAYS.between(startDate, endDate) + 1; // +1 to include end date
            return cost.getPrice().multiply(new BigDecimal(daysCount));
        }
    }

    /**
     * Calculate monthly charge
     * Conservative approach: charge for each partial or full month
     */
    private BigDecimal calculateMonthlyCharge(BigDecimal monthlyPrice, LocalDate startDate, LocalDate endDate) {
        // Count months between start and end
        // If it spans any part of a month, charge for that month

        long monthsBetween = 0;
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            monthsBetween++;
            currentDate = currentDate.plusMonths(1);
        }

        // Include the end month if we haven't passed it yet
        if (currentDate.isBefore(endDate) || currentDate.isEqual(endDate)) {
            monthsBetween++;
        }

        return monthlyPrice.multiply(new BigDecimal(monthsBetween));
    }

    /**
     * Check if two date ranges overlap
     */
    private boolean overlaps(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        LocalDate adjustedEnd1 = end1 != null ? end1 : LocalDate.now().plusYears(100);
        LocalDate adjustedEnd2 = end2 != null ? end2 : LocalDate.now().plusYears(100);

        return !start1.isAfter(adjustedEnd2) && !start2.isAfter(adjustedEnd1);
    }

    /**
     * Return the maximum of two dates
     */
    private LocalDate maxDate(LocalDate d1, LocalDate d2) {
        return d1.isAfter(d2) ? d1 : d2;
    }

    /**
     * Return the minimum of two dates
     */
    private LocalDate minDate(LocalDate d1, LocalDate d2) {
        return d1.isBefore(d2) ? d1 : d2;
    }

    /**
     * DTO: Result of shift charge calculation
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ShiftChargeResult {
        private Long shiftId;
        private String shiftNumber;
        private String shiftType;
        private LocalDate periodStartDate;
        private LocalDate periodEndDate;
        private List<ChargeLineItem> lineItems;
        private BigDecimal totalCharges;
    }

    /**
     * DTO: Individual charge line item
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ChargeLineItem {
        private String attributeCode;
        private String attributeName;
        private String attributeValue;
        private BigDecimal price;
        private String billingUnit;
        private LocalDate chargeStartDate;
        private LocalDate chargeEndDate;
        private long daysActive;
        private BigDecimal charge;
    }

    /**
     * DTO: Affected shift information
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AffectedShiftInfo {
        private Long shiftId;
        private String cabNumber;
        private String shiftType;
        private String ownerName;
        private LocalDate attributeStartDate;
        private LocalDate attributeEndDate;
        private String attributeValue;
        private BigDecimal monthlyCharge;
        private String billingUnit;
    }
}
