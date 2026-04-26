package com.taxi.domain.receipt.model;

import java.util.List;

public enum ReceiptType {
    GAS_RECEIPT("Gas Receipt",
        List.of("fuel", "gas", "petrol", "liters", "litres", "gallons", "pump", "unleaded")),
    PARKING("Parking",
        List.of("parking", "parkade", "lot", "meter", "stall", "hourly rate")),
    MAINTENANCE("Maintenance",
        List.of("oil change", "repair", "service", "parts", "labor", "labour", "mechanic", "tire", "brake", "lube")),
    BILL("Bill",
        List.of("invoice", "bill", "due date", "account number", "statement of account")),
    ACCOUNT_CHARGE("Account Charge",
        List.of("remittance", "wcb", "workers compensation", "worker travel", "workers comp", "account charge")),
    AIRPORT_FEE("Airport Fee",
        List.of("airport", "yvr", "yyz", "yul", "pearson", "airport fee")),
    MEAL("Meal",
        List.of("restaurant", "food", "meal", "cafe", "coffee", "breakfast", "lunch", "dinner", "dine")),
    OTHER("Other", List.of()),
    UNMATCHED("Unmatched", List.of());

    private final String displayName;
    private final List<String> keywords;

    ReceiptType(String displayName, List<String> keywords) {
        this.displayName = displayName;
        this.keywords = keywords;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public static ReceiptType fromString(String value) {
        if (value == null) {
            return UNMATCHED;
        }
        for (ReceiptType t : values()) {
            if (t.name().equalsIgnoreCase(value) || t.displayName.equalsIgnoreCase(value)) {
                return t;
            }
        }
        return UNMATCHED;
    }
}
