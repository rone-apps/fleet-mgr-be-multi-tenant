package com.taxi.domain.statement.service;

import com.taxi.domain.driver.repository.DriverRepository;
import com.taxi.domain.statement.model.*;
import com.taxi.domain.statement.repository.StatementBalanceTransferRepository;
import com.taxi.domain.statement.repository.StatementRepository;
import com.taxi.domain.statement.repository.TransferExecutionRepository;
import com.taxi.web.dto.expense.StatementLineItem;
import com.taxi.web.dto.report.OwnerReportDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing transfer executions - calculated transfers for specific periods
 * that flow through approval workflow before being applied to statements
 */
@Service
@Slf4j
public class TransferExecutionService {

    private final TransferExecutionRepository executionRepository;
    private final StatementBalanceTransferRepository transferConfigRepository;
    private final StatementRepository statementRepository;
    private final DriverRepository driverRepository;
    private final com.taxi.domain.report.service.FinancialStatementService financialStatementService;

    public TransferExecutionService(
            TransferExecutionRepository executionRepository,
            StatementBalanceTransferRepository transferConfigRepository,
            StatementRepository statementRepository,
            DriverRepository driverRepository,
            @Lazy com.taxi.domain.report.service.FinancialStatementService financialStatementService
    ) {
        this.executionRepository = executionRepository;
        this.transferConfigRepository = transferConfigRepository;
        this.statementRepository = statementRepository;
        this.driverRepository = driverRepository;
        this.financialStatementService = financialStatementService;
    }

    /**
     * Generate transfer executions for a specific period
     *
     * 1. Find all ACTIVE transfer configurations
     * 2. For each config, check if applicable to this period
     * 3. Calculate amount based on latest statement balance
     * 4. Create PENDING execution records
     * 5. Return list for admin review
     *
     * @param periodFrom Start of period
     * @param periodTo End of period
     * @param userId User requesting generation
     * @return List of created executions
     */
    @Transactional
    public List<TransferExecution> generateExecutionsForPeriod(
            LocalDate periodFrom,
            LocalDate periodTo,
            Long userId
    ) {
        log.info("Generating transfer executions for period {} to {} by user {}",
                periodFrom, periodTo, userId);

        // Validation: period dates
        if (periodFrom == null || periodTo == null) {
            throw new IllegalArgumentException("Period dates are required");
        }
        if (periodFrom.isAfter(periodTo)) {
            throw new IllegalArgumentException("Period from must be before or equal to period to");
        }

        // Validation: Check if any statements in this period are already FINALIZED
        List<Statement> finalizedStatements = statementRepository.findByPeriodFromAndPeriodToAndStatus(
                periodFrom, periodTo, StatementStatus.FINALIZED
        );
        if (!finalizedStatements.isEmpty()) {
            throw new IllegalStateException(
                    String.format("Cannot generate transfers - %d statements are already FINALIZED for period %s to %s",
                            finalizedStatements.size(), periodFrom, periodTo)
            );
        }

        // Get all ACTIVE transfer configurations
        List<StatementBalanceTransfer> configs = transferConfigRepository.findByStatus(TransferStatus.ACTIVE);
        log.info("Found {} ACTIVE transfer configurations", configs.size());

        List<TransferExecution> executions = new ArrayList<>();

        for (StatementBalanceTransfer config : configs) {
            try {
                log.info("Processing config: {} (Source: {} → Target: {})",
                        config.getTransferNumber(),
                        config.getSourcePersonName(),
                        config.getTargetPersonName());

                // Validate config has required person references
                if (config.getSourcePerson() == null || config.getTargetPerson() == null) {
                    log.warn("Config {} has null person references, skipping", config.getTransferNumber());
                    continue;
                }

                // Check if config is applicable to this period
                boolean isApplicable = config.isApplicableForPeriod(periodFrom, periodTo);
                log.info("Config {} applicable for period {} to {}: {}",
                        config.getTransferNumber(), periodFrom, periodTo, isApplicable);

                if (!isApplicable) {
                    log.info("Skipping config {} - not applicable (Type: {}, Start: {}, End: {}, PeriodFrom: {}, PeriodTo: {})",
                            config.getTransferNumber(),
                            config.getTransferType(),
                            config.getStartDate(),
                            config.getEndDate(),
                            config.getStatementPeriodFrom(),
                            config.getStatementPeriodTo());
                    continue;
                }

                // Check if execution already exists for this config and period
                if (executionRepository.existsByTransferConfigIdAndPeriodFromAndPeriodTo(
                        config.getId(), periodFrom, periodTo)) {
                    log.info("Execution already exists for config {} and period {} to {}",
                            config.getTransferNumber(), periodFrom, periodTo);
                    continue;
                }

                // Calculate transfer amount for this period
                // This generates the statement report for the period and returns the amount to transfer
                CalculationResult result = calculateTransferAmountWithSnapshot(config, periodFrom, periodTo);

                log.info("Calculated amount for config {}: {} (Source person: {}, netDue: {})",
                        config.getTransferNumber(), result.transferAmount,
                        config.getSourcePerson().getId(), result.netDueSnapshot);

                if (result.transferAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    log.info("Skipping config {} - calculated amount is zero or negative: {}",
                            config.getTransferNumber(), result.transferAmount);
                    continue;
                }

                BigDecimal amount = result.transferAmount;
                BigDecimal balanceSnapshot = result.netDueSnapshot;
                Statement latestSourceStatement = null; // We're using calculated report, not latest statement

                // Get person names
                String sourceName = config.getSourcePersonName() != null
                        ? config.getSourcePersonName()
                        : config.getSourcePerson().getFirstName() + " " + config.getSourcePerson().getLastName();
                String targetName = config.getTargetPersonName() != null
                        ? config.getTargetPersonName()
                        : config.getTargetPerson().getFirstName() + " " + config.getTargetPerson().getLastName();

                // Create execution
                TransferExecution execution = TransferExecution.builder()
                        .executionNumber(generateExecutionNumber())
                        .transferConfig(config)
                        .configTransferNumber(config.getTransferNumber())
                        .periodFrom(periodFrom)
                        .periodTo(periodTo)
                        .calculatedAmount(amount)
                        .sourceBalanceSnapshot(balanceSnapshot)
                        .sourceStatementSnapshot(latestSourceStatement)
                        .calculationDate(LocalDateTime.now())
                        .calculatedBy(userId)
                        .calculationNotes(String.format("Generated from config %s for period %s to %s",
                                config.getTransferNumber(), periodFrom, periodTo))
                        .sourcePersonId(config.getSourcePerson().getId())
                        .sourcePersonName(sourceName)
                        .targetPersonId(config.getTargetPerson().getId())
                        .targetPersonName(targetName)
                        .status(ExecutionStatus.PENDING)
                        .build();

                executions.add(executionRepository.save(execution));
                log.info("Created execution {} for config {} with amount {}",
                        execution.getExecutionNumber(), config.getTransferNumber(), amount);

            } catch (Exception e) {
                log.error("Error processing config {}: {}", config.getTransferNumber(), e.getMessage(), e);
                // Continue with next config
            }
        }

        log.info("Generated {} transfer executions for period {} to {}",
                executions.size(), periodFrom, periodTo);
        return executions;
    }

    /**
     * Helper class to return calculation results
     */
    private static class CalculationResult {
        BigDecimal transferAmount;
        BigDecimal netDueSnapshot;

        CalculationResult(BigDecimal transferAmount, BigDecimal netDueSnapshot) {
            this.transferAmount = transferAmount;
            this.netDueSnapshot = netDueSnapshot;
        }
    }

    /**
     * Calculate transfer amount for a config and period with snapshot
     *
     * NEW Logic (generates statement for the period):
     * 1. Generate/calculate the statement report for source person FOR THIS PERIOD
     * 2. Get the netDue (balance) from that calculated report
     * 3. If POSITIVE_ONLY and balance <= 0, skip
     * 4. If "Transfer All" (amount < $1), use full balance
     * 5. If "Up to Max", use min(balance, maxAmount)
     */
    private CalculationResult calculateTransferAmountWithSnapshot(
            StatementBalanceTransfer config,
            LocalDate periodFrom,
            LocalDate periodTo
    ) {
        Long sourcePersonId = config.getSourcePerson().getId();

        try {
            // Generate the statement report for this person for this period
            // This calculates what they would owe AFTER this period's statement
            // IMPORTANT: Pass false to exclude APPROVED executions (avoid circular calculation)
            log.info("Calculating statement for person {} for period {} to {}",
                    sourcePersonId, periodFrom, periodTo);

            OwnerReportDTO report = financialStatementService.generateOwnerReport(
                    sourcePersonId, periodFrom, periodTo, false
            );

            BigDecimal netDue = report.getNetDue() != null
                    ? report.getNetDue()
                    : BigDecimal.ZERO;

            log.info("Calculated statement for person {}: netDue={}, totalRevenues={}, totalExpenses={}",
                    sourcePersonId, netDue, report.getTotalRevenues(), report.getTotalExpenses());

            // Apply balance direction rules
            if (config.getBalanceDirection() == BalanceDirection.POSITIVE_ONLY) {
                if (netDue.compareTo(BigDecimal.ZERO) <= 0) {
                    log.info("NetDue {} is not positive, skipping (POSITIVE_ONLY rule)", netDue);
                    return new CalculationResult(BigDecimal.ZERO, netDue);
                }
            }

            // For transfer purposes, we want the absolute value
            // Positive netDue = driver owes company money (company's receivable)
            // We transfer this to another driver (reducing source's debt, increasing target's debt)
            BigDecimal absoluteBalance = netDue.abs();

            // Check if there's a maximum amount specified
            // If transferAmount is null or very small (< $1), treat as "transfer all"
            BigDecimal configAmount = config.getTransferAmount();
            if (configAmount == null || configAmount.compareTo(new BigDecimal("1.00")) < 0) {
                // Transfer All mode - use full balance
                log.info("Transfer All mode - using full calculated netDue: {}", absoluteBalance);
                return new CalculationResult(absoluteBalance, netDue);
            }

            // Up to Max Amount mode - use the lesser of balance or max amount
            BigDecimal transferAmount = absoluteBalance.min(configAmount);
            log.info("Max Amount mode - calculated netDue: {}, max: {}, transferring: {}",
                    absoluteBalance, configAmount, transferAmount);
            return new CalculationResult(transferAmount, netDue);

        } catch (Exception e) {
            log.error("Error calculating statement for person {} period {} to {}: {}",
                    sourcePersonId, periodFrom, periodTo, e.getMessage(), e);
            return new CalculationResult(BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }

    /**
     * Find latest finalized or paid statement for a person before a date
     */
    private Statement findLatestStatementForPerson(Long personId, LocalDate beforeDate) {
        return statementRepository.findLatestByPersonIdAndStatusInAndPeriodToBefore(
                personId,
                List.of(StatementStatus.FINALIZED, StatementStatus.PAID),
                beforeDate.plusDays(1) // Include statements ending on beforeDate
        ).orElse(null);
    }

    /**
     * Generate unique execution number
     * Format: EXEC-YYYY-MM-DD-shortUuid
     */
    private String generateExecutionNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String shortUuid = UUID.randomUUID().toString().substring(0, 8);
        return String.format("EXEC-%s-%s", dateStr, shortUuid);
    }

    /**
     * Get execution by ID
     */
    @Transactional(readOnly = true)
    public TransferExecution getExecution(Long id) {
        return executionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found: " + id));
    }

    /**
     * Get all executions
     */
    @Transactional(readOnly = true)
    public List<TransferExecution> getAllExecutions() {
        return executionRepository.findAll();
    }

    /**
     * Get executions by status
     */
    @Transactional(readOnly = true)
    public List<TransferExecution> getExecutionsByStatus(ExecutionStatus status) {
        return executionRepository.findByStatus(status);
    }

    /**
     * Get executions for a period
     */
    @Transactional(readOnly = true)
    public List<TransferExecution> getExecutionsForPeriod(
            LocalDate periodFrom,
            LocalDate periodTo,
            ExecutionStatus status
    ) {
        if (status != null) {
            return executionRepository.findByPeriodFromAndPeriodToAndStatus(periodFrom, periodTo, status);
        }
        return executionRepository.findByPeriodFromAndPeriodTo(periodFrom, periodTo);
    }

    /**
     * Approve an execution
     */
    @Transactional
    public TransferExecution approve(Long executionId, Long userId, String notes) {
        log.info("Approving execution {} by user {}", executionId, userId);

        TransferExecution execution = getExecution(executionId);
        execution.approve(userId, notes);
        execution = executionRepository.save(execution);

        log.info("Approved execution {} - status now {}", execution.getExecutionNumber(), execution.getStatus());
        return execution;
    }

    /**
     * Reject an execution
     */
    @Transactional
    public TransferExecution reject(Long executionId, Long userId, String reason) {
        log.info("Rejecting execution {} by user {}", executionId, userId);

        TransferExecution execution = getExecution(executionId);
        execution.reject(userId, reason);
        execution = executionRepository.save(execution);

        log.info("Rejected execution {} - status now {}", execution.getExecutionNumber(), execution.getStatus());
        return execution;
    }

    /**
     * Apply transfer executions to a report
     * Called from FinancialStatementService
     *
     * Adds transfer line items to the report (expenses for source, revenue for target)
     *
     * @param includePendingApproved If true, includes APPROVED executions (for statement generation).
     *                               If false, only includes APPLIED/FINALIZED (for balance calculation)
     */
    @Transactional
    public void applyExecutionsToReport(
            OwnerReportDTO report,
            Long personId,
            LocalDate periodFrom,
            LocalDate periodTo,
            boolean includePendingApproved
    ) {
        log.debug("Applying transfer executions to report for person {} period {} to {} (includePending={})",
                personId, periodFrom, periodTo, includePendingApproved);

        List<TransferExecution> outgoing = new ArrayList<>();
        List<TransferExecution> incoming = new ArrayList<>();

        if (includePendingApproved) {
            // Include APPROVED executions (for statement generation)
            outgoing.addAll(executionRepository.findBySourcePersonIdAndPeriodAndStatus(
                    personId, periodFrom, periodTo, ExecutionStatus.APPROVED
            ));
            incoming.addAll(executionRepository.findByTargetPersonIdAndPeriodAndStatus(
                    personId, periodFrom, periodTo, ExecutionStatus.APPROVED
            ));
        }

        // Always include APPLIED and FINALIZED executions
        outgoing.addAll(executionRepository.findBySourcePersonIdAndPeriodAndStatus(
                personId, periodFrom, periodTo, ExecutionStatus.APPLIED
        ));
        outgoing.addAll(executionRepository.findBySourcePersonIdAndPeriodAndStatus(
                personId, periodFrom, periodTo, ExecutionStatus.FINALIZED
        ));
        incoming.addAll(executionRepository.findByTargetPersonIdAndPeriodAndStatus(
                personId, periodFrom, periodTo, ExecutionStatus.APPLIED
        ));
        incoming.addAll(executionRepository.findByTargetPersonIdAndPeriodAndStatus(
                personId, periodFrom, periodTo, ExecutionStatus.FINALIZED
        ));

        // Add outgoing transfers as EXPENSE (reduces what driver receives)
        // Example: Driver A earns $1,800, transfers $500 to B → Driver A gets $1,300
        // netDue = revenues - expenses, so expense reduces netDue
        for (TransferExecution exec : outgoing) {
            report.getOneTimeExpenses().add(StatementLineItem.builder()
                    .categoryCode("BALANCE_TRANSFER_OUT")
                    .categoryName("Balance Transfer")
                    .date(LocalDate.now())
                    .description(String.format("Balance transfer to %s (Exec: %s)",
                            exec.getTargetPersonName(), exec.getExecutionNumber()))
                    .amount(exec.getCalculatedAmount())
                    .build());

            log.info("Added outgoing transfer as EXPENSE: {} to {} - amount {} (reduces source's earnings)",
                    exec.getExecutionNumber(), exec.getTargetPersonName(), exec.getCalculatedAmount());
        }

        // Add incoming transfers as REVENUE (increases what driver receives)
        // Example: Driver B earns $200, receives $500 from A → Driver B gets $700
        // netDue = revenues - expenses, so revenue increases netDue
        for (TransferExecution exec : incoming) {
            report.getRevenues().add(OwnerReportDTO.RevenueLineItem.builder()
                    .categoryName("Balance Transfer")
                    .revenueType("BALANCE_TRANSFER_IN")
                    .revenueSubType("BALANCE_TRANSFER")
                    .description(String.format("Balance transfer from %s (Exec: %s)",
                            exec.getSourcePersonName(), exec.getExecutionNumber()))
                    .amount(exec.getCalculatedAmount())
                    .build());

            log.info("Added incoming transfer as REVENUE: {} from {} - amount {} (increases target's earnings)",
                    exec.getExecutionNumber(), exec.getSourcePersonName(), exec.getCalculatedAmount());
        }

        log.debug("Applied {} outgoing and {} incoming transfer executions to report for person {}",
                outgoing.size(), incoming.size(), personId);
    }

    /**
     * Convenience overload - defaults to NOT including pending APPROVED executions
     * This is the safe default for calculations and balance checks
     */
    @Transactional
    public void applyExecutionsToReport(
            OwnerReportDTO report,
            Long personId,
            LocalDate periodFrom,
            LocalDate periodTo
    ) {
        applyExecutionsToReport(report, personId, periodFrom, periodTo, false);
    }

    /**
     * Mark executions as applied after statement is saved
     * Called from FinancialStatementService after statement creation
     */
    @Transactional
    public void markExecutionsApplied(
            Long statementId,
            Long personId,
            LocalDate periodFrom,
            LocalDate periodTo,
            Long userId
    ) {
        log.info("Marking executions as applied for statement {} person {} period {} to {}",
                statementId, personId, periodFrom, periodTo);

        Statement statement = statementRepository.findById(statementId)
                .orElseThrow(() -> new IllegalArgumentException("Statement not found: " + statementId));

        // Find APPROVED executions where this person is the source
        List<TransferExecution> outgoing = executionRepository.findBySourcePersonIdAndPeriodAndStatus(
                personId, periodFrom, periodTo, ExecutionStatus.APPROVED
        );

        // Find APPROVED executions where this person is the target
        List<TransferExecution> incoming = executionRepository.findByTargetPersonIdAndPeriodAndStatus(
                personId, periodFrom, periodTo, ExecutionStatus.APPROVED
        );

        // Mark outgoing executions as applied (this statement is the source)
        for (TransferExecution exec : outgoing) {
            exec.applyToStatements(statement, exec.getTargetStatement(), userId);
            executionRepository.save(exec);
            log.info("Marked execution {} as APPLIED - source statement {}", exec.getExecutionNumber(), statementId);
        }

        // Mark incoming executions as applied (this statement is the target)
        for (TransferExecution exec : incoming) {
            exec.applyToStatements(exec.getSourceStatement(), statement, userId);
            executionRepository.save(exec);
            log.info("Marked execution {} as APPLIED - target statement {}", exec.getExecutionNumber(), statementId);
        }

        log.info("Marked {} outgoing and {} incoming executions as APPLIED for statement {}",
                outgoing.size(), incoming.size(), statementId);
    }

    /**
     * Finalize executions when statement is finalized
     * Called from FinancialStatementService after statement finalization
     */
    @Transactional
    public void finalizeExecutionsForStatement(Long statementId) {
        log.info("Finalizing transfer executions for statement {}", statementId);

        List<TransferExecution> executions = executionRepository.findBySourceStatementIdOrTargetStatementId(statementId);

        int finalizedCount = 0;
        for (TransferExecution exec : executions) {
            if (exec.getStatus() == ExecutionStatus.APPLIED) {
                exec.finalize();
                executionRepository.save(exec);
                finalizedCount++;
                log.info("Finalized execution {} - now immutable", exec.getExecutionNumber());
            }
        }

        log.info("Finalized {} transfer executions for statement {}", finalizedCount, statementId);
    }

    /**
     * Batch approve executions
     */
    @Transactional
    public List<TransferExecution> batchApprove(List<Long> executionIds, Long userId, String notes) {
        log.info("Batch approving {} executions by user {}", executionIds.size(), userId);

        List<TransferExecution> approved = new ArrayList<>();
        for (Long id : executionIds) {
            try {
                TransferExecution execution = approve(id, userId, notes);
                approved.add(execution);
            } catch (Exception e) {
                log.error("Error approving execution {}: {}", id, e.getMessage());
                // Continue with others
            }
        }

        log.info("Batch approved {} out of {} executions", approved.size(), executionIds.size());
        return approved;
    }

    /**
     * Batch reject executions
     */
    @Transactional
    public List<TransferExecution> batchReject(List<Long> executionIds, Long userId, String reason) {
        log.info("Batch rejecting {} executions by user {}", executionIds.size(), userId);

        List<TransferExecution> rejected = new ArrayList<>();
        for (Long id : executionIds) {
            try {
                TransferExecution execution = reject(id, userId, reason);
                rejected.add(execution);
            } catch (Exception e) {
                log.error("Error rejecting execution {}: {}", id, e.getMessage());
                // Continue with others
            }
        }

        log.info("Batch rejected {} out of {} executions", rejected.size(), executionIds.size());
        return rejected;
    }
}
