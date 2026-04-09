package com.taxi.web.dto.tax;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncomeTaxBracketDTO {
    private Long id;
    private Integer taxYear;
    private String jurisdiction;
    private Integer bracketOrder;
    private BigDecimal minIncome;
    private BigDecimal maxIncome;
    private BigDecimal rate;
    private LocalDateTime createdAt;
}
