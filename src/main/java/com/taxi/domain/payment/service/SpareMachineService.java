package com.taxi.domain.payment.service;

import com.taxi.domain.cab.model.Cab;
import com.taxi.domain.cab.model.CabStatus;
import com.taxi.domain.cab.repository.CabRepository;
import com.taxi.domain.payment.model.SpareMachine;
import com.taxi.domain.payment.model.SpareMachineAssignment;
import com.taxi.domain.payment.repository.SpareMachineRepository;
import com.taxi.domain.payment.repository.SpareMachineAssignmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SpareMachineService {

    private static final int VIRTUAL_CAB_MIN = 10000;
    private static final int VIRTUAL_CAB_MAX = 11000;

    private final SpareMachineRepository spareMachineRepository;
    private final SpareMachineAssignmentRepository assignmentRepository;
    private final CabRepository cabRepository;

    /**
     * Create a new spare machine with auto-assigned virtual cab ID and virtual cab
     */
    public SpareMachine createSpare(String machineName, String merchantNumber) {
        log.info("Creating spare machine: {} with merchant: {}", machineName, merchantNumber);

        // Find next available virtual cab ID
        Integer nextVirtualCabId = getNextAvailableVirtualCabId();

        SpareMachine spare = SpareMachine.builder()
            .machineName(machineName)
            .virtualCabId(nextVirtualCabId)
            .merchantNumber(merchantNumber)
            .build();

        spare = spareMachineRepository.save(spare);

        // Create virtual cab if it doesn't exist
        String virtualCabNumber = nextVirtualCabId.toString();
        if (!cabRepository.existsByCabNumber(virtualCabNumber)) {
            log.info("Creating virtual cab for spare machine: {}", spare.getId());
            Cab virtualCab = Cab.builder()
                .cabNumber(virtualCabNumber)
                .registrationNumber("VIRTUAL-" + virtualCabNumber)
                .make("Virtual")
                .model("Credit Card Terminal")
                .notes("Virtual cab for credit card payment terminal - " + machineName)
                .status(CabStatus.ACTIVE)
                .build();
            cabRepository.save(virtualCab);
            log.info("Virtual cab {} created for spare machine {}", virtualCabNumber, spare.getId());
        }

        return spare;
    }

    /**
     * Assign a spare machine to a real cab
     */
    public SpareMachineAssignment assignToRealCab(Long spareId, Integer realCabNumber, String shift,
                                                   LocalDateTime assignedAt, String notes, String createdBy) {
        log.info("Assigning spare {} to cab {} (shift: {}) starting at {}", spareId, realCabNumber, shift, assignedAt);

        SpareMachine spare = spareMachineRepository.findById(spareId)
            .orElseThrow(() -> new RuntimeException("Spare machine not found: " + spareId));

        // Check if already assigned
        Optional<SpareMachineAssignment> existing = assignmentRepository.findBySpareMachineIdAndReturnedAtIsNull(spareId);
        if (existing.isPresent()) {
            throw new RuntimeException("Spare machine " + spareId + " is already assigned");
        }

        LocalDateTime actualAssignedAt = assignedAt != null ? assignedAt : LocalDateTime.now();
        String actualShift = shift != null ? shift : "BOTH";

        // Create assignment
        SpareMachineAssignment assignment = SpareMachineAssignment.builder()
            .spareMachineId(spareId)
            .realCabNumber(realCabNumber)
            .shift(actualShift)
            .assignedAt(actualAssignedAt)
            .notes(notes)
            .createdBy(createdBy)
            .build();

        assignment = assignmentRepository.save(assignment);

        log.info("Spare {} assigned successfully with assignment ID: {}", spareId, assignment.getId());
        return assignment;
    }

    /**
     * Return a spare machine (end its assignment)
     */
    public SpareMachineAssignment returnSpare(Long assignmentId, LocalDateTime returnedAt) {
        log.info("Returning spare assignment: {}", assignmentId);

        SpareMachineAssignment assignment = assignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new RuntimeException("Assignment not found: " + assignmentId));

        if (assignment.getReturnedAt() != null) {
            throw new RuntimeException("Assignment " + assignmentId + " already returned");
        }

        assignment.setReturnedAt(returnedAt != null ? returnedAt : LocalDateTime.now());
        assignment = assignmentRepository.save(assignment);

        log.info("Spare assignment {} returned at {}", assignmentId, assignment.getReturnedAt());
        return assignment;
    }

    /**
     * Core routing logic: resolve transaction cab number for spare machines
     * - If merchant is associated with a spare machine, check assignment and get real cab
     * - Returns null if merchant is not a spare machine
     */
    @Transactional(readOnly = true)
    public Integer resolveTransactionCab(String merchantNumber, LocalDateTime transactionTime) {
        log.debug("Resolving transaction cab for spare merchant: {} at time: {}", merchantNumber, transactionTime);

        if (merchantNumber == null || transactionTime == null) {
            log.warn("Merchant number or transaction time is null");
            return null;
        }

        // Check if this merchant is associated with a spare machine
        SpareMachine spare = spareMachineRepository.findByMerchantNumber(merchantNumber)
            .orElse(null);

        if (spare == null) {
            log.debug("Merchant {} is not a spare machine merchant", merchantNumber);
            return null;
        }

        log.debug("Found spare machine {} with merchant {}", spare.getId(), merchantNumber);

        // Find active assignment at transaction time
        List<SpareMachineAssignment> assignments = assignmentRepository.findBySpareMachineIdOrderByAssignedAtDesc(spare.getId());

        for (SpareMachineAssignment assignment : assignments) {
            if (assignment.isActive(transactionTime)) {
                log.debug("Found active assignment for spare {}: real cab {}", spare.getId(), assignment.getRealCabNumber());
                return assignment.getRealCabNumber();
            }
        }

        log.warn("No active assignment found for spare {} at time {}", spare.getId(), transactionTime);
        return null;  // Return null if no active assignment (merchant should be looked up in merchant2cab instead)
    }

    /**
     * Get all currently active assignments
     */
    @Transactional(readOnly = true)
    public List<SpareMachineAssignment> getCurrentAssignments() {
        return assignmentRepository.findByReturnedAtIsNullOrderByAssignedAtDesc();
    }

    /**
     * Get assignment history for a spare machine
     */
    @Transactional(readOnly = true)
    public List<SpareMachineAssignment> getHistoryForSpare(Long spareId) {
        return assignmentRepository.findBySpareMachineIdOrderByAssignedAtDesc(spareId);
    }

    /**
     * Get all spare machines (active only)
     */
    @Transactional(readOnly = true)
    public List<SpareMachine> getAllSpares() {
        List<SpareMachine> all = spareMachineRepository.findAll();
        return all.stream()
            .filter(s -> s.getIsActive() != null && s.getIsActive())
            .collect(Collectors.toList());
    }

    /**
     * Get a specific spare machine
     */
    @Transactional(readOnly = true)
    public Optional<SpareMachine> getSpare(Long id) {
        return spareMachineRepository.findById(id);
    }

    /**
     * Deactivate a spare machine (soft delete - preserves history)
     * Cannot deactivate if it has an active assignment
     */
    public SpareMachine deactivateSpare(Long id) {
        log.info("Deactivating spare machine: {}", id);

        SpareMachine spare = spareMachineRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Spare machine not found: " + id));

        // Check if spare has active assignment
        Optional<SpareMachineAssignment> activeAssignment = assignmentRepository.findBySpareMachineIdAndReturnedAtIsNull(id);
        if (activeAssignment.isPresent()) {
            throw new RuntimeException("Cannot deactivate a spare machine with an active assignment. Return it first.");
        }

        spare.setIsActive(false);
        spare = spareMachineRepository.save(spare);

        log.info("Spare machine {} deactivated", id);
        return spare;
    }

    /**
     * Activate a spare machine
     */
    public SpareMachine activateSpare(Long id) {
        log.info("Activating spare machine: {}", id);

        SpareMachine spare = spareMachineRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Spare machine not found: " + id));

        spare.setIsActive(true);
        spare = spareMachineRepository.save(spare);

        log.info("Spare machine {} activated", id);
        return spare;
    }

    /**
     * Helper: check if a cab ID is in the virtual range (spare machines)
     */
    private boolean isVirtualCabId(Integer cabId) {
        return cabId != null && cabId >= VIRTUAL_CAB_MIN && cabId <= VIRTUAL_CAB_MAX;
    }

    /**
     * Helper: find next available virtual cab ID
     */
    private Integer getNextAvailableVirtualCabId() {
        // In a real system, this could be more sophisticated
        // For now, find the max and add 1
        List<SpareMachine> allSpares = spareMachineRepository.findAll();

        if (allSpares.isEmpty()) {
            return VIRTUAL_CAB_MIN;
        }

        Integer maxVirtualCabId = allSpares.stream()
            .map(SpareMachine::getVirtualCabId)
            .max(Integer::compareTo)
            .orElse(VIRTUAL_CAB_MIN - 1);

        Integer nextId = maxVirtualCabId + 1;

        if (nextId > VIRTUAL_CAB_MAX) {
            throw new RuntimeException("No more virtual cab IDs available (max: " + VIRTUAL_CAB_MAX + ")");
        }

        return nextId;
    }
}
