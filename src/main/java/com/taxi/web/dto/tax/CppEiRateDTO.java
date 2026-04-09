package com.taxi.web.dto.tax;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CppEiRateDTO {
    private Long id;
    private Integer taxYear;
    private BigDecimal cppEmployeeRate;
    private BigDecimal cppMaxPensionable;
    private BigDecimal cppBasicExemption;
    private BigDecimal eiEmployeeRate;
    private BigDecimal eiMaxInsurable;
    private LocalDateTime createdAt;
}
