package com.taxi.web.dto.expense;

import com.taxi.domain.profile.model.ItemRateChargedTo;
import com.taxi.domain.profile.model.ItemRateUnitType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemRateDTO {
    private Long id;
    private String name;
    private ItemRateUnitType unitType;
    private String unitTypeDisplay;  // Display label like "Miles driven" or "Airport trips"
    private BigDecimal rate;
    private ItemRateChargedTo chargedTo;
    private String chargedToDisplay;  // Display label like "Charged to Driver"
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Boolean isActive;
    private String notes;
}
