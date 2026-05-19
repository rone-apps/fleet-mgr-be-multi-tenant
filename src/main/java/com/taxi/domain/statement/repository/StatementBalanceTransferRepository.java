package com.taxi.domain.statement.repository;

import com.taxi.domain.statement.model.StatementBalanceTransfer;
import com.taxi.domain.statement.model.TransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Statement Balance Transfers
 */
@Repository
public interface StatementBalanceTransferRepository extends JpaRepository<StatementBalanceTransfer, Long> {

    /**
     * Find transfer by transfer number
     */
    Optional<StatementBalanceTransfer> findByTransferNumber(String transferNumber);

    /**
     * Find all active transfers where given person is the source (outgoing transfers)
     */
    @Query("""
        SELECT t FROM StatementBalanceTransfer t
        WHERE t.sourcePerson.id = :personId
        AND t.status = 'ACTIVE'
        AND (
            (t.transferType = 'RECURRING' AND t.startDate <= :periodTo
             AND (t.endDate IS NULL OR t.endDate >= :periodFrom))
            OR
            (t.transferType = 'ONE_TIME' AND t.statementPeriodFrom = :periodFrom
             AND t.statementPeriodTo = :periodTo)
        )
        """)
    List<StatementBalanceTransfer> findApplicableTransfersForSourcePerson(
            @Param("personId") Long personId,
            @Param("periodFrom") LocalDate periodFrom,
            @Param("periodTo") LocalDate periodTo
    );

    /**
     * Find all active transfers where given person is the target (incoming transfers)
     */
    @Query("""
        SELECT t FROM StatementBalanceTransfer t
        WHERE t.targetPerson.id = :personId
        AND t.status = 'ACTIVE'
        AND (
            (t.transferType = 'RECURRING' AND t.startDate <= :periodTo
             AND (t.endDate IS NULL OR t.endDate >= :periodFrom))
            OR
            (t.transferType = 'ONE_TIME' AND t.statementPeriodFrom = :periodFrom
             AND t.statementPeriodTo = :periodTo)
        )
        """)
    List<StatementBalanceTransfer> findApplicableTransfersForTargetPerson(
            @Param("personId") Long personId,
            @Param("periodFrom") LocalDate periodFrom,
            @Param("periodTo") LocalDate periodTo
    );

    /**
     * Find all transfers for a person (both source and target) with given status
     */
    @Query("""
        SELECT t FROM StatementBalanceTransfer t
        WHERE (t.sourcePerson.id = :personId OR t.targetPerson.id = :personId)
        AND t.status = :status
        ORDER BY t.createdAt DESC
        """)
    List<StatementBalanceTransfer> findByPersonIdAndStatus(
            @Param("personId") Long personId,
            @Param("status") TransferStatus status
    );

    /**
     * Find all transfers for a person (both source and target)
     */
    @Query("""
        SELECT t FROM StatementBalanceTransfer t
        WHERE (t.sourcePerson.id = :personId OR t.targetPerson.id = :personId)
        ORDER BY t.createdAt DESC
        """)
    List<StatementBalanceTransfer> findByPersonId(@Param("personId") Long personId);

    /**
     * Find outgoing transfers for a person with given status
     */
    List<StatementBalanceTransfer> findBySourcePersonIdAndStatus(Long sourcePersonId, TransferStatus status);

    /**
     * Find incoming transfers for a person with given status
     */
    List<StatementBalanceTransfer> findByTargetPersonIdAndStatus(Long targetPersonId, TransferStatus status);

    /**
     * Find outgoing transfers for a person
     */
    List<StatementBalanceTransfer> findBySourcePersonId(Long sourcePersonId);

    /**
     * Find incoming transfers for a person
     */
    List<StatementBalanceTransfer> findByTargetPersonId(Long targetPersonId);

    /**
     * Check for circular transfers - find active transfers from target to source
     * Used to prevent A→B and B→A from overlapping
     */
    @Query("""
        SELECT t FROM StatementBalanceTransfer t
        WHERE t.sourcePerson.id = :targetPersonId
        AND t.targetPerson.id = :sourcePersonId
        AND t.status = 'ACTIVE'
        AND (
            (t.transferType = 'RECURRING' AND t.startDate <= :periodTo
             AND (t.endDate IS NULL OR t.endDate >= :periodFrom))
            OR
            (t.transferType = 'ONE_TIME' AND t.statementPeriodFrom = :periodFrom
             AND t.statementPeriodTo = :periodTo)
        )
        """)
    List<StatementBalanceTransfer> findCircularTransfers(
            @Param("sourcePersonId") Long sourcePersonId,
            @Param("targetPersonId") Long targetPersonId,
            @Param("periodFrom") LocalDate periodFrom,
            @Param("periodTo") LocalDate periodTo
    );

    /**
     * Find all transfers by status
     */
    List<StatementBalanceTransfer> findByStatus(TransferStatus status);

    /**
     * Find all transfers ordered by creation date
     */
    List<StatementBalanceTransfer> findAllByOrderByCreatedAtDesc();
}
