package com.taxi.web.dto.statement;

import com.taxi.domain.statement.model.ExecutionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for TransferExecution entity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferExecutionDTO {

    private Long id;
    private String executionNumber;

    // Link to configuration
    private Long transferConfigId;
    private String configTransferNumber;

    // Period
    private LocalDate periodFrom;
    private LocalDate periodTo;

    // Calculated details
    private BigDecimal calculatedAmount;
    private BigDecimal sourceBalanceSnapshot;
    private Long sourceStatementIdSnapshot;
    private LocalDateTime calculationDate;
    private Long calculatedBy;
    private String calculationNotes;

    // Person details
    private Long sourcePersonId;
    private String sourcePersonName;
    private Long targetPersonId;
    private String targetPersonName;

    // Status
    private ExecutionStatus status;

    // Approval tracking
    private LocalDateTime approvedDate;
    private Long approvedBy;
    private String approvalNotes;

    // Rejection tracking
    private LocalDateTime rejectedDate;
    private Long rejectedBy;
    private String rejectionReason;

    // Application tracking
    private LocalDateTime appliedDate;
    private Long appliedBy;
    private Long sourceStatementId;
    private Long targetStatementId;

    // Finalization tracking
    private LocalDateTime finalizedDate;

    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
