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
public class ItemRateOverrideDTO {
    private Long id;
    private Long itemRateId;
    private String itemRateName;  // For display
    private String ownerDriverNumber;
    private String cabNumber;  // nullable
    private String shiftType;  // nullable
    private String dayOfWeek;  // nullable
    private BigDecimal overrideRate;
    private Integer priority;
    private LocalDate startDate;
    private LocalDate endDate;  // nullable
    private Boolean isActive;
    private String notes;
}
