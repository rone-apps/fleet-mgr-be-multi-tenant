package com.taxi.web.dto.expense;

import com.taxi.domain.expense.model.ExpenseCategoryRule;
import com.taxi.domain.expense.model.MatchingCriteria;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for expense category rule
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseCategoryRuleDTO {

    private Long id;

    private Long expenseCategoryId;

    private String configurationMode;

    private MatchingCriteria matchingCriteria;

    private boolean isActive;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * Convert entity to DTO
     */
    public static ExpenseCategoryRuleDTO fromEntity(ExpenseCategoryRule entity) {
        if (entity == null) {
            return null;
        }

        MatchingCriteria criteria = null;
        if (entity.getMatchingCriteria() != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                criteria = mapper.readValue(entity.getMatchingCriteria(), MatchingCriteria.class);
            } catch (Exception e) {
                // Log error but continue
            }
        }

        return ExpenseCategoryRuleDTO.builder()
            .id(entity.getId())
            .expenseCategoryId(entity.getExpenseCategory().getId())
            .configurationMode(entity.getConfigurationMode().name())
            .matchingCriteria(criteria)
            .isActive(entity.isActive())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }
}
