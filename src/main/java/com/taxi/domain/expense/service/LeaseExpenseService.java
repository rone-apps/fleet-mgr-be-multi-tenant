package com.taxi.domain.expense.service;

import com.taxi.domain.cab.model.Cab;
import com.taxi.domain.cab.model.CabType;
import com.taxi.domain.cab.repository.CabRepository;
import com.taxi.domain.expense.model.LeaseExpense;
import com.taxi.domain.expense.repository.LeaseExpenseRepository;
import com.taxi.domain.lease.model.LeasePlan;
import com.taxi.domain.lease.model.LeaseRate;
import com.taxi.domain.lease.repository.LeasePlanRepository;
import com.taxi.domain.lease.repository.LeaseRateRepository;
import com.taxi.domain.shift.model.ShiftType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaseExpenseService {

    private final LeaseExpenseRepository leaseExpenseRepository;
    private final LeasePlanRepository leasePlanRepository;
    private final LeaseRateRepository leaseRateRepository;
    private final CabRepository cabRepository;

    /**
     * Calculate and create a lease expense
     */
    @Transactional
    public LeaseExpense createLeaseExpense(
            Long driverId,
            Long cabId,
            LocalDate leaseDate,
            ShiftType shiftType,
            BigDecimal milesDriven,
            Long shiftId,
            String notes
    ) {
        log.info("Creating lease expense for driver {} on {}", driverId, leaseDate);

        // Get cab details
        Cab cab = cabRepository.findById(cabId)
            .orElseThrow(() -> new RuntimeException("Cab not found: " + cabId));

        CabType cabType = cab.getCabType();
        Boolean hasAirportLicense = cab.getHasAirportLicense();
        DayOfWeek dayOfWeek = leaseDate.getDayOfWeek();

        // Get active lease plan - find plan where effective_from <= date AND (effective_to IS NULL OR effective_to >= date)
        LeasePlan activePlan = leasePlanRepository.findAll().stream()
            .filter(p -> !p.getEffectiveFrom().isAfter(leaseDate) && 
                        (p.getEffectiveTo() == null || !p.getEffectiveTo().isBefore(leaseDate)))
            .filter(p -> p.isActive())
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No active lease plan found for date: " + leaseDate));

        // Get applicable lease rate - find by plan and all criteria
        LeaseRate leaseRate = leaseRateRepository.findAll().stream()
            .filter(r -> r.getLeasePlan().getId().equals(activePlan.getId()))
            .filter(r -> r.getCabType() == cabType)
            .filter(r -> r.isHasAirportLicense() == Boolean.TRUE.equals(hasAirportLicense))
            .filter(r -> r.getShiftType() == shiftType)
            .filter(r -> r.getDayOfWeek() == dayOfWeek)
            .findFirst()
            .orElseThrow(() -> new RuntimeException(
                String.format("No lease rate found for: plan=%d, cabType=%s, airport=%s, shift=%s, day=%s",
                    activePlan.getId(), cabType, hasAirportLicense, shiftType, dayOfWeek)
            ));

        // Calculate amounts
        BigDecimal baseAmount = leaseRate.getBaseRate();
        BigDecimal mileageAmount = BigDecimal.ZERO;
        
        if (milesDriven != null && milesDriven.compareTo(BigDecimal.ZERO) > 0) {
            mileageAmount = milesDriven.multiply(leaseRate.getMileageRate());
        }

        BigDecimal totalAmount = baseAmount.add(mileageAmount);

        // Create lease expense
        LeaseExpense leaseExpense = LeaseExpense.builder()
            .driverId(driverId)
            .cabId(cabId)
            .leaseDate(leaseDate)
            .dayOfWeek(dayOfWeek)
            .shiftType(shiftType)
            .cabType(cabType)
            .hasAirportLicense(hasAirportLicense)
            .leasePlan(activePlan)
            .leaseRate(leaseRate)
            .baseAmount(baseAmount)
            .milesDriven(milesDriven)
            .mileageAmount(mileageAmount)
            .totalAmount(totalAmount)
            .shiftId(shiftId)
            .notes(notes)
            .isPaid(false)
            .build();

        LeaseExpense saved = leaseExpenseRepository.save(leaseExpense);
        log.info("Created lease expense: id={}, total={}", saved.getId(), saved.getTotalAmount());
        
        return saved;
    }

    /**
     * Get lease expense by ID
     */
    @Transactional(readOnly = true)
    public LeaseExpense getById(Long id) {
        return leaseExpenseRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Lease expense not found: " + id));
    }

    /**
     * Get lease expenses between dates
     */
    @Transactional(readOnly = true)
    public List<LeaseExpense> getLeaseExpensesBetween(LocalDate startDate, LocalDate endDate) {
        return leaseExpenseRepository.findByLeaseDateBetween(startDate, endDate);
    }

    /**
     * Get lease expenses for a driver
     */
    @Transactional(readOnly = true)
    public List<LeaseExpense> getLeaseExpensesByDriver(Long driverId) {
        return leaseExpenseRepository.findByDriverIdOrderByLeaseDateDesc(driverId);
    }

    /**
     * Get lease expenses for a driver between dates
     */
    @Transactional(readOnly = true)
    public List<LeaseExpense> getLeaseExpensesByDriverBetween(
            Long driverId, 
            LocalDate startDate, 
            LocalDate endDate
    ) {
        return leaseExpenseRepository.findByDriverIdAndLeaseDateBetween(driverId, startDate, endDate);
    }

    /**
     * Get unpaid lease expenses
     */
    @Transactional(readOnly = true)
    public List<LeaseExpense> getUnpaidLeaseExpenses() {
        return leaseExpenseRepository.findUnpaid();
    }

    /**
     * Get unpaid lease expenses for a driver
     */
    @Transactional(readOnly = true)
    public List<LeaseExpense> getUnpaidLeaseExpensesByDriver(Long driverId) {
        return leaseExpenseRepository.findUnpaidByDriverId(driverId);
    }

    /**
     * Get total unpaid amount for a driver
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalUnpaidByDriver(Long driverId) {
        return leaseExpenseRepository.getTotalUnpaidByDriverId(driverId);
    }

    /**
     * Mark lease expense as paid
     */
    @Transactional
    public LeaseExpense markAsPaid(Long id, LocalDate paidDate) {
        LeaseExpense leaseExpense = getById(id);
        leaseExpense.setIsPaid(true);
        leaseExpense.setPaidDate(paidDate != null ? paidDate : LocalDate.now());
        
        LeaseExpense updated = leaseExpenseRepository.save(leaseExpense);
        log.info("Marked lease expense {} as paid on {}", id, updated.getPaidDate());
        
        return updated;
    }

    /**
     * Mark lease expense as unpaid
     */
    @Transactional
    public LeaseExpense markAsUnpaid(Long id) {
        LeaseExpense leaseExpense = getById(id);
        leaseExpense.setIsPaid(false);
        leaseExpense.setPaidDate(null);
        
        LeaseExpense updated = leaseExpenseRepository.save(leaseExpense);
        log.info("Marked lease expense {} as unpaid", id);
        
        return updated;
    }

    /**
     * Update notes
     */
    @Transactional
    public LeaseExpense updateNotes(Long id, String notes) {
        LeaseExpense leaseExpense = getById(id);
        leaseExpense.setNotes(notes);
        return leaseExpenseRepository.save(leaseExpense);
    }

    /**
     * Delete lease expense
     */
    @Transactional
    public void delete(Long id) {
        LeaseExpense leaseExpense = getById(id);
        leaseExpenseRepository.delete(leaseExpense);
        log.info("Deleted lease expense: {}", id);
    }
}
