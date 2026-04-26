package com.taxi.domain.receipt.service;

import com.taxi.domain.receipt.model.ReceiptType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ReceiptClassifierService {
    private static final Logger logger = LoggerFactory.getLogger(ReceiptClassifierService.class);

    /**
     * Classify receipt type. Priority:
     * 1. AI-suggested type if it maps to a known ReceiptType (not OTHER/UNMATCHED)
     * 2. Keyword scan on all string values in parsedJson (header + items)
     * 3. UNMATCHED
     */
    public ReceiptType classify(String aiSuggestedType, Map<String, Object> parsedJson) {
        logger.info("🔍 Classifying receipt - AI suggested type: {}, Has JSON: {}", aiSuggestedType, parsedJson != null);

        // Step 1: trust AI if it's a recognized non-generic type
        ReceiptType aiType = ReceiptType.fromString(aiSuggestedType);
        if (aiType != ReceiptType.OTHER && aiType != ReceiptType.UNMATCHED) {
            logger.info("✅ Using AI-suggested type: {}", aiType);
            return aiType;
        }

        // Step 2: keyword scan on all string values in the JSON
        if (parsedJson != null) {
            String allText = flattenJsonToLowercase(parsedJson);
            logger.debug("📝 Flattened JSON text (first 500 chars): {}", allText.substring(0, Math.min(500, allText.length())));

            for (ReceiptType type : ReceiptType.values()) {
                if (type == ReceiptType.OTHER || type == ReceiptType.UNMATCHED) continue;
                for (String keyword : type.getKeywords()) {
                    if (allText.contains(keyword.toLowerCase())) {
                        logger.info("✅ Keyword '{}' matched type: {}", keyword, type);
                        return type;
                    }
                }
            }
            logger.warn("⚠️ No keywords matched in JSON");
        } else {
            logger.warn("⚠️ parsedJson is NULL - cannot classify");
        }

        logger.info("❌ No match found, returning UNMATCHED");
        return ReceiptType.UNMATCHED;
    }

    private String flattenJsonToLowercase(Map<String, Object> json) {
        StringBuilder sb = new StringBuilder();
        collectValues(json, sb);
        return sb.toString().toLowerCase();
    }

    @SuppressWarnings("unchecked")
    private void collectValues(Object obj, StringBuilder sb) {
        if (obj instanceof Map) {
            ((Map<?, ?>) obj).values().forEach(v -> collectValues(v, sb));
        } else if (obj instanceof List) {
            ((List<?>) obj).forEach(v -> collectValues(v, sb));
        } else if (obj != null) {
            sb.append(" ").append(obj);
        }
    }
}
