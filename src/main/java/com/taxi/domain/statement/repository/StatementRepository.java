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

    List<Statement> findByPersonIdOrderByPeriodToDesc(Long personId);

    Optional<Statement> findByPersonIdAndPeriodFromAndPeriodTo(Long personId, LocalDate from, LocalDate to);

    List<Statement> findByPersonIdAndStatus(Long personId, StatementStatus status);

    @Query("SELECT s FROM Statement s WHERE s.periodFrom >= :from AND s.periodTo <= :to ORDER BY s.generatedDate DESC")
    List<Statement> findByPeriod(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT s FROM Statement s WHERE s.periodFrom >= :from AND s.periodTo <= :to AND s.status = :status ORDER BY s.generatedDate DESC")
    List<Statement> findByPeriodAndStatus(@Param("from") LocalDate from, @Param("to") LocalDate to, @Param("status") StatementStatus status);
}
