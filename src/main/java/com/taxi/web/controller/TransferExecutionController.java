package com.taxi.web.controller;

import com.taxi.domain.statement.model.ExecutionStatus;
import com.taxi.domain.statement.model.TransferExecution;
import com.taxi.domain.statement.service.TransferExecutionService;
import com.taxi.web.dto.statement.GenerateTransfersRequest;
import com.taxi.web.dto.statement.TransferExecutionDTO;
import com.taxi.web.dto.statement.TransferExecutionMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * REST API for managing transfer executions
 * Provides endpoints for generating, approving, rejecting, and viewing transfer executions
 */
@RestController
@RequestMapping("/transfer-executions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
@Slf4j
public class TransferExecutionController {

    private final TransferExecutionService executionService;
    private final TransferExecutionMapper mapper;

    /**
     * Generate transfer executions for a specific period
     * POST /transfer-executions/generate
     */
    @PostMapping("/generate")
    public ResponseEntity<?> generateForPeriod(
            @Valid @RequestBody GenerateTransfersRequest request
    ) {
        log.info("Generating transfer executions for period {} to {}",
                request.getPeriodFrom(), request.getPeriodTo());

        try {
            List<TransferExecution> executions = executionService.generateExecutionsForPeriod(
                    request.getPeriodFrom(),
                    request.getPeriodTo(),
                    getCurrentUserId()
            );

            List<TransferExecutionDTO> dtos = executions.stream()
                    .map(mapper::toDTO)
                    .collect(Collectors.toList());

            log.info("Generated {} transfer executions for period {} to {}",
                    dtos.size(), request.getPeriodFrom(), request.getPeriodTo());
            return ResponseEntity.ok(dtos);

        } catch (IllegalArgumentException e) {
            log.error("Validation error generating transfers: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Validation Error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);

        } catch (IllegalStateException e) {
            log.error("State error generating transfers: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Cannot Generate");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);

        } catch (Exception e) {
            log.error("Error generating transfer executions", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Server Error");
            error.put("message", "An unexpected error occurred. Please try again.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get executions for a period with optional status filter
     * GET /transfer-executions?periodFrom=2026-04-01&periodTo=2026-04-30&status=PENDING
     */
    @GetMapping
    public ResponseEntity<?> getExecutions(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodTo,
            @RequestParam(required = false) ExecutionStatus status
    ) {
        log.info("Fetching executions for period {} to {} with status {}",
                periodFrom, periodTo, status);

        try {
            List<TransferExecution> executions = executionService.getExecutionsForPeriod(
                    periodFrom, periodTo, status
            );

            List<TransferExecutionDTO> dtos = executions.stream()
                    .map(mapper::toDTO)
                    .collect(Collectors.toList());

            log.info("Found {} executions for period {} to {}",
                    dtos.size(), periodFrom, periodTo);
            return ResponseEntity.ok(dtos);

        } catch (Exception e) {
            log.error("Error fetching executions", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Server Error");
            error.put("message", "Failed to fetch executions: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get all executions (optionally filtered by status)
     * GET /transfer-executions/all?status=PENDING
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllExecutions(
            @RequestParam(required = false) ExecutionStatus status
    ) {
        log.info("Fetching all executions with status {}", status);

        try {
            List<TransferExecution> executions;
            if (status != null) {
                executions = executionService.getExecutionsByStatus(status);
            } else {
                executions = executionService.getAllExecutions();
            }

            List<TransferExecutionDTO> dtos = executions.stream()
                    .map(mapper::toDTO)
                    .collect(Collectors.toList());

            log.info("Found {} total executions", dtos.size());
            return ResponseEntity.ok(dtos);

        } catch (Exception e) {
            log.error("Error fetching all executions", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Server Error");
            error.put("message", "Failed to fetch executions: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get a single execution by ID
     * GET /transfer-executions/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getExecution(@PathVariable Long id) {
        log.info("Fetching execution with ID {}", id);

        try {
            TransferExecution execution = executionService.getExecution(id);
            TransferExecutionDTO dto = mapper.toDTO(execution);
            return ResponseEntity.ok(dto);

        } catch (IllegalArgumentException e) {
            log.error("Execution not found: {}", id);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Not Found");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);

        } catch (Exception e) {
            log.error("Error fetching execution {}", id, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Server Error");
            error.put("message", "Failed to fetch execution: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Approve an execution
     * POST /transfer-executions/{id}/approve
     * Body: { "notes": "Optional approval notes" }
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approve(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body
    ) {
        log.info("Approving execution {}", id);

        try {
            String notes = body != null ? body.get("notes") : null;
            TransferExecution execution = executionService.approve(id, getCurrentUserId(), notes);
            TransferExecutionDTO dto = mapper.toDTO(execution);

            log.info("Approved execution {} successfully", id);
            return ResponseEntity.ok(dto);

        } catch (IllegalArgumentException e) {
            log.error("Execution not found: {}", id);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Not Found");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);

        } catch (IllegalStateException e) {
            log.error("Cannot approve execution {}: {}", id, e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid Operation");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);

        } catch (Exception e) {
            log.error("Error approving execution {}", id, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Server Error");
            error.put("message", "An unexpected error occurred. Please try again.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Reject an execution
     * POST /transfer-executions/{id}/reject
     * Body: { "reason": "Required rejection reason" }
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<?> reject(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        log.info("Rejecting execution {}", id);

        try {
            String reason = body.get("reason");
            if (reason == null || reason.trim().isEmpty()) {
                log.error("Rejection reason is required");
                Map<String, String> error = new HashMap<>();
                error.put("error", "Validation Error");
                error.put("message", "Rejection reason is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            TransferExecution execution = executionService.reject(id, getCurrentUserId(), reason);
            TransferExecutionDTO dto = mapper.toDTO(execution);

            log.info("Rejected execution {} successfully", id);
            return ResponseEntity.ok(dto);

        } catch (IllegalArgumentException e) {
            log.error("Execution not found or validation error: {}", id);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Not Found");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);

        } catch (IllegalStateException e) {
            log.error("Cannot reject execution {}: {}", id, e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid Operation");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);

        } catch (Exception e) {
            log.error("Error rejecting execution {}", id, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Server Error");
            error.put("message", "An unexpected error occurred. Please try again.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Batch approve executions
     * POST /transfer-executions/batch-approve
     * Body: { "executionIds": [1, 2, 3], "notes": "Optional notes" }
     */
    @PostMapping("/batch-approve")
    public ResponseEntity<?> batchApprove(@RequestBody Map<String, Object> body) {
        log.info("Batch approving executions");

        try {
            @SuppressWarnings("unchecked")
            List<Integer> idInts = (List<Integer>) body.get("executionIds");
            if (idInts == null || idInts.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Validation Error");
                error.put("message", "Execution IDs are required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            List<Long> executionIds = idInts.stream()
                    .map(Integer::longValue)
                    .collect(Collectors.toList());

            String notes = (String) body.get("notes");

            List<TransferExecution> approved = executionService.batchApprove(
                    executionIds, getCurrentUserId(), notes
            );

            List<TransferExecutionDTO> dtos = approved.stream()
                    .map(mapper::toDTO)
                    .collect(Collectors.toList());

            log.info("Batch approved {} executions", dtos.size());
            Map<String, Object> response = new HashMap<>();
            response.put("approved", dtos);
            response.put("total", executionIds.size());
            response.put("successCount", dtos.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error batch approving executions", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Server Error");
            error.put("message", "An unexpected error occurred. Please try again.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Batch reject executions
     * POST /transfer-executions/batch-reject
     * Body: { "executionIds": [1, 2, 3], "reason": "Required rejection reason" }
     */
    @PostMapping("/batch-reject")
    public ResponseEntity<?> batchReject(@RequestBody Map<String, Object> body) {
        log.info("Batch rejecting executions");

        try {
            @SuppressWarnings("unchecked")
            List<Integer> idInts = (List<Integer>) body.get("executionIds");
            if (idInts == null || idInts.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Validation Error");
                error.put("message", "Execution IDs are required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            String reason = (String) body.get("reason");
            if (reason == null || reason.trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Validation Error");
                error.put("message", "Rejection reason is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            List<Long> executionIds = idInts.stream()
                    .map(Integer::longValue)
                    .collect(Collectors.toList());

            List<TransferExecution> rejected = executionService.batchReject(
                    executionIds, getCurrentUserId(), reason
            );

            List<TransferExecutionDTO> dtos = rejected.stream()
                    .map(mapper::toDTO)
                    .collect(Collectors.toList());

            log.info("Batch rejected {} executions", dtos.size());
            Map<String, Object> response = new HashMap<>();
            response.put("rejected", dtos);
            response.put("total", executionIds.size());
            response.put("successCount", dtos.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error batch rejecting executions", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Server Error");
            error.put("message", "An unexpected error occurred. Please try again.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get current user ID from security context
     * TODO: Implement this method to extract user ID from Spring Security context
     */
    private Long getCurrentUserId() {
        // For now, return a default value
        // This should be implemented to extract from SecurityContextHolder
        return 1L;
    }
}
