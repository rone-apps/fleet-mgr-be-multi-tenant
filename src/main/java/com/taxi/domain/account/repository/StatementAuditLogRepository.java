package com.taxi.domain.account.repository;

import com.taxi.domain.account.model.StatementAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StatementAuditLogRepository extends JpaRepository<StatementAuditLog, Long> {

    @Query("SELECT a FROM StatementAuditLog a WHERE a.statement.id = :statementId ORDER BY a.changedAt DESC")
    List<StatementAuditLog> findByStatementId(@Param("statementId") Long statementId);

    @Query("SELECT a FROM StatementAuditLog a WHERE a.changeType = :changeType ORDER BY a.changedAt DESC")
    List<StatementAuditLog> findByChangeType(@Param("changeType") String changeType);
}
