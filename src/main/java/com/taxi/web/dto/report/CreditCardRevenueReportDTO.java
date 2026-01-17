package com.taxi.web.dto.report;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DTO for complete credit card revenue report
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditCardRevenueReportDTO {
    
    private String driverNumber;
    private String driverName;
    private LocalDate startDate;
    private LocalDate endDate;
    
    @Builder.Default
    private List<CreditCardRevenueDTO> transactionItems = new ArrayList<>();
    
    // Summary totals
    private Integer totalTransactions;
    private BigDecimal totalAmount;
    private BigDecimal totalTipAmount;
    private BigDecimal grandTotal;
    private BigDecimal totalProcessingFees;
    private BigDecimal netTotal;
    
    // Settlement breakdown
    private Integer settledTransactions;
    private BigDecimal settledAmount;
    private Integer unsettledTransactions;
    private BigDecimal unsettledAmount;
    
    // Refund breakdown
    private Integer refundedTransactions;
    private BigDecimal totalRefundedAmount;
    
    // Card type breakdown
    @Builder.Default
    private Map<String, CardTypeStats> cardTypeBreakdown = new HashMap<>();
    
    /**
     * Add a transaction item to the report
     */
    public void addTransactionItem(CreditCardRevenueDTO item) {
        this.transactionItems.add(item);
    }
    
    /**
     * Calculate all summary totals
     */
    public void calculateSummary() {
        this.totalTransactions = transactionItems.size();
        
        this.totalAmount = transactionItems.stream()
                .map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        this.totalTipAmount = transactionItems.stream()
                .map(t -> t.getTipAmount() != null ? t.getTipAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        this.grandTotal = totalAmount.add(totalTipAmount);
        
        this.totalProcessingFees = transactionItems.stream()
                .map(t -> t.getProcessingFee() != null ? t.getProcessingFee() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        this.netTotal = grandTotal.subtract(totalProcessingFees);
        
        // Settlement breakdown
        this.settledTransactions = (int) transactionItems.stream()
                .filter(t -> Boolean.TRUE.equals(t.getIsSettled()))
                .count();
        
        this.settledAmount = transactionItems.stream()
                .filter(t -> Boolean.TRUE.equals(t.getIsSettled()))
                .map(t -> t.getTotalAmount() != null ? t.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        this.unsettledTransactions = totalTransactions - settledTransactions;
        this.unsettledAmount = grandTotal.subtract(settledAmount);
        
        // Refund breakdown
        this.refundedTransactions = (int) transactionItems.stream()
                .filter(t -> Boolean.TRUE.equals(t.getIsRefunded()))
                .count();
        
        this.totalRefundedAmount = transactionItems.stream()
                .filter(t -> Boolean.TRUE.equals(t.getIsRefunded()))
                .map(t -> t.getRefundAmount() != null ? t.getRefundAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Card type breakdown
        calculateCardTypeBreakdown();
    }
    
    /**
     * Calculate statistics by card type
     */
    private void calculateCardTypeBreakdown() {
        cardTypeBreakdown.clear();
        
        transactionItems.stream()
                .filter(t -> t.getCardType() != null)
                .forEach(transaction -> {
                    String cardType = transaction.getCardType();
                    CardTypeStats stats = cardTypeBreakdown.getOrDefault(
                            cardType, 
                            new CardTypeStats(cardType)
                    );
                    
                    stats.incrementCount();
                    stats.addAmount(transaction.getTotalAmount() != null ? transaction.getTotalAmount() : BigDecimal.ZERO);
                    
                    cardTypeBreakdown.put(cardType, stats);
                });
    }
    
    /**
     * Statistics for a specific card type
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardTypeStats {
        private String cardType;
        private Integer count = 0;
        private BigDecimal totalAmount = BigDecimal.ZERO;
        
        public CardTypeStats(String cardType) {
            this.cardType = cardType;
            this.count = 0;
            this.totalAmount = BigDecimal.ZERO;
        }
        
        public void incrementCount() {
            this.count++;
        }
        
        public void addAmount(BigDecimal amount) {
            this.totalAmount = this.totalAmount.add(amount);
        }
    }
}
