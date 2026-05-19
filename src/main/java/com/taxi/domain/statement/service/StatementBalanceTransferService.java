package com.taxi.domain.statement.service;

import com.taxi.domain.driver.model.Driver;
import com.taxi.domain.driver.repository.DriverRepository;
import com.taxi.web.dto.report.OwnerReportDTO;
import com.taxi.domain.statement.model.*;
import com.taxi.domain.statement.repository.StatementBalanceTransferHistoryRepository;
import com.taxi.domain.statement.repository.StatementBalanceTransferRepository;
import com.taxi.domain.statement.repository.StatementRepository;
import com.taxi.web.dto.expense.StatementLineItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing statement balance transfers between drivers/owners
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StatementBalanceTransferService {

    private final StatementBalanceTransferRepository transferRepository;
    private final StatementBalanceTransferHistoryRepository historyRepository;
    private final DriverRepository driverRepository;
    private final StatementRepository statementRepository;

    /**
     * Create a new balance transfer
     */
    @Transactional
    public StatementBalanceTransfer createTransfer(
            Long sourcePersonId,
            Long targetPersonId,
            TransferType transferType,
            BalanceDirection balanceDirection,
            BigDecimal transferAmount,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate statementPeriodFrom,
            LocalDate statementPeriodTo,
            String description,
            String notes,
            String reason,
            Long createdBy
    ) {
        log.info("Creating balance transfer from person {} to person {}, type: {}, direction: {}, amount: {}",
                sourcePersonId, targetPersonId, transferType, balanceDirection, transferAmount);

        // Validation 1: No self-transfer
        if (sourcePersonId.equals(targetPersonId)) {
            throw new IllegalArgumentException("Cannot transfer to the same person");
        }

        // Validation 2: Transfer amount must be positive
        if (transferAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }

        // Validation 3: ONE_TIME requires statement period dates
        if (transferType == TransferType.ONE_TIME) {
            if (statementPeriodFrom == null || statementPeriodTo == null) {
                throw new IllegalArgumentException("ONE_TIME transfer requires statement period dates");
            }
        }

        // Validation 4: Date logic
        if (endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }

        // Validation 5: Check for circular transfers (A→B and B→A)
        LocalDate checkPeriodFrom = transferType == TransferType.ONE_TIME ? statementPeriodFrom : startDate;
        LocalDate checkPeriodTo = transferType == TransferType.ONE_TIME ? statementPeriodTo : (endDate != null ? endDate : LocalDate.now().plusYears(10));

        List<StatementBalanceTransfer> reverseTransfers = transferRepository.findCircularTransfers(
                sourcePersonId, targetPersonId, checkPeriodFrom, checkPeriodTo);

        if (!reverseTransfers.isEmpty()) {
            throw new IllegalStateException("Circular transfer detected - a transfer already exists from target to source for this period");
        }

        // Get source and target persons
        Driver sourcePerson = driverRepository.findById(sourcePersonId)
                .orElseThrow(() -> new IllegalArgumentException("Source person not found: " + sourcePersonId));
        Driver targetPerson = driverRepository.findById(targetPersonId)
                .orElseThrow(() -> new IllegalArgumentException("Target person not found: " + targetPersonId));

        // Determine person types (DRIVER or OWNER)
        String sourcePersonType = sourcePerson.getIsOwner() ? "OWNER" : "DRIVER";
        String targetPersonType = targetPerson.getIsOwner() ? "OWNER" : "DRIVER";

        // Create transfer
        StatementBalanceTransfer transfer = StatementBalanceTransfer.builder()
                .sourcePerson(sourcePerson)
                .sourcePersonType(sourcePersonType)
                .sourcePersonName(sourcePerson.getFullName())
                .targetPerson(targetPerson)
                .targetPersonType(targetPersonType)
                .targetPersonName(targetPerson.getFullName())
                .transferType(transferType)
                .balanceDirection(balanceDirection)
                .transferAmount(transferAmount)
                .startDate(startDate)
                .endDate(endDate)
                .statementPeriodFrom(statementPeriodFrom)
                .statementPeriodTo(statementPeriodTo)
                .description(description)
                .notes(notes)
                .reason(reason)
                .createdBy(createdBy)
                .status(TransferStatus.ACTIVE)
                .build();

        transfer = transferRepository.save(transfer);
        log.info("Created transfer {} from {} to {}", transfer.getTransferNumber(),
                sourcePerson.getFullName(), targetPerson.getFullName());

        return transfer;
    }

    /**
     * Apply transfers to a report during statement generation
     * Called from FinancialStatementService before calculateTotals()
     */
    @Transactional
    public void applyTransfersToStatement(
            OwnerReportDTO report,
            Long personId,
            LocalDate periodFrom,
            LocalDate periodTo
    ) {
        log.debug("Checking for balance transfers for person {} in period {} to {}",
                personId, periodFrom, periodTo);

        // Find outgoing transfers (source) - appear as expenses
        List<StatementBalanceTransfer> outgoingTransfers =
                transferRepository.findApplicableTransfersForSourcePerson(personId, periodFrom, periodTo);

        for (StatementBalanceTransfer transfer : outgoingTransfers) {
            // Check if already applied for this period
            List<StatementBalanceTransferHistory> existingHistory =
                    historyRepository.findActiveSourceHistoryForPeriod(transfer.getId(), periodFrom, periodTo);

            if (!existingHistory.isEmpty()) {
                log.debug("Transfer {} already applied as source for period {} to {}, skipping",
                        transfer.getTransferNumber(), periodFrom, periodTo);
                continue;
            }

            // Add as expense line item
            report.getOneTimeExpenses().add(StatementLineItem.builder()
                    .categoryCode("BALANCE_TRANSFER_OUT")
                    .categoryName("Balance Transfer")
                    .applicationType("BALANCE_TRANSFER")
                    .date(LocalDate.now())
                    .description("Transfer to " + transfer.getTargetPerson().getFullName())
                    .amount(transfer.getTransferAmount())
                    .isRecurring(false)
                    .build());

            log.info("Applied outgoing transfer {} of ${} to statement for person {} as expense",
                    transfer.getTransferNumber(), transfer.getTransferAmount(), personId);
        }

        // Find incoming transfers (target) - appear as revenues
        List<StatementBalanceTransfer> incomingTransfers =
                transferRepository.findApplicableTransfersForTargetPerson(personId, periodFrom, periodTo);

        for (StatementBalanceTransfer transfer : incomingTransfers) {
            // Check if already applied for this period
            List<StatementBalanceTransferHistory> existingHistory =
                    historyRepository.findActiveTargetHistoryForPeriod(transfer.getId(), periodFrom, periodTo);

            if (!existingHistory.isEmpty()) {
                log.debug("Transfer {} already applied as target for period {} to {}, skipping",
                        transfer.getTransferNumber(), periodFrom, periodTo);
                continue;
            }

            // Add as revenue line item
            report.getRevenues().add(OwnerReportDTO.RevenueLineItem.builder()
                    .categoryName("Balance Transfer")
                    .revenueDate(LocalDate.now())
                    .description("Transfer from " + transfer.getSourcePerson().getFullName())
                    .revenueType("BALANCE_TRANSFER_IN")
                    .revenueSubType("TRANSFER_REVENUE")
                    .amount(transfer.getTransferAmount())
                    .build());

            log.info("Applied incoming transfer {} of ${} to statement for person {} as revenue",
                    transfer.getTransferNumber(), transfer.getTransferAmount(), personId);
        }
    }

    /**
     * Record transfer history after statement is finalized
     * Called from FinancialStatementService after saving the statement
     */
    @Transactional
    public void recordTransferApplications(
            Long statementId,
            Long personId,
            LocalDate periodFrom,
            LocalDate periodTo
    ) {
        log.debug("Recording transfer applications for statement {} person {} period {} to {}",
                statementId, personId, periodFrom, periodTo);

        Statement statement = statementRepository.findById(statementId)
                .orElseThrow(() -> new IllegalArgumentException("Statement not found: " + statementId));

        // Record outgoing transfers
        List<StatementBalanceTransfer> outgoingTransfers =
                transferRepository.findApplicableTransfersForSourcePerson(personId, periodFrom, periodTo);

        for (StatementBalanceTransfer transfer : outgoingTransfers) {
            // Check if already recorded
            List<StatementBalanceTransferHistory> existingHistory =
                    historyRepository.findActiveSourceHistoryForPeriod(transfer.getId(), periodFrom, periodTo);

            if (!existingHistory.isEmpty()) {
                continue; // Already recorded
            }

            // Create history record
            StatementBalanceTransferHistory history = StatementBalanceTransferHistory.builder()
                    .transfer(transfer)
                    .sourceStatement(statement)
                    .transferAmount(transfer.getTransferAmount())
                    .appliedPeriodFrom(periodFrom)
                    .appliedPeriodTo(periodTo)
                    .description("Outgoing transfer to " + transfer.getTargetPerson().getFullName())
                    .build();

            historyRepository.save(history);

            // Update transfer amounts
            transfer.applyTransfer(transfer.getTransferAmount());
            transferRepository.save(transfer);

            log.info("Recorded outgoing transfer {} application to statement {}",
                    transfer.getTransferNumber(), statementId);
        }

        // Record incoming transfers
        List<StatementBalanceTransfer> incomingTransfers =
                transferRepository.findApplicableTransfersForTargetPerson(personId, periodFrom, periodTo);

        for (StatementBalanceTransfer transfer : incomingTransfers) {
            // Check if already recorded
            List<StatementBalanceTransferHistory> existingHistory =
                    historyRepository.findActiveTargetHistoryForPeriod(transfer.getId(), periodFrom, periodTo);

            if (!existingHistory.isEmpty()) {
                continue; // Already recorded
            }

            // Create history record
            StatementBalanceTransferHistory history = StatementBalanceTransferHistory.builder()
                    .transfer(transfer)
                    .targetStatement(statement)
                    .transferAmount(transfer.getTransferAmount())
                    .appliedPeriodFrom(periodFrom)
                    .appliedPeriodTo(periodTo)
                    .description("Incoming transfer from " + transfer.getSourcePerson().getFullName())
                    .build();

            historyRepository.save(history);

            log.info("Recorded incoming transfer {} application to statement {}",
                    transfer.getTransferNumber(), statementId);
        }
    }

    /**
     * Cancel a transfer
     */
    @Transactional
    public void cancelTransfer(Long transferId, Long userId, String reason) {
        StatementBalanceTransfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + transferId));

        if (transfer.getStatus() != TransferStatus.ACTIVE && transfer.getStatus() != TransferStatus.SUSPENDED) {
            throw new IllegalStateException("Can only cancel ACTIVE or SUSPENDED transfers");
        }

        transfer.cancel(userId, reason);
        transferRepository.save(transfer);

        log.info("Cancelled transfer {} by user {}, reason: {}", transfer.getTransferNumber(), userId, reason);
    }

    /**
     * Suspend a transfer
     */
    @Transactional
    public void suspendTransfer(Long transferId) {
        StatementBalanceTransfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + transferId));

        if (transfer.getStatus() != TransferStatus.ACTIVE) {
            throw new IllegalStateException("Can only suspend ACTIVE transfers");
        }

        transfer.suspend();
        transferRepository.save(transfer);

        log.info("Suspended transfer {}", transfer.getTransferNumber());
    }

    /**
     * Resume a suspended transfer
     */
    @Transactional
    public void resumeTransfer(Long transferId) {
        StatementBalanceTransfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + transferId));

        if (transfer.getStatus() != TransferStatus.SUSPENDED) {
            throw new IllegalStateException("Can only resume SUSPENDED transfers");
        }

        transfer.resume();
        transferRepository.save(transfer);

        log.info("Resumed transfer {}", transfer.getTransferNumber());
    }

    /**
     * Get transfer history for a specific transfer
     */
    @Transactional(readOnly = true)
    public List<StatementBalanceTransferHistory> getTransferHistory(Long transferId) {
        return historyRepository.findByTransferIdOrderByAppliedAtDesc(transferId);
    }

    /**
     * Get all transfers for a person (both source and target)
     */
    @Transactional(readOnly = true)
    public List<StatementBalanceTransfer> getTransfersForPerson(Long personId) {
        return transferRepository.findByPersonId(personId);
    }

    /**
     * Get transfers for a person with specific status
     */
    @Transactional(readOnly = true)
    public List<StatementBalanceTransfer> getTransfersForPersonByStatus(Long personId, TransferStatus status) {
        return transferRepository.findByPersonIdAndStatus(personId, status);
    }

    /**
     * Get a single transfer by ID
     */
    @Transactional(readOnly = true)
    public StatementBalanceTransfer getTransfer(Long transferId) {
        return transferRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + transferId));
    }

    /**
     * Get all transfers
     */
    @Transactional(readOnly = true)
    public List<StatementBalanceTransfer> getAllTransfers() {
        return transferRepository.findAllByOrderByCreatedAtDesc();
    }
}
