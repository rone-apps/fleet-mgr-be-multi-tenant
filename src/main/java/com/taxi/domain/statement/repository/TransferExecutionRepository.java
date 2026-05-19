package com.taxi.domain.statement.repository;

import com.taxi.domain.statement.model.ExecutionStatus;
import com.taxi.domain.statement.model.TransferExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for TransferExecution entity
 */
@Repository
public interface TransferExecutionRepository extends JpaRepository<TransferExecution, Long> {

    /**
     * Find executions by period and status
     */
    List<TransferExecution> findByPeriodFromAndPeriodToAndStatus(
        LocalDate periodFrom,
        LocalDate periodTo,
        ExecutionStatus status
    );

    /**
     * Find executions by period (all statuses)
     */
    List<TransferExecution> findByPeriodFromAndPeriodTo(
        LocalDate periodFrom,
        LocalDate periodTo
    );

    /**
     * Find executions by config and period
     */
    List<TransferExecution> findByTransferConfigIdAndPeriodFromAndPeriodTo(
        Long transferConfigId,
        LocalDate periodFrom,
        LocalDate periodTo
    );

    /**
     * Find executions by source person and period with status
     */
    @Query("SELECT e FROM TransferExecution e WHERE e.sourcePersonId = :personId " +
           "AND e.periodFrom = :periodFrom AND e.periodTo = :periodTo " +
           "AND e.status = :status")
    List<TransferExecution> findBySourcePersonIdAndPeriodAndStatus(
        @Param("personId") Long personId,
        @Param("periodFrom") LocalDate periodFrom,
        @Param("periodTo") LocalDate periodTo,
        @Param("status") ExecutionStatus status
    );

    /**
     * Find executions by target person and period with status
     */
    @Query("SELECT e FROM TransferExecution e WHERE e.targetPersonId = :personId " +
           "AND e.periodFrom = :periodFrom AND e.periodTo = :periodTo " +
           "AND e.status = :status")
    List<TransferExecution> findByTargetPersonIdAndPeriodAndStatus(
        @Param("personId") Long personId,
        @Param("periodFrom") LocalDate periodFrom,
        @Param("periodTo") LocalDate periodTo,
        @Param("status") ExecutionStatus status
    );

    /**
     * Find executions by source person and period (all statuses)
     */
    List<TransferExecution> findBySourcePersonIdAndPeriodFromAndPeriodTo(
        Long sourcePersonId,
        LocalDate periodFrom,
        LocalDate periodTo
    );

    /**
     * Find executions by target person and period (all statuses)
     */
    List<TransferExecution> findByTargetPersonIdAndPeriodFromAndPeriodTo(
        Long targetPersonId,
        LocalDate periodFrom,
        LocalDate periodTo
    );

    /**
     * Find executions by status
     */
    List<TransferExecution> findByStatus(ExecutionStatus status);

    /**
     * Check if execution exists for config and period (excluding specific status)
     */
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM TransferExecution e " +
           "WHERE e.transferConfig.id = :configId " +
           "AND e.periodFrom = :periodFrom AND e.periodTo = :periodTo " +
           "AND e.status != :excludeStatus")
    boolean existsByTransferConfigIdAndPeriodAndStatusNot(
        @Param("configId") Long configId,
        @Param("periodFrom") LocalDate periodFrom,
        @Param("periodTo") LocalDate periodTo,
        @Param("excludeStatus") ExecutionStatus excludeStatus
    );

    /**
     * Check if execution exists for config and period
     */
    boolean existsByTransferConfigIdAndPeriodFromAndPeriodTo(
        Long transferConfigId,
        LocalDate periodFrom,
        LocalDate periodTo
    );

    /**
     * Find executions by source or target statement ID
     */
    @Query("SELECT e FROM TransferExecution e WHERE e.sourceStatement.id = :statementId " +
           "OR e.targetStatement.id = :statementId")
    List<TransferExecution> findBySourceStatementIdOrTargetStatementId(
        @Param("statementId") Long statementId
    );

    /**
     * Find execution by execution number
     */
    Optional<TransferExecution> findByExecutionNumber(String executionNumber);

    /**
     * Find all executions for a person (either source or target) in a period
     */
    @Query("SELECT e FROM TransferExecution e WHERE " +
           "(e.sourcePersonId = :personId OR e.targetPersonId = :personId) " +
           "AND e.periodFrom = :periodFrom AND e.periodTo = :periodTo")
    List<TransferExecution> findByPersonAndPeriod(
        @Param("personId") Long personId,
        @Param("periodFrom") LocalDate periodFrom,
        @Param("periodTo") LocalDate periodTo
    );

    /**
     * Find all APPROVED executions for a person (as source) in a period
     */
    @Query("SELECT e FROM TransferExecution e WHERE e.sourcePersonId = :personId " +
           "AND e.periodFrom >= :periodFrom AND e.periodTo <= :periodTo " +
           "AND e.status = 'APPROVED'")
    List<TransferExecution> findApprovedForSourceInPeriod(
        @Param("personId") Long personId,
        @Param("periodFrom") LocalDate periodFrom,
        @Param("periodTo") LocalDate periodTo
    );

    /**
     * Find all APPROVED executions for a person (as target) in a period
     */
    @Query("SELECT e FROM TransferExecution e WHERE e.targetPersonId = :personId " +
           "AND e.periodFrom >= :periodFrom AND e.periodTo <= :periodTo " +
           "AND e.status = 'APPROVED'")
    List<TransferExecution> findApprovedForTargetInPeriod(
        @Param("personId") Long personId,
        @Param("periodFrom") LocalDate periodFrom,
        @Param("periodTo") LocalDate periodTo
    );
}
