package com.taxi.web.controller;

import com.taxi.domain.payment.dto.AssignSpareMachineRequest;
import com.taxi.domain.payment.dto.ReturnSpareMachineRequest;
import com.taxi.domain.payment.dto.SpareMachineAssignmentDTO;
import com.taxi.domain.payment.dto.SpareMachineDTO;
import com.taxi.domain.payment.model.SpareMachine;
import com.taxi.domain.payment.model.SpareMachineAssignment;
import com.taxi.domain.payment.service.SpareMachineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/payment/spare-machines")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class SpareMachineController {

    private final SpareMachineService spareMachineService;

    /**
     * Create a new spare machine with auto-assigned virtual cab ID
     */
    @PostMapping
    public ResponseEntity<SpareMachineDTO> createSpareMachine(
            @RequestParam String machineName,
            @RequestParam String merchantNumber) {
        SpareMachine spare = spareMachineService.createSpare(machineName, merchantNumber);
        return ResponseEntity.status(HttpStatus.CREATED).body(SpareMachineDTO.fromEntity(spare));
    }

    /**
     * Get all spare machines
     */
    @GetMapping
    public ResponseEntity<List<SpareMachineDTO>> getAllSpareMachines() {
        List<SpareMachine> spares = spareMachineService.getAllSpares();
        List<SpareMachineDTO> dtos = spares.stream()
                .map(SpareMachineDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get a specific spare machine
     */
    @GetMapping("/{id}")
    public ResponseEntity<SpareMachineDTO> getSpareMachine(@PathVariable Long id) {
        return spareMachineService.getSpare(id)
                .map(spare -> ResponseEntity.ok(SpareMachineDTO.fromEntity(spare)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Deactivate a spare machine (soft delete - preserves history)
     */
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<SpareMachineDTO> deactivateSpareMachine(@PathVariable Long id) {
        SpareMachine spare = spareMachineService.deactivateSpare(id);
        return ResponseEntity.ok(SpareMachineDTO.fromEntity(spare));
    }

    /**
     * Activate a spare machine
     */
    @PostMapping("/{id}/activate")
    public ResponseEntity<SpareMachineDTO> activateSpareMachine(@PathVariable Long id) {
        SpareMachine spare = spareMachineService.activateSpare(id);
        return ResponseEntity.ok(SpareMachineDTO.fromEntity(spare));
    }

    /**
     * Assign a spare machine to a real cab
     */
    @PostMapping("/{id}/assign")
    public ResponseEntity<SpareMachineAssignmentDTO> assignSpareToRealCab(
            @PathVariable Long id,
            @Valid @RequestBody AssignSpareMachineRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        SpareMachineAssignment assignment = spareMachineService.assignToRealCab(
                id,
                request.getRealCabNumber(),
                request.getShift(),
                request.getAssignedAt(),
                request.getNotes(),
                userDetails.getUsername()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(SpareMachineAssignmentDTO.fromEntity(assignment));
    }

    /**
     * Return a spare machine (end its assignment)
     */
    @PostMapping("/assignments/{assignmentId}/return")
    public ResponseEntity<SpareMachineAssignmentDTO> returnSpareMachine(
            @PathVariable Long assignmentId,
            @Valid @RequestBody ReturnSpareMachineRequest request) {
        SpareMachineAssignment assignment = spareMachineService.returnSpare(assignmentId, request.getReturnedAt());
        return ResponseEntity.ok(SpareMachineAssignmentDTO.fromEntity(assignment));
    }

    /**
     * Get current active assignments
     */
    @GetMapping("/assignments/current")
    public ResponseEntity<List<SpareMachineAssignmentDTO>> getCurrentAssignments() {
        List<SpareMachineAssignment> assignments = spareMachineService.getCurrentAssignments();
        List<SpareMachineAssignmentDTO> dtos = assignments.stream()
                .map(SpareMachineAssignmentDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get assignment history for a spare machine
     */
    @GetMapping("/{spareId}/assignments")
    public ResponseEntity<List<SpareMachineAssignmentDTO>> getAssignmentHistoryForSpare(@PathVariable Long spareId) {
        List<SpareMachineAssignment> assignments = spareMachineService.getHistoryForSpare(spareId);
        List<SpareMachineAssignmentDTO> dtos = assignments.stream()
                .map(SpareMachineAssignmentDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
}
