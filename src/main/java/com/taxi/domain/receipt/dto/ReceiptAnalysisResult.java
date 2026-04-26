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
    private BigDecimal tipAmount;
    private String passengerName;
    private String pickupAddress;
    private String dropoffAddress;
    private String startTime;
    private java.util.Map<String, Object> extractedFields;
    private java.util.Map<String, Object> rawJsonData;
    private String classifiedType;

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

    public BigDecimal getTipAmount() {
        return tipAmount;
    }

    public void setTipAmount(BigDecimal tipAmount) {
        this.tipAmount = tipAmount;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public void setPassengerName(String passengerName) {
        this.passengerName = passengerName;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public void setPickupAddress(String pickupAddress) {
        this.pickupAddress = pickupAddress;
    }

    public String getDropoffAddress() {
        return dropoffAddress;
    }

    public void setDropoffAddress(String dropoffAddress) {
        this.dropoffAddress = dropoffAddress;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public java.util.Map<String, Object> getExtractedFields() {
        return extractedFields;
    }

    public void setExtractedFields(java.util.Map<String, Object> extractedFields) {
        this.extractedFields = extractedFields;
    }

    public java.util.Map<String, Object> getRawJsonData() {
        return rawJsonData;
    }

    public void setRawJsonData(java.util.Map<String, Object> rawJsonData) {
        this.rawJsonData = rawJsonData;
    }

    public String getClassifiedType() {
        return classifiedType;
    }

    public void setClassifiedType(String classifiedType) {
        this.classifiedType = classifiedType;
    }
}
