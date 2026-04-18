package com.taxi.domain.receipt.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ReceiptAnalysisResult {
    private Long receiptId;
    private String documentType;
    private String vendorName;
    private String accountNumber;
    private LocalDate receiptDate;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private List<LineItem> lineItems;
    private String cabNumber;
    private String driverName;

    public ReceiptAnalysisResult() {}

    public ReceiptAnalysisResult(Long receiptId, String documentType, String vendorName,
                                 LocalDate receiptDate, BigDecimal subtotal, BigDecimal taxAmount,
                                 BigDecimal totalAmount, List<LineItem> lineItems) {
        this.receiptId = receiptId;
        this.documentType = documentType;
        this.vendorName = vendorName;
        this.receiptDate = receiptDate;
        this.subtotal = subtotal;
        this.taxAmount = taxAmount;
        this.totalAmount = totalAmount;
        this.lineItems = lineItems;
        this.cabNumber = null;
        this.driverName = null;
    }

    public Long getReceiptId() {
        return receiptId;
    }

    public void setReceiptId(Long receiptId) {
        this.receiptId = receiptId;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getVendorName() {
        return vendorName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public LocalDate getReceiptDate() {
        return receiptDate;
    }

    public void setReceiptDate(LocalDate receiptDate) {
        this.receiptDate = receiptDate;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public List<LineItem> getLineItems() {
        return lineItems;
    }

    public void setLineItems(List<LineItem> lineItems) {
        this.lineItems = lineItems;
    }

    public String getCabNumber() {
        return cabNumber;
    }

    public void setCabNumber(String cabNumber) {
        this.cabNumber = cabNumber;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }
}
