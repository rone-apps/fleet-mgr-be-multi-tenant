package com.taxi.web.dto.tax;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxCreditDTO {
    private Long id;
    private Integer taxYear;
    private String jurisdiction;
    private String creditCode;
    private String creditName;
    private BigDecimal amount;
    private BigDecimal rate;
    private String description;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
