package com.taxi.web.dto.expense;

import com.taxi.domain.shift.model.ShiftType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for creating a lease expense
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLeaseExpenseRequest {
    private Long driverId;
    private Long cabId;
    private LocalDate leaseDate;
    private ShiftType shiftType;
    private BigDecimal milesDriven;
    private Long shiftId; // Optional - if calculating from a shift
    private String notes;
}
