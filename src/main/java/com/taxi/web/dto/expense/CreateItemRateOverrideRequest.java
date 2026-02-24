package com.taxi.web.dto.expense;

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
public class CreateItemRateOverrideRequest {
    private Long itemRateId;
    private String ownerDriverNumber;
    private String cabNumber;  // optional
    private String shiftType;  // optional
    private String dayOfWeek;  // optional
    private BigDecimal overrideRate;
    private LocalDate startDate;
    private LocalDate endDate;  // optional
    private String notes;
}
