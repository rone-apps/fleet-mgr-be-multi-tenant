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
public class CreateItemRateRequest {
    private String name;
    private ItemRateUnitType unitType;
    private BigDecimal rate;
    private ItemRateChargedTo chargedTo;
    private LocalDate effectiveFrom;
    private String notes;
}
