package com.taxi.web.dto.revenue;

import com.taxi.domain.expense.model.ApplicationType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for creating/updating OtherRevenue
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtherRevenueRequest {
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "Revenue date is required")
    private LocalDate revenueDate;

    // ✅ Legacy system - now optional (use ApplicationType instead)
    private String entityType;  // CAB, DRIVER, OWNER, SHIFT, COMPANY

    // ✅ Legacy system - now optional (use ApplicationType instead)
    private Long entityId;

    @NotNull(message = "Revenue type is required")
    private String revenueType;  // BONUS, CREDIT, ADJUSTMENT, etc.

    private String description;

    private String referenceNumber;

    private String paymentStatus;  // PENDING, PAID, CANCELLED, PROCESSING

    private String paymentMethod;

    private LocalDate paymentDate;

    private String notes;

    // ✅ Category is optional for new ApplicationType system
    private Long categoryId;

    // ✅ NEW: Application Type System (matching OneTimeExpense)
    // Allows revenues to be applied using the same targeting criteria as expenses
    private ApplicationType applicationType;

    // Specific relationship IDs based on application type
    private Long shiftProfileId;
    private Long specificShiftId;
    private Long specificPersonId;
    private Long attributeTypeId;
}
