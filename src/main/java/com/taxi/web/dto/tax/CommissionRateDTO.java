package com.taxi.web.dto.tax;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommissionRateDTO {
    private Long id;
    private Long commissionTypeId;
    private String commissionTypeName;
    private BigDecimal rate;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Boolean isActive;
    private String notes;
    private LocalDateTime createdAt;
}
