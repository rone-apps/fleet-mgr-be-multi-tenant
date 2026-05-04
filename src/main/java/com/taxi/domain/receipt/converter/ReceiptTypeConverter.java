package com.taxi.domain.receipt.converter;

import com.taxi.domain.receipt.model.Receipt;
import com.taxi.domain.receipt.model.ReceiptType;
import com.taxi.web.dto.receipt.ConfirmReceiptRequest;

import java.util.Collections;
import java.util.List;
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

    /**
     * Validate parsed JSON before saving domain records. Override in subclasses to enforce requirements.
     * @param parsedJson deserialized Map of { header: {...}, items: [...] }
     * @return List of validation error messages (empty = no errors)
     */
    default List<String> validate(Map<String, Object> parsedJson) {
        return Collections.emptyList();
    }
}
