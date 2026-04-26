package com.taxi.web.dto.receipt;

public class ConfirmReceiptRequest {
    private Long receiptId;
    private String documentType;
    private Long cabId;
    private Long ownerId;
    private Long accountCustomerId;

    public ConfirmReceiptRequest() {}

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

    public Long getCabId() {
        return cabId;
    }

    public void setCabId(Long cabId) {
        this.cabId = cabId;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public Long getAccountCustomerId() {
        return accountCustomerId;
    }

    public void setAccountCustomerId(Long accountCustomerId) {
        this.accountCustomerId = accountCustomerId;
    }
}
