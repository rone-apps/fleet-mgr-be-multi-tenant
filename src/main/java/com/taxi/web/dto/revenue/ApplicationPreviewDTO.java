package com.taxi.web.dto.revenue;

import com.taxi.domain.expense.model.ApplicationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for previewing which entities a category applies to
 * Provides summary information about application scope
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationPreviewDTO {

    private ApplicationType applicationType;

    private String description;
}
