package com.taxi.domain.wcb.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WcbProcessingResult {
    private String status; // SUCCESS, PARTIAL, DUPLICATE_SUMMARY
    private Long remittanceId; // ID of summary (new or existing)
    private String payeeName;
    private java.math.BigDecimal totalAmount;
    private String totalMismatchWarning;

    private SummaryResult summary = new SummaryResult();
    private LineItemsResult lineItems = new LineItemsResult();

    public static class SummaryResult {
        public String status; // SAVED, REJECTED
        public String reason;
        public Long existingReceiptId; // If rejected due to duplicate

        public SummaryResult() {}
        public SummaryResult(String status, String reason) {
            this.status = status;
            this.reason = reason;
        }
    }

    public static class LineItemsResult {
        public int totalProcessed = 0;
        public int saved = 0;
        public int rejected = 0;
        public List<String> rejectionReasons = new ArrayList<>();

        public void addRejection(String reason) {
            rejected++;
            if (!rejectionReasons.contains(reason)) {
                rejectionReasons.add(reason);
            }
        }
    }

    // Getters and setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getRemittanceId() { return remittanceId; }
    public void setRemittanceId(Long remittanceId) { this.remittanceId = remittanceId; }

    public String getPayeeName() { return payeeName; }
    public void setPayeeName(String payeeName) { this.payeeName = payeeName; }

    public java.math.BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(java.math.BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getTotalMismatchWarning() { return totalMismatchWarning; }
    public void setTotalMismatchWarning(String totalMismatchWarning) { this.totalMismatchWarning = totalMismatchWarning; }

    public SummaryResult getSummary() { return summary; }
    public void setSummary(SummaryResult summary) { this.summary = summary; }

    public LineItemsResult getLineItems() { return lineItems; }
    public void setLineItems(LineItemsResult lineItems) { this.lineItems = lineItems; }

    public Map<String, Object> toMap() {
        return Map.of(
            "status", status,
            "remittanceId", remittanceId != null ? remittanceId : "null",
            "payeeName", payeeName != null ? payeeName : "",
            "totalAmount", totalAmount != null ? totalAmount.toString() : "",
            "totalMismatchWarning", totalMismatchWarning != null ? totalMismatchWarning : "",
            "summary", Map.of(
                "status", summary.status,
                "reason", summary.reason != null ? summary.reason : "",
                "existingReceiptId", summary.existingReceiptId != null ? summary.existingReceiptId : "null"
            ),
            "lineItems", Map.of(
                "totalProcessed", lineItems.totalProcessed,
                "saved", lineItems.saved,
                "rejected", lineItems.rejected,
                "rejectionReasons", lineItems.rejectionReasons
            )
        );
    }
}
