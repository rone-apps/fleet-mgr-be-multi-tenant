package com.taxi.domain.statement.repository;

import com.taxi.domain.statement.model.StatementBalanceTransferHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for Statement Balance Transfer History
 */
@Repository
public interface StatementBalanceTransferHistoryRepository extends JpaRepository<StatementBalanceTransferHistory, Long> {

    /**
     * Find all history records for a transfer
     */
    List<StatementBalanceTransferHistory> findByTransferId(Long transferId);

    /**
     * Find all history records for a transfer ordered by application date
     */
    List<StatementBalanceTransferHistory> findByTransferIdOrderByAppliedAtDesc(Long transferId);

    /**
     * Find history records for a source statement
     */
    List<StatementBalanceTransferHistory> findBySourceStatementId(Long statementId);

    /**
     * Find history records for a target statement
     */
    List<StatementBalanceTransferHistory> findByTargetStatementId(Long statementId);

    /**
     * Find non-reversed history records for a transfer
     */
    List<StatementBalanceTransferHistory> findByTransferIdAndIsReversed(Long transferId, Boolean isReversed);

    /**
     * Check if transfer has already been applied for a specific period (source side)
     */
    @Query("""
        SELECT h FROM StatementBalanceTransferHistory h
        WHERE h.transfer.id = :transferId
        AND h.sourceStatement IS NOT NULL
        AND h.appliedPeriodFrom = :periodFrom
        AND h.appliedPeriodTo = :periodTo
        AND h.isReversed = FALSE
        """)
    List<StatementBalanceTransferHistory> findActiveSourceHistoryForPeriod(
            @Param("transferId") Long transferId,
            @Param("periodFrom") LocalDate periodFrom,
            @Param("periodTo") LocalDate periodTo
    );

    /**
     * Check if transfer has already been applied for a specific period (target side)
     */
    @Query("""
        SELECT h FROM StatementBalanceTransferHistory h
        WHERE h.transfer.id = :transferId
        AND h.targetStatement IS NOT NULL
        AND h.appliedPeriodFrom = :periodFrom
        AND h.appliedPeriodTo = :periodTo
        AND h.isReversed = FALSE
        """)
    List<StatementBalanceTransferHistory> findActiveTargetHistoryForPeriod(
            @Param("transferId") Long transferId,
            @Param("periodFrom") LocalDate periodFrom,
            @Param("periodTo") LocalDate periodTo
    );

    /**
     * Find all history records for a person in a period (as source)
     */
    @Query("""
        SELECT h FROM StatementBalanceTransferHistory h
        WHERE h.transfer.sourcePerson.id = :personId
        AND h.appliedPeriodFrom = :periodFrom
        AND h.appliedPeriodTo = :periodTo
        AND h.isReversed = FALSE
        ORDER BY h.appliedAt DESC
        """)
    List<StatementBalanceTransferHistory> findActiveHistoryForSourcePerson(
            @Param("personId") Long personId,
            @Param("periodFrom") LocalDate periodFrom,
            @Param("periodTo") LocalDate periodTo
    );

    /**
     * Find all history records for a person in a period (as target)
     */
    @Query("""
        SELECT h FROM StatementBalanceTransferHistory h
        WHERE h.transfer.targetPerson.id = :personId
        AND h.appliedPeriodFrom = :periodFrom
        AND h.appliedPeriodTo = :periodTo
        AND h.isReversed = FALSE
        ORDER BY h.appliedAt DESC
        """)
    List<StatementBalanceTransferHistory> findActiveHistoryForTargetPerson(
            @Param("personId") Long personId,
            @Param("periodFrom") LocalDate periodFrom,
            @Param("periodTo") LocalDate periodTo
    );

    /**
     * Find all history records for a specific statement period
     */
    @Query("""
        SELECT h FROM StatementBalanceTransferHistory h
        WHERE h.appliedPeriodFrom = :periodFrom
        AND h.appliedPeriodTo = :periodTo
        ORDER BY h.appliedAt DESC
        """)
    List<StatementBalanceTransferHistory> findByPeriod(
            @Param("periodFrom") LocalDate periodFrom,
            @Param("periodTo") LocalDate periodTo
    );
}
