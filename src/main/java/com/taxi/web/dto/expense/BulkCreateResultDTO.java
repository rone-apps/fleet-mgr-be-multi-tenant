package com.taxi.web.dto.expense;

import com.taxi.domain.expense.service.ExpenseAutoApplyService.BulkCreateResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for bulk create operation result
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkCreateResultDTO {

    private int successCount;

    private List<String> errors;

    /**
     * Convert from service result to DTO
     */
    public static BulkCreateResultDTO fromService(BulkCreateResult result) {
        return BulkCreateResultDTO.builder()
            .successCount(result.getSuccessCount())
            .errors(result.errors)
            .build();
    }
}
