package com.taxi.web.dto.report;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for a single fixed expense item in the report
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FixedExpenseItemDTO {
    
    // Expense details
    private Long expenseId;
    private String description;
    private String category;
    private String expenseType;  // RECURRING or ONE_TIME
    
    // Assignment
    private String assignedTo;     // e.g., "CAB-001", "CAB-001 DAY", "John Smith"
    private String assignedToType; // CAB, SHIFT, DRIVER, OWNER
    
    // Amounts
    private BigDecimal originalAmount;  // Original expense amount
    private BigDecimal chargedAmount;   // Amount charged to this shift owner
    private String splitNote;           // e.g., "50% of cab expense"
    
    // Dates
    private LocalDate startDate;
    private LocalDate endDate;
    
    // Who pays
    private String ownerDriverNumber;
    private String ownerDriverName;
    private String cabNumber;
    private String shiftType;
}