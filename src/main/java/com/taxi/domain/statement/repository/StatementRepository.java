package com.taxi.domain.statement.repository;

import com.taxi.domain.statement.model.Statement;
import com.taxi.domain.statement.model.StatementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
