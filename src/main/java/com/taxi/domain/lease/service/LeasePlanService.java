package com.taxi.domain.lease.service;

import com.taxi.domain.lease.model.LeasePlan;
import com.taxi.domain.lease.model.LeaseRate;
import com.taxi.domain.lease.repository.LeasePlanRepository;
import com.taxi.domain.lease.repository.LeaseRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Service for managing lease plans with strict business rules:
 * 1. Plans cannot be deleted (only deactivated)
 * 2. Rates cannot be edited (create new plan instead)
 * 3. Only one active plan at a time (no date overlaps)
 * 4. Plans auto-deactivate when end date passes
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LeasePlanService {

    private final LeasePlanRepository leasePlanRepository;
    private final LeaseRateRepository leaseRateRepository;

    /**
     * Create a new lease plan with validation
     * Ensures no date overlap with existing plans
     */
    @Transactional
    public LeasePlan createPlan(LeasePlan plan) {
        log.info("Creating lease plan: {}", plan.getPlanName());
        
        // Validate no overlap with existing plans
        validateNoOverlap(plan.getEffectiveFrom(), plan.getEffectiveTo(), null);
        
        // Validate dates
        if (plan.getEffectiveTo() != null && plan.getEffectiveTo().isBefore(plan.getEffectiveFrom())) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }
        
        LeasePlan saved = leasePlanRepository.save(plan);
        log.info("Created lease plan ID: {}", saved.getId());
        return saved;
    }

    /**
     * Update ONLY allowed fields (name, notes, end date to close plan)
     * Rates and effective_from CANNOT be changed
     */
    @Transactional
    public LeasePlan updatePlan(Long id, LeasePlan updates) {
        log.info("Updating lease plan ID: {}", id);
        
        LeasePlan plan = leasePlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lease plan not found: " + id));
        
        // ONLY allow updating these fields:
        if (updates.getPlanName() != null) {
            plan.setPlanName(updates.getPlanName());
        }
        if (updates.getNotes() != null) {
            plan.setNotes(updates.getNotes());
        }
        
        // Allow setting end date to close the plan (but not changing it if already set)
        if (updates.getEffectiveTo() != null) {
            if (plan.getEffectiveTo() == null) {
                // First time setting end date - validate no overlap
                validateNoOverlap(plan.getEffectiveFrom(), updates.getEffectiveTo(), id);
                plan.setEffectiveTo(updates.getEffectiveTo());
                
                // Auto-deactivate if end date is in the past
                if (updates.getEffectiveTo().isBefore(LocalDate.now())) {
                    plan.setActive(false);
                }
            } else {
                throw new IllegalArgumentException(
                    "Cannot change end date once set. Create new plan instead.");
            }
        }
        
        return leasePlanRepository.save(plan);
    }

    /**
     * Deactivate a plan by setting end date
     */
    @Transactional
    public LeasePlan deactivatePlan(Long id, LocalDate endDate) {
        log.info("Deactivating lease plan ID: {} with end date: {}", id, endDate);
        
        LeasePlan plan = leasePlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lease plan not found: " + id));
        
        if (!plan.isActive()) {
            throw new IllegalStateException("Plan is already inactive");
        }
        
        if (endDate.isBefore(plan.getEffectiveFrom())) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }
        
        validateNoOverlap(plan.getEffectiveFrom(), endDate, id);
        
        plan.deactivate(endDate);
        
        LeasePlan saved = leasePlanRepository.save(plan);
        log.info("Deactivated plan ID: {}, end date: {}", id, endDate);
        return saved;
    }

    /**
     * DELETION IS NOT ALLOWED
     */
    @Transactional
    public void deletePlan(Long id) {
        throw new UnsupportedOperationException(
            "Lease plans cannot be deleted (audit trail requirement). Use deactivate instead."
        );
    }

    @Transactional(readOnly = true)
    public LeasePlan getPlanById(Long id) {
        return leasePlanRepository.findByIdWithRates(id)
                .orElseThrow(() -> new RuntimeException("Lease plan not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<LeasePlan> getAllPlans() {
        return leasePlanRepository.findAllWithRates();
    }

    @Transactional(readOnly = true)
    public List<LeasePlan> getActivePlans() {
        return leasePlanRepository.findAllWithRates().stream()
                .filter(LeasePlan::isActive)
                .toList();
    }

    @Transactional(readOnly = true)
    public LeasePlan getPlanActiveOnDate(LocalDate date) {
        return leasePlanRepository.findAllWithRates().stream()
                .filter(p -> p.isActiveOn(date))
                .findFirst()
                .orElse(null);
    }

    @Transactional
    public LeasePlan addRates(Long planId, List<LeaseRate> rates) {
        log.info("Adding {} rates to plan ID: {}", rates.size(), planId);
        
        LeasePlan plan = getPlanById(planId);
        
        for (LeaseRate rate : rates) {
            rate.setLeasePlan(plan);
            plan.addLeaseRate(rate);
        }
        
        LeasePlan saved = leasePlanRepository.save(plan);
        log.info("Added rates to plan ID: {}", planId);
        return saved;
    }


    @Transactional
    public LeasePlan addRate(Long planId, LeaseRate rate) {
        log.info("Adding {} rate to plan ID: {}", planId);
        
        LeasePlan plan = getPlanById(planId);
        rate.setLeasePlan(plan);
        plan.addLeaseRate(rate);
    
        
        LeasePlan saved = leasePlanRepository.save(plan);
        log.info("Added rates to plan ID: {}", planId);
        return saved;
    }

    /**
     * RATE EDITING IS NOT ALLOWED
     */
    @Transactional
    public void updateRate(Long rateId, LeaseRate updates) {
        throw new UnsupportedOperationException(
            "Lease rates cannot be edited. Create a new plan with new rates instead."
        );
    }

    /**
     * RATE DELETION IS NOT ALLOWED
     */
    @Transactional
    public void deleteRate(Long rateId) {
        throw new UnsupportedOperationException(
            "Lease rates cannot be deleted. Create a new plan with new rates instead."
        );
    }

    /**
     * Auto-deactivate expired plans
     */
    @Transactional
    public void autoDeactivateExpiredPlans() {
        LocalDate today = LocalDate.now();
        List<LeasePlan> plans = leasePlanRepository.findAll();
        
        int deactivated = 0;
        for (LeasePlan plan : plans) {
            if (plan.isActive() && 
                plan.getEffectiveTo() != null && 
                plan.getEffectiveTo().isBefore(today)) {
                
                plan.setActive(false);
                leasePlanRepository.save(plan);
                deactivated++;
                log.info("Auto-deactivated plan ID: {}", plan.getId());
            }
        }
        
        if (deactivated > 0) {
            log.info("Auto-deactivated {} expired plans", deactivated);
        }
    }

    /**
     * Validate no overlap - only ONE plan can be active at a time
     */
    private void validateNoOverlap(LocalDate start, LocalDate end, Long excludePlanId) {
        List<LeasePlan> allPlans = leasePlanRepository.findAll();
        
        for (LeasePlan existing : allPlans) {
            if (excludePlanId != null && existing.getId().equals(excludePlanId)) {
                continue;
            }
            
            if (existing.overlapsWith(start, end)) {
                throw new IllegalArgumentException(
                    String.format(
                        "Date range overlaps with '%s' (%s to %s). " +
                        "Only one plan can be active at a time.",
                        existing.getPlanName(),
                        existing.getEffectiveFrom(),
                        existing.getEffectiveTo() != null ? existing.getEffectiveTo() : "ongoing"
                    )
                );
            }
        }
    }
}
