package com.taxi.web.dto.statement;

import com.taxi.domain.statement.model.TransferExecution;
import org.springframework.stereotype.Component;

/**
 * Mapper for TransferExecution entity to DTO conversions
 */
@Component
public class TransferExecutionMapper {

    /**
     * Convert TransferExecution entity to DTO
     */
    public TransferExecutionDTO toDTO(TransferExecution execution) {
        if (execution == null) {
            return null;
        }

        return TransferExecutionDTO.builder()
                .id(execution.getId())
                .executionNumber(execution.getExecutionNumber())
                .transferConfigId(execution.getTransferConfig() != null ? execution.getTransferConfig().getId() : null)
                .configTransferNumber(execution.getConfigTransferNumber())
                .periodFrom(execution.getPeriodFrom())
                .periodTo(execution.getPeriodTo())
                .calculatedAmount(execution.getCalculatedAmount())
                .sourceBalanceSnapshot(execution.getSourceBalanceSnapshot())
                .sourceStatementIdSnapshot(execution.getSourceStatementSnapshot() != null
                        ? execution.getSourceStatementSnapshot().getId() : null)
                .calculationDate(execution.getCalculationDate())
                .calculatedBy(execution.getCalculatedBy())
                .calculationNotes(execution.getCalculationNotes())
                .sourcePersonId(execution.getSourcePersonId())
                .sourcePersonName(execution.getSourcePersonName())
                .targetPersonId(execution.getTargetPersonId())
                .targetPersonName(execution.getTargetPersonName())
                .status(execution.getStatus())
                .approvedDate(execution.getApprovedDate())
                .approvedBy(execution.getApprovedBy())
                .approvalNotes(execution.getApprovalNotes())
                .rejectedDate(execution.getRejectedDate())
                .rejectedBy(execution.getRejectedBy())
                .rejectionReason(execution.getRejectionReason())
                .appliedDate(execution.getAppliedDate())
                .appliedBy(execution.getAppliedBy())
                .sourceStatementId(execution.getSourceStatement() != null ? execution.getSourceStatement().getId() : null)
                .targetStatementId(execution.getTargetStatement() != null ? execution.getTargetStatement().getId() : null)
                .finalizedDate(execution.getFinalizedDate())
                .createdAt(execution.getCreatedAt())
                .updatedAt(execution.getUpdatedAt())
                .build();
    }
}
