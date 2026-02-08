package com.taxi.web.dto.expense;

import com.taxi.domain.expense.model.ExpenseCategoryRule;
import com.taxi.domain.expense.model.MatchingCriteria;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating or updating expense category rules
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateExpenseCategoryRuleRequest {

    private ExpenseCategoryRule.ConfigurationMode configurationMode;

    private MatchingCriteria matchingCriteria;
}
