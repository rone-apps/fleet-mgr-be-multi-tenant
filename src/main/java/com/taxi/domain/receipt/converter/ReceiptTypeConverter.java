package com.taxi.domain.receipt.converter;

import com.taxi.domain.receipt.model.Receipt;
import com.taxi.domain.receipt.model.ReceiptType;
import com.taxi.web.dto.receipt.ConfirmReceiptRequest;

import java.util.Map;

public interface ReceiptTypeConverter {
    ReceiptType getSupportedType();

    /**
     * Convert parsed JSON data to domain records. Called during confirm step.
     * @param receipt     the persisted Receipt entity (has parsedDataJson already set)
     * @param parsedJson  deserialized Map of { header: {...}, items: [...] }
     * @param request     ConfirmReceiptRequest from frontend (user-edited fields)
     */
    void convert(Receipt receipt, Map<String, Object> parsedJson, ConfirmReceiptRequest request);
}
