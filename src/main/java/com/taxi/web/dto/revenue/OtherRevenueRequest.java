package com.taxi.web.dto.revenue;

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
    
    @NotNull(message = "Entity type is required")
    private String entityType;  // CAB, DRIVER, OWNER, SHIFT, COMPANY
    
    @NotNull(message = "Entity ID is required")
    private Long entityId;
    
    @NotNull(message = "Revenue type is required")
    private String revenueType;  // BONUS, CREDIT, ADJUSTMENT, etc.
    
    private String description;
    
    private String referenceNumber;
    
    private String paymentStatus;  // PENDING, PAID, CANCELLED, PROCESSING
    
    private String paymentMethod;
    
    private LocalDate paymentDate;
    
    private String notes;
    
    @NotNull(message = "Category ID is required")
    private Long categoryId;
}
