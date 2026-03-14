package com.taxi.web.dto.tax;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxRateDTO {
    private Long id;
    private Long taxTypeId;
    private String taxTypeName;
    private BigDecimal rate;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Boolean isActive;
    private String notes;
    private LocalDateTime createdAt;
}
