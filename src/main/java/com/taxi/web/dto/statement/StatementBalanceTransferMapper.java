package com.taxi.web.dto.statement;

import com.taxi.domain.statement.model.StatementBalanceTransfer;
import com.taxi.domain.statement.model.StatementBalanceTransferHistory;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting statement balance transfer entities to DTOs
 */
@Component
public class StatementBalanceTransferMapper {

    /**
     * Convert StatementBalanceTransfer entity to DTO
     */
    public StatementBalanceTransferDTO toDTO(StatementBalanceTransfer entity) {
        if (entity == null) {
            return null;
        }

        return StatementBalanceTransferDTO.builder()
                .id(entity.getId())
                .transferNumber(entity.getTransferNumber())
                .sourcePersonId(entity.getSourcePerson() != null ? entity.getSourcePerson().getId() : null)
                .sourcePersonType(entity.getSourcePersonType())
                .sourcePersonName(entity.getSourcePersonName())
                .targetPersonId(entity.getTargetPerson() != null ? entity.getTargetPerson().getId() : null)
                .targetPersonType(entity.getTargetPersonType())
                .targetPersonName(entity.getTargetPersonName())
                .transferType(entity.getTransferType() != null ? entity.getTransferType().name() : null)
                .balanceDirection(entity.getBalanceDirection() != null ? entity.getBalanceDirection().name() : null)
                .transferAmount(entity.getTransferAmount())
                .transferredAmount(entity.getTransferredAmount())
                .remainingAmount(entity.getRemainingAmount())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .statementPeriodFrom(entity.getStatementPeriodFrom())
                .statementPeriodTo(entity.getStatementPeriodTo())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .description(entity.getDescription())
                .notes(entity.getNotes())
                .reason(entity.getReason())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .cancelledAt(entity.getCancelledAt())
                .cancelledBy(entity.getCancelledBy())
                .cancellationReason(entity.getCancellationReason())
                .build();
    }

    /**
     * Convert StatementBalanceTransferHistory entity to DTO
     */
    public StatementBalanceTransferHistoryDTO toHistoryDTO(StatementBalanceTransferHistory entity) {
        if (entity == null) {
            return null;
        }

        return StatementBalanceTransferHistoryDTO.builder()
                .id(entity.getId())
                .transferId(entity.getTransfer() != null ? entity.getTransfer().getId() : null)
                .transferNumber(entity.getTransfer() != null ? entity.getTransfer().getTransferNumber() : null)
                .sourceStatementId(entity.getSourceStatement() != null ? entity.getSourceStatement().getId() : null)
                .targetStatementId(entity.getTargetStatement() != null ? entity.getTargetStatement().getId() : null)
                .transferAmount(entity.getTransferAmount())
                .appliedPeriodFrom(entity.getAppliedPeriodFrom())
                .appliedPeriodTo(entity.getAppliedPeriodTo())
                .appliedAt(entity.getAppliedAt())
                .appliedBy(entity.getAppliedBy())
                .description(entity.getDescription())
                .isReversed(entity.getIsReversed())
                .reversedAt(entity.getReversedAt())
                .reversedBy(entity.getReversedBy())
                .reversalReason(entity.getReversalReason())
                .build();
    }
}
