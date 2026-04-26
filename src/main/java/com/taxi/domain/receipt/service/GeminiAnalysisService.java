package com.taxi.domain.receipt.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxi.domain.receipt.dto.LineItem;
import com.taxi.domain.receipt.dto.ReceiptAnalysisResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeminiAnalysisService {
    private static final Logger logger = LoggerFactory.getLogger(GeminiAnalysisService.class);

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GeminiAnalysisService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public ReceiptAnalysisResult analyzeReceipt(Long receiptId, byte[] imageData, String imageMimeType) {
        if (geminiApiKey == null || geminiApiKey.trim().isEmpty()) {
            throw new IllegalStateException("GEMINI_API_KEY is not configured");
        }

        try {
            String imageBase64 = Base64.getEncoder().encodeToString(imageData);

            logger.info("Analyzing receipt with Gemini - Image size: {} bytes", imageData.length);

            // Build Gemini API request
            Map<String, Object> requestBody = buildGeminiRequest(imageBase64, imageMimeType);

            // Call Gemini API via REST
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(requestBody, headers);
            String url = String.format("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=%s", geminiApiKey);

            JsonNode response = restTemplate.postForObject(url, httpEntity, JsonNode.class);

            if (response == null) {
                throw new RuntimeException("No response from Gemini API");
            }

            String responseText = extractResponseText(response);
            ReceiptAnalysisResult result = parseGeminiResponse(receiptId, responseText);

            logger.info("Receipt analysis completed with Gemini: {}", receiptId);
            return result;

        } catch (RestClientException e) {
            logger.error("Failed to call Gemini API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to analyze receipt with Gemini API: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error during Gemini receipt analysis: {}", e.getMessage(), e);
            throw new RuntimeException("Unexpected error during Gemini receipt analysis: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> buildGeminiRequest(String imageBase64, String mediaType) {
        String analysisPrompt = buildAnalysisPrompt();

        // Build image content part
        Map<String, Object> inlineData = new HashMap<>();
        inlineData.put("mimeType", normalizeMediaType(mediaType));
        inlineData.put("data", imageBase64);

        Map<String, Object> imagePart = new HashMap<>();
        imagePart.put("inlineData", inlineData);

        // Build text content part
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", analysisPrompt);

        // Build request
        Map<String, Object> content = new HashMap<>();
        content.put("role", "user");
        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(imagePart);
        parts.add(textPart);
        content.put("parts", parts);

        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("maxOutputTokens", 4096);
        requestBody.put("generationConfig", generationConfig);

        List<Map<String, Object>> contents = new ArrayList<>();
        contents.add(content);
        requestBody.put("contents", contents);

        return requestBody;
    }

    private String extractResponseText(JsonNode response) {
        if (response.has("candidates") && response.get("candidates").isArray()) {
            JsonNode firstCandidate = response.get("candidates").get(0);
            if (firstCandidate.has("content") && firstCandidate.get("content").has("parts")) {
                JsonNode parts = firstCandidate.get("content").get("parts");
                if (parts.isArray() && parts.size() > 0) {
                    JsonNode firstPart = parts.get(0);
                    if (firstPart.has("text")) {
                        return firstPart.get("text").asText();
                    }
                }
            }
        }
        throw new RuntimeException("Failed to extract text from Gemini response");
    }

    private String buildAnalysisPrompt() {
        return "PARSE THIS DOCUMENT INTO STRUCTURED JSON.\n\n" +
            "The document has TWO sections:\n" +
            "1. HEADER - Top section with summary/metadata information\n" +
            "2. ITEMS - Table or list of detailed line items/rows\n\n" +
            "JSON STRUCTURE (REQUIRED):\n" +
            "{\n" +
            "  \"header\": {\n" +
            "    \"field_name\": \"value\",\n" +
            "    \"total_paid_amount\": \"$1,234.56\",\n" +
            "    \"cheque_date\": \"Jan 15 2026\"\n" +
            "  },\n" +
            "  \"items\": [\n" +
            "    {\"invoice_no\": \"ABC-123\", \"name\": \"John Doe\", \"service_date\": \"Jan 01 2026\", \"service_code\": \"1100546\", \"description\": \"Service Type\", \"units\": \"1.0\", \"rate\": \"$100.00\", \"amount\": \"$100.00\", \"explanation\": null},\n" +
            "    {\"invoice_no\": \"ABC-124\", \"name\": \"Jane Doe\", \"service_date\": \"Jan 02 2026\", \"service_code\": \"1100546\", \"description\": \"Service Type\", \"units\": \"2.0\", \"rate\": \"$50.00\", \"amount\": \"$100.00\", \"explanation\": null}\n" +
            "  ]\n" +
            "}\n\n" +
            "EXTRACTION INSTRUCTIONS:\n" +
            "1. Extract ALL header fields from the top/summary section\n" +
            "2. Extract ALL items/rows from the table or itemized list\n" +
            "3. Standardize column names: use lowercase with underscores (invoice_no, service_date, service_code, etc.)\n" +
            "4. Use these standard item fields when available: invoice_no, claim_no, name, service_date, service_code, description, units, rate, amount, explanation\n" +
            "5. Format ALL currency/money values as strings with $ and comma separators: \"$1,234.56\"\n" +
            "6. Format ALL dates as readable text: \"Jan 15 2026\" (abbreviated month, day, year - NOT ISO format)\n" +
            "7. Keep all text values as strings (names, descriptions, codes)\n" +
            "8. Use null for truly missing/empty values\n" +
            "9. Return ONLY valid JSON (no markdown, no code blocks, no extra text)\n" +
            "10. Include all rows from the table/items section - extract EVERY single row\n" +
            "11. Close all brackets and braces properly\n" +
            "12. Do NOT include page numbers, headers, footers, or metadata";
    }

    private ReceiptAnalysisResult parseGeminiResponse(Long receiptId, String responseText) {
        try {
            // Extract JSON from markdown code blocks if present
            String jsonText = responseText;
            if (responseText.contains("```json")) {
                jsonText = responseText.replaceAll("```json\\s*", "").replaceAll("```\\s*", "");
            } else if (responseText.contains("```")) {
                jsonText = responseText.replaceAll("```\\s*", "");
            }
            jsonText = jsonText.trim();

            logger.debug("Parsing Gemini response for receipt {}", receiptId);
            ReceiptAnalysisResult result = new ReceiptAnalysisResult(receiptId, "OTHER", "", null, null, null, null, new ArrayList<>());

            // Try normal JSON parsing first
            try {
                JsonNode jsonNode = objectMapper.readTree(jsonText);
                logger.info("✅ JSON parsed successfully");

                // Store complete raw JSON data for frontend display
                java.util.Map<String, Object> rawJsonMap = objectMapper.convertValue(
                    jsonNode,
                    new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {}
                );
                result.setRawJsonData(rawJsonMap);
                logger.info("💾 rawJsonData set: {} keys at top level", rawJsonMap.keySet());
                if (rawJsonMap.containsKey("header")) {
                    logger.info("  - header: {} fields", ((Map<?, ?>) rawJsonMap.get("header")).size());
                }
                if (rawJsonMap.containsKey("items")) {
                    logger.info("  - items: {} rows", ((List<?>) rawJsonMap.get("items")).size());
                }

                // Extract header fields (top section)
                List<LineItem> lineItems = new ArrayList<>();
                if (jsonNode.has("header") && jsonNode.get("header").isObject()) {
                    java.util.Map<String, Object> extractedFields = objectMapper.convertValue(
                        jsonNode.get("header"),
                        new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {}
                    );
                    result.setExtractedFields(extractedFields);
                    logger.info("Extracted {} header fields for receipt {}", extractedFields.size(), receiptId);
                }

                // Extract items (table rows)
                if (jsonNode.has("items") && jsonNode.get("items").isArray()) {
                    for (JsonNode item : jsonNode.get("items")) {
                        LineItem lineItem = new LineItem();
                        lineItem.setDescription(item.has("description") ? item.get("description").asText() : null);
                        if (item.has("quantity") && !item.get("quantity").isNull()) {
                            lineItem.setQuantity(new BigDecimal(item.get("quantity").asDouble()));
                        }
                        if (item.has("unit_price") && !item.get("unit_price").isNull()) {
                            lineItem.setUnitPrice(new BigDecimal(item.get("unit_price").asDouble()));
                        }
                        if (item.has("total") && !item.get("total").isNull()) {
                            lineItem.setTotal(new BigDecimal(item.get("total").asDouble()));
                        }
                        lineItems.add(lineItem);
                    }
                    result.setLineItems(lineItems);
                }
                logger.info("Successfully parsed receipt {} with {} items", receiptId, lineItems.size());
                return result;

            } catch (Exception jsonParseError) {
                // JSON parsing failed - use regex-based fallback extraction
                logger.warn("❌ JSON parsing FAILED for receipt {}: {}", receiptId, jsonParseError.getMessage());
                logger.warn("Response text (first 500 chars): {}", jsonText.substring(0, Math.min(500, jsonText.length())));
                java.util.Map<String, Object> extractedFields = extractFieldsRegex(jsonText);
                result.setExtractedFields(extractedFields);
                // NOTE: rawJsonData is NOT set here - only extractedFields
                logger.info("📋 Fallback: Extracted {} fields via regex (NO rawJsonData)", extractedFields.size());
                return result;
            }

        } catch (Exception e) {
            logger.error("Failed to parse Gemini response for receipt {}: {}", receiptId, e.getMessage());
            logger.debug("Raw response text (first 2000 chars): {}", responseText.substring(0, Math.min(2000, responseText.length())));
            throw new RuntimeException("Failed to parse Gemini analysis result: " + e.getMessage(), e);
        }
    }

    private java.util.Map<String, Object> extractFieldsRegex(String text) {
        java.util.Map<String, Object> fields = new java.util.LinkedHashMap<>();

        // Extract key-value pairs: "key": value (handles strings, numbers, null)
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "\"([^\"]+)\"\\s*:\\s*(?:\"([^\"]*)\"|([0-9.-]+)|null|true|false)"
        );
        java.util.regex.Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String key = matcher.group(1);
            String stringValue = matcher.group(2);
            String numericValue = matcher.group(3);

            Object value;
            if (stringValue != null) {
                value = stringValue.isEmpty() ? null : stringValue;
            } else if (numericValue != null) {
                try {
                    value = numericValue.contains(".") ? Double.parseDouble(numericValue) : Long.parseLong(numericValue);
                } catch (NumberFormatException e) {
                    value = numericValue;
                }
            } else {
                value = null;
            }

            if (value != null && !key.equals("extracted_fields") && !key.equals("line_items")) {
                fields.put(key, value);
            }
        }

        return fields;
    }

    private String normalizeMediaType(String mimeType) {
        if (mimeType == null) {
            return "image/jpeg";
        }
        if (mimeType.contains("heic") || mimeType.contains("heif")) {
            return "image/heic";
        }
        if (mimeType.contains("jpeg") || mimeType.contains("jpg")) {
            return "image/jpeg";
        }
        if (mimeType.contains("png")) {
            return "image/png";
        }
        if (mimeType.contains("gif")) {
            return "image/gif";
        }
        if (mimeType.contains("webp")) {
            return "image/webp";
        }
        return mimeType;
    }

}
