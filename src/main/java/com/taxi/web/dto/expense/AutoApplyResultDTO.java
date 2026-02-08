package com.taxi.web.dto.expense;

import com.taxi.domain.expense.service.ExpenseAutoApplyService.AutoApplyResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for auto-apply operation result
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutoApplyResultDTO {

    private int successCount;

    private int totalMatched;

    private List<String> errors;

    /**
     * Convert from service result to DTO
     */
    public static AutoApplyResultDTO fromService(AutoApplyResult result) {
        return AutoApplyResultDTO.builder()
            .successCount(result.getSuccessCount())
            .totalMatched(result.totalMatched)
            .errors(result.errors)
            .build();
    }
}
