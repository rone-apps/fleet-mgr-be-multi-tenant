package com.taxi.web.dto.expense;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for changing the rate of a recurring expense
 */
@Data
public class ChangeRateRequest {
    private BigDecimal newAmount;
    private LocalDate effectiveDate;
    private String notes;
}
