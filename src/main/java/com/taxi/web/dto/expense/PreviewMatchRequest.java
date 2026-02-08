package com.taxi.web.dto.expense;

import com.taxi.domain.expense.model.MatchingCriteria;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for previewing matching cabs for unsaved criteria
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreviewMatchRequest {

    private MatchingCriteria matchingCriteria;
}
