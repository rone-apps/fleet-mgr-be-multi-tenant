package com.taxi.web.dto.report;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for complete charges revenue report
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargesRevenueReportDTO {
    
    private String driverNumber;
    private String driverName;
    private LocalDate startDate;
    private LocalDate endDate;
    
    @Builder.Default
    private List<ChargesRevenueDTO> chargeItems = new ArrayList<>();
    
    // Summary totals
    private Integer totalCharges;
    private BigDecimal totalFareAmount;
    private BigDecimal totalTipAmount;
    private BigDecimal grandTotal;
    
    // Payment breakdown
    private Integer paidCharges;
    private BigDecimal paidAmount;
    private Integer unpaidCharges;
    private BigDecimal unpaidAmount;
    
    /**
     * Add a charge item to the report
     */
    public void addChargeItem(ChargesRevenueDTO item) {
        this.chargeItems.add(item);
    }
    
    /**
     * Calculate all summary totals
     */
    public void calculateSummary() {
        this.totalCharges = chargeItems.size();
        
        this.totalFareAmount = chargeItems.stream()
                .map(c -> c.getFareAmount() != null ? c.getFareAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        this.totalTipAmount = chargeItems.stream()
                .map(c -> c.getTipAmount() != null ? c.getTipAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        this.grandTotal = totalFareAmount.add(totalTipAmount);
        
        // Paid vs Unpaid breakdown
        this.paidCharges = (int) chargeItems.stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsPaid()))
                .count();
        
        this.paidAmount = chargeItems.stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsPaid()))
                .map(c -> c.getTotalAmount() != null ? c.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        this.unpaidCharges = totalCharges - paidCharges;
        this.unpaidAmount = grandTotal.subtract(paidAmount);
    }
}