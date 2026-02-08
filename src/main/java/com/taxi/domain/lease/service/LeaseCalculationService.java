package com.taxi.domain.lease.service;

import com.taxi.domain.cab.model.CabType;
import com.taxi.domain.lease.model.LeasePlan;
import com.taxi.domain.lease.model.LeaseRate;
import com.taxi.domain.lease.repository.LeasePlanRepository;
import com.taxi.domain.lease.repository.LeaseRateRepository;
import com.taxi.domain.shift.model.CabShift;
import com.taxi.domain.shift.model.ShiftType;
import com.taxi.domain.cab.model.Cab;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Domain service for calculating lease amounts
 * 
 * Lease Formula: Total Lease = Base Rate + (Miles Driven × Mileage Rate)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class LeaseCalculationService {

    private final LeasePlanRepository leasePlanRepository;
    private final LeaseRateRepository leaseRateRepository;

    /**
     * Calculate lease amount for a shift on a specific date
     * 
     * @param cab The cab being operated
     * @param shift The shift being operated
     * @param date The date of operation
     * @param milesDriven Total miles driven during the shift
     * @return LeaseAmount containing breakdown of charges
     */
    public LeaseAmount calculateLease(Cab cab, CabShift shift, LocalDate date, BigDecimal milesDriven) {
        log.debug("Calculating lease for cab: {}, shift: {}, date: {}, miles: {}", 
            cab.getCabNumber(), shift.getShiftType(), date, milesDriven);

        // Get the lease plan active on the given date
        LeasePlan leasePlan = leasePlanRepository.findPlanActiveOnDate(date)
            .orElseThrow(() -> new LeaseCalculationException(
                "No active lease plan found for date: " + date));

        // Determine criteria for rate lookup
        // Attributes are now at shift level, not cab level
        CabType cabType = shift.getCabType();
        Boolean hasAirportLicense = shift.getHasAirportLicense();
        ShiftType shiftType = shift.getShiftType();
        DayOfWeek dayOfWeek = date.getDayOfWeek();

        log.debug("Lease criteria - CabType: {}, Airport: {}, Shift: {}, Day: {}", 
            cabType, hasAirportLicense, shiftType, dayOfWeek);

        // Find the applicable lease rate
        LeaseRate leaseRate = leaseRateRepository.findRateByCriteria(
                leasePlan.getId(), cabType, hasAirportLicense, shiftType, dayOfWeek)
            .orElseThrow(() -> new LeaseCalculationException(
                String.format("No lease rate found for criteria: CabType=%s, Airport=%s, Shift=%s, Day=%s",
                    cabType, hasAirportLicense, shiftType, dayOfWeek)));

        // Calculate lease components
        BigDecimal baseRate = leaseRate.getBaseRate();
        BigDecimal mileageRate = leaseRate.getMileageRate();
        BigDecimal mileageCharge = mileageRate.multiply(milesDriven);
        BigDecimal totalLease = baseRate.add(mileageCharge);

        log.info("Lease calculated - Base: {}, Mileage: {} × {} = {}, Total: {}", 
            baseRate, mileageRate, milesDriven, mileageCharge, totalLease);

        return LeaseAmount.builder()
            .baseRate(baseRate)
            .mileageRate(mileageRate)
            .milesDriven(milesDriven)
            .mileageCharge(mileageCharge)
            .totalLeaseAmount(totalLease)
            .leasePlanId(leasePlan.getId())
            .leasePlanName(leasePlan.getPlanName())
            .leaseRateId(leaseRate.getId())
            .calculatedDate(LocalDate.now())
            .build();
    }

    /**
     * Calculate lease without miles (base rate only)
     * Used for estimates before shift completion
     */
    public LeaseAmount calculateBaseLease(Cab cab, CabShift shift, LocalDate date) {
        return calculateLease(cab, shift, date, BigDecimal.ZERO);
    }

    /**
     * Get applicable lease rate for preview (without calculation)
     */
    public LeaseRate getApplicableRate(Cab cab, CabShift shift, LocalDate date) {
        LeasePlan leasePlan = leasePlanRepository.findPlanActiveOnDate(date)
            .orElseThrow(() -> new LeaseCalculationException(
                "No active lease plan found for date: " + date));

        // Attributes are now at shift level, not cab level
        CabType cabType = shift.getCabType();
        Boolean hasAirportLicense = shift.getHasAirportLicense();
        ShiftType shiftType = shift.getShiftType();
        DayOfWeek dayOfWeek = date.getDayOfWeek();

        return leaseRateRepository.findRateByCriteria(
                leasePlan.getId(), cabType, hasAirportLicense, shiftType, dayOfWeek)
            .orElseThrow(() -> new LeaseCalculationException(
                "No lease rate found for given criteria"));
    }
    
    /**
     * Find applicable rate based on CabType, airport license, and logon time
     * Used for DriverShift-based calculations
     */
    public LeaseRate findApplicableRate(CabType cabType, boolean hasAirportLicense, 
                                       java.time.LocalDateTime logonTime) {
        LocalDate date = logonTime.toLocalDate();
        
        // Determine shift type based on logon hour
        // DAY: 00:00-11:59, NIGHT: 12:00-23:59
        ShiftType shiftType = logonTime.getHour() < 12 ? ShiftType.DAY : ShiftType.NIGHT;
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        
        LeasePlan leasePlan = leasePlanRepository.findPlanActiveOnDate(date)
            .orElseThrow(() -> new LeaseCalculationException(
                "No active lease plan found for date: " + date));
        
        return leaseRateRepository.findRateByCriteria(
                leasePlan.getId(), cabType, hasAirportLicense, shiftType, dayOfWeek)
            .orElse(null);
    }

    /**
     * Value object containing lease calculation results
     */
    @lombok.Data
    @lombok.Builder
    public static class LeaseAmount {
        private BigDecimal baseRate;
        private BigDecimal mileageRate;
        private BigDecimal milesDriven;
        private BigDecimal mileageCharge;
        private BigDecimal totalLeaseAmount;
        private Long leasePlanId;
        private String leasePlanName;
        private Long leaseRateId;
        private LocalDate calculatedDate;

        public String getFormattedSummary() {
            return String.format("Base: $%.2f + Mileage: (%.4f × %.2f) = $%.2f | Total: $%.2f",
                baseRate, mileageRate, milesDriven, mileageCharge, totalLeaseAmount);
        }
    }

    /**
     * Exception for lease calculation errors
     */
    public static class LeaseCalculationException extends RuntimeException {
        public LeaseCalculationException(String message) {
            super(message);
        }

        public LeaseCalculationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
