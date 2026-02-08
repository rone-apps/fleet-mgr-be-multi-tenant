package com.taxi.web.dto.expense;

import com.taxi.domain.expense.model.ApplicationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * ApplicationPreviewDTO - Preview of how an expense category will be applied
 *
 * Shows users which entities will receive the expense before they create it.
 * This prevents surprises and helps verify the configuration is correct.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationPreviewDTO {

    /**
     * The application type of this category
     */
    private ApplicationType applicationType;

    /**
     * Display label for the application type
     */
    private String applicationTypeLabel;

    /**
     * Total number of entities that will receive this expense
     */
    private int affectedEntityCount;

    /**
     * Names of first 10 entities that will receive the expense
     * (for preview purposes - full list obtained when creating)
     */
    private List<String> affectedEntityNames;

    /**
     * Target entity display (e.g., "Premium Sedan Profile" or "Cab 101 - DAY")
     */
    private String targetEntityDescription;

    /**
     * Human-readable description of the application
     * Example: "This expense will apply to 15 shifts with the 'Premium Sedan' profile"
     */
    private String description;
}
