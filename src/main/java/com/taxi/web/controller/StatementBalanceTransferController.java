package com.taxi.web.controller;

import com.taxi.domain.statement.model.StatementBalanceTransfer;
import com.taxi.domain.statement.model.StatementBalanceTransferHistory;
import com.taxi.domain.statement.model.TransferStatus;
import com.taxi.domain.statement.model.TransferType;
import com.taxi.domain.statement.model.BalanceDirection;
import com.taxi.domain.statement.service.StatementBalanceTransferService;
import com.taxi.web.dto.statement.CreateStatementBalanceTransferRequest;
import com.taxi.web.dto.statement.StatementBalanceTransferDTO;
import com.taxi.web.dto.statement.StatementBalanceTransferHistoryDTO;
import com.taxi.web.dto.statement.StatementBalanceTransferMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * REST API for managing statement balance transfers
 */
@RestController
@RequestMapping("/statement-balance-transfers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
@Slf4j
public class StatementBalanceTransferController {

    private final StatementBalanceTransferService transferService;
    private final StatementBalanceTransferMapper mapper;
    private final com.taxi.domain.statement.repository.StatementRepository statementRepository;

    /**
     * Calculate current balance for a person
     * GET /statement-balance-transfers/calculate-balance/{personId}
     */
    @GetMapping("/calculate-balance/{personId}")
    public ResponseEntity<?> calculateBalance(@PathVariable Long personId) {
        log.info("Calculating current balance for person {}", personId);

        try {
            // Get the latest finalized or paid statement for this person
            java.util.Optional<com.taxi.domain.statement.model.Statement> statementOpt =
                statementRepository.findLatestByPersonIdAndStatusInAndPeriodToBefore(
                    personId,
                    java.util.List.of(
                        com.taxi.domain.statement.model.StatementStatus.FINALIZED,
                        com.taxi.domain.statement.model.StatementStatus.PAID
                    ),
                    java.time.LocalDate.now().plusDays(1) // Get statements up to today
                );

            if (statementOpt.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("balance", 0.00);
                response.put("message", "No statements found for this person");
                return ResponseEntity.ok(response);
            }

            com.taxi.domain.statement.model.Statement latestStatement = statementOpt.get();
            java.math.BigDecimal netDue = latestStatement.getNetDue() != null
                ? latestStatement.getNetDue()
                : java.math.BigDecimal.ZERO;

            Map<String, Object> response = new HashMap<>();
            response.put("balance", netDue);
            response.put("statementId", latestStatement.getId());
            response.put("periodFrom", latestStatement.getPeriodFrom());
            response.put("periodTo", latestStatement.getPeriodTo());
            response.put("personName", latestStatement.getPersonName());

            log.info("Current balance for person {}: {}", personId, netDue);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error calculating balance for person {}", personId, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Calculation Error");
            error.put("message", "Failed to calculate balance: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Create a new balance transfer
     */
    @PostMapping
    public ResponseEntity<?> createTransfer(
            @Valid @RequestBody CreateStatementBalanceTransferRequest request
    ) {
        log.info("Creating balance transfer from person {} to person {}",
                request.getSourcePersonId(), request.getTargetPersonId());

        try {
            StatementBalanceTransfer transfer = transferService.createTransfer(
                    request.getSourcePersonId(),
                    request.getTargetPersonId(),
                    TransferType.valueOf(request.getTransferType()),
                    BalanceDirection.valueOf(request.getBalanceDirection()),
                    request.getTransferAmount(),
                    request.getStartDate(),
                    request.getEndDate(),
                    request.getStatementPeriodFrom(),
                    request.getStatementPeriodTo(),
                    request.getDescription(),
                    request.getNotes(),
                    request.getReason(),
                    getCurrentUserId() // TODO: Implement getCurrentUserId() from security context
            );

            StatementBalanceTransferDTO dto = mapper.toDTO(transfer);
            log.info("Created transfer {} successfully", dto.getTransferNumber());
            return ResponseEntity.ok(dto);

        } catch (IllegalArgumentException e) {
            log.error("Validation error creating transfer: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Validation Error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (IllegalStateException e) {
            log.error("State error creating transfer: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid Operation");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity error creating transfer: {}", e.getMessage());
            String message = "Data validation error";
            if (e.getMessage() != null) {
                if (e.getMessage().contains("Out of range")) {
                    message = "Transfer amount exceeds maximum allowed value";
                } else if (e.getMessage().contains("Duplicate entry")) {
                    message = "A transfer with these details already exists";
                } else {
                    message = "Data validation error: Please check your input values";
                }
            }
            Map<String, String> error = new HashMap<>();
            error.put("error", "Data Validation Error");
            error.put("message", message);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            log.error("Error creating transfer", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Server Error");
            error.put("message", "An unexpected error occurred. Please try again.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get all transfers for a person (both source and target)
     */
    @GetMapping("/person/{personId}")
    public ResponseEntity<List<StatementBalanceTransferDTO>> getTransfersForPerson(
            @PathVariable Long personId,
            @RequestParam(required = false) String status
    ) {
        log.info("Fetching transfers for person {} with status {}", personId, status);

        try {
            List<StatementBalanceTransfer> transfers;

            if (status != null && !status.isEmpty()) {
                TransferStatus transferStatus = TransferStatus.valueOf(status);
                transfers = transferService.getTransfersForPersonByStatus(personId, transferStatus);
            } else {
                transfers = transferService.getTransfersForPerson(personId);
            }

            List<StatementBalanceTransferDTO> dtos = transfers.stream()
                    .map(mapper::toDTO)
                    .collect(Collectors.toList());

            log.info("Found {} transfers for person {}", dtos.size(), personId);
            return ResponseEntity.ok(dtos);

        } catch (Exception e) {
            log.error("Error fetching transfers for person {}", personId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get a single transfer by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<StatementBalanceTransferDTO> getTransfer(@PathVariable Long id) {
        log.info("Fetching transfer with ID {}", id);

        try {
            StatementBalanceTransfer transfer = transferService.getTransfer(id);
            StatementBalanceTransferDTO dto = mapper.toDTO(transfer);
            return ResponseEntity.ok(dto);

        } catch (IllegalArgumentException e) {
            log.error("Transfer not found: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error fetching transfer {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get all transfers
     */
    @GetMapping
    public ResponseEntity<List<StatementBalanceTransferDTO>> getAllTransfers() {
        log.info("Fetching all transfers");

        try {
            List<StatementBalanceTransfer> transfers = transferService.getAllTransfers();
            List<StatementBalanceTransferDTO> dtos = transfers.stream()
                    .map(mapper::toDTO)
                    .collect(Collectors.toList());

            log.info("Found {} total transfers", dtos.size());
            return ResponseEntity.ok(dtos);

        } catch (Exception e) {
            log.error("Error fetching all transfers", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Cancel a transfer
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<StatementBalanceTransferDTO> cancelTransfer(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        log.info("Cancelling transfer {}", id);

        try {
            String reason = body.get("reason");
            if (reason == null || reason.trim().isEmpty()) {
                log.error("Cancellation reason is required");
                return ResponseEntity.badRequest().build();
            }

            transferService.cancelTransfer(id, getCurrentUserId(), reason);
            StatementBalanceTransfer transfer = transferService.getTransfer(id);
            StatementBalanceTransferDTO dto = mapper.toDTO(transfer);

            log.info("Cancelled transfer {} successfully", id);
            return ResponseEntity.ok(dto);

        } catch (IllegalArgumentException e) {
            log.error("Transfer not found: {}", id);
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            log.error("Cannot cancel transfer {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error cancelling transfer {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Suspend a transfer
     */
    @PostMapping("/{id}/suspend")
    public ResponseEntity<StatementBalanceTransferDTO> suspendTransfer(@PathVariable Long id) {
        log.info("Suspending transfer {}", id);

        try {
            transferService.suspendTransfer(id);
            StatementBalanceTransfer transfer = transferService.getTransfer(id);
            StatementBalanceTransferDTO dto = mapper.toDTO(transfer);

            log.info("Suspended transfer {} successfully", id);
            return ResponseEntity.ok(dto);

        } catch (IllegalArgumentException e) {
            log.error("Transfer not found: {}", id);
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            log.error("Cannot suspend transfer {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error suspending transfer {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Resume a suspended transfer
     */
    @PostMapping("/{id}/resume")
    public ResponseEntity<StatementBalanceTransferDTO> resumeTransfer(@PathVariable Long id) {
        log.info("Resuming transfer {}", id);

        try {
            transferService.resumeTransfer(id);
            StatementBalanceTransfer transfer = transferService.getTransfer(id);
            StatementBalanceTransferDTO dto = mapper.toDTO(transfer);

            log.info("Resumed transfer {} successfully", id);
            return ResponseEntity.ok(dto);

        } catch (IllegalArgumentException e) {
            log.error("Transfer not found: {}", id);
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            log.error("Cannot resume transfer {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error resuming transfer {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get transfer history for a specific transfer
     */
    @GetMapping("/{id}/history")
    public ResponseEntity<List<StatementBalanceTransferHistoryDTO>> getTransferHistory(
            @PathVariable Long id
    ) {
        log.info("Fetching history for transfer {}", id);

        try {
            List<StatementBalanceTransferHistory> history = transferService.getTransferHistory(id);
            List<StatementBalanceTransferHistoryDTO> dtos = history.stream()
                    .map(mapper::toHistoryDTO)
                    .collect(Collectors.toList());

            log.info("Found {} history records for transfer {}", dtos.size(), id);
            return ResponseEntity.ok(dtos);

        } catch (Exception e) {
            log.error("Error fetching history for transfer {}", id, e);
            return ResponseEntity.internalServerError().build();
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
