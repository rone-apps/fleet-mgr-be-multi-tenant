package com.taxi.domain.statement.repository;

import com.taxi.domain.statement.model.Statement;
import com.taxi.domain.statement.model.StatementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StatementRepository extends JpaRepository<Statement, Long> {
    Optional<Statement> findTopByPersonIdOrderByPeriodToDesc(Long personId);

    Optional<Statement> findTopByPersonIdAndStatusOrderByPeriodToDesc(Long personId, StatementStatus status);

    Optional<Statement> findTopByPersonIdAndStatusAndPeriodToBeforeOrderByPeriodToDesc(Long personId, StatementStatus status, LocalDate beforeDate);

    @Query("SELECT s FROM Statement s WHERE s.personId = :personId AND s.status IN :statuses AND s.periodTo < :beforeDate ORDER BY s.periodTo DESC LIMIT 1")
    Optional<Statement> findLatestByPersonIdAndStatusInAndPeriodToBefore(@Param("personId") Long personId, @Param("statuses") List<StatementStatus> statuses, @Param("beforeDate") LocalDate beforeDate);

    List<Statement> findByPersonIdOrderByPeriodToDesc(Long personId);

    Optional<Statement> findByPersonIdAndPeriodFromAndPeriodTo(Long personId, LocalDate from, LocalDate to);

    List<Statement> findByPersonIdAndStatus(Long personId, StatementStatus status);

    @Query("SELECT s FROM Statement s WHERE s.periodFrom <= :to AND s.periodTo >= :from ORDER BY s.generatedDate DESC")
    List<Statement> findByPeriod(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT s FROM Statement s WHERE s.periodFrom <= :to AND s.periodTo >= :from AND s.status = :status ORDER BY s.generatedDate DESC")
    List<Statement> findByPeriodAndStatus(@Param("from") LocalDate from, @Param("to") LocalDate to, @Param("status") StatementStatus status);
}
