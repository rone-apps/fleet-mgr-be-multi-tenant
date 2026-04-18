package com.taxi.domain.receipt.service;

import com.fasterxml.jackson.core.type.TypeReference;
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

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

@Service
public class ReceiptAnalysisService {
    private static final Logger logger = LoggerFactory.getLogger(ReceiptAnalysisService.class);

    @Value("${anthropic.api.key}")
    private String anthropicApiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public ReceiptAnalysisService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public ReceiptAnalysisResult analyzeReceipt(Long receiptId, byte[] imageData, String imageMimeType) {
        if (anthropicApiKey == null || anthropicApiKey.trim().isEmpty()) {
            throw new IllegalStateException("ANTHROPIC_API_KEY is not configured");
        }

        try {
            String imageBase64 = Base64.getEncoder().encodeToString(imageData);
            String mediaType = normalizeMediaType(imageMimeType);

            // Build Claude API request
            Map<String, Object> requestBody = buildClaudeRequest(imageBase64, mediaType);

            // Call Claude API
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", anthropicApiKey);
            headers.set("anthropic-version", "2023-06-01");

            HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(requestBody, headers);
            JsonNode response = restTemplate.postForObject(
                "https://api.anthropic.com/v1/messages",
                httpEntity,
                JsonNode.class
            );

            if (response == null) {
                throw new RuntimeException("No response from Claude API");
            }

            String responseText = extractResponseText(response);
            ReceiptAnalysisResult result = parseClaudeResponse(receiptId, responseText);
            return result;

        } catch (RestClientException e) {
            logger.error("Failed to call Claude API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to analyze receipt with Claude API: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error during receipt analysis: {}", e.getMessage(), e);
            throw new RuntimeException("Unexpected error during receipt analysis: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> buildClaudeRequest(String imageBase64, String mediaType) {
        String analysisPrompt = "Analyze this receipt or bill image. Extract structured data and return ONLY valid JSON:\n" +
            "{\n" +
            "  \"document_type\": \"one of: GAS_RECEIPT|PARKING|MAINTENANCE|BILL|ACCOUNT_CHARGE|AIRPORT_FEE|MEAL|OTHER\",\n" +
            "  \"vendor_name\": \"string (business/vendor name, or account number if no vendor name visible)\",\n" +
            "  \"account_number\": \"string or null (any account/reference/confirmation number on receipt)\",\n" +
            "  \"receipt_date\": \"YYYY-MM-DD or null\",\n" +
            "  \"subtotal\": number or null,\n" +
            "  \"tax_amount\": number or null,\n" +
            "  \"total_amount\": number or null,\n" +
            "  \"cab_number\": \"string or null (any handwritten or printed cab/vehicle number)\",\n" +
            "  \"driver_name\": \"string or null (any handwritten or printed driver/owner name)\",\n" +
            "  \"line_items\": [{\"description\": \"string\", \"quantity\": number, \"unit_price\": number, \"total\": number}]\n" +
            "}\n" +
            "If any field cannot be determined, use null. Return ONLY the JSON object, no additional text.";

        // Build image content block
        Map<String, Object> imageContent = new HashMap<>();
        imageContent.put("type", "image");
        Map<String, Object> imageData = new HashMap<>();
        imageData.put("type", "base64");
        imageData.put("media_type", mediaType);
        imageData.put("data", imageBase64);
        imageContent.put("source", imageData);

        // Build text content block
        Map<String, Object> textContent = new HashMap<>();
        textContent.put("type", "text");
        textContent.put("text", analysisPrompt);

        // Build message
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        List<Map<String, Object>> content = new ArrayList<>();
        content.add(imageContent);
        content.add(textContent);
        message.put("content", content);

        // Build request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "claude-haiku-4-5-20251001");
        requestBody.put("max_tokens", 1024);
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(message);
        requestBody.put("messages", messages);

        return requestBody;
    }

    private String normalizeMediaType(String mimeType) {
        if (mimeType == null) {
            return "image/jpeg";
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
        return "image/jpeg";
    }

    private String extractResponseText(JsonNode response) {
        if (response.has("content") && response.get("content").isArray()) {
            for (JsonNode content : response.get("content")) {
                if (content.has("type") && "text".equals(content.get("type").asText())) {
                    if (content.has("text")) {
                        return content.get("text").asText();
                    }
                }
            }
        }
        throw new RuntimeException("Failed to extract text from Claude response");
    }

    private ReceiptAnalysisResult parseClaudeResponse(Long receiptId, String responseText) {
        try {
            // Extract JSON from markdown code blocks if present
            String jsonText = responseText;
            if (responseText.contains("```json")) {
                jsonText = responseText.replaceAll("```json\\s*", "").replaceAll("```\\s*", "");
            } else if (responseText.contains("```")) {
                jsonText = responseText.replaceAll("```\\s*", "");
            }

            JsonNode jsonNode = objectMapper.readTree(jsonText);

            String documentType = jsonNode.has("document_type") && !jsonNode.get("document_type").isNull()
                ? jsonNode.get("document_type").asText()
                : "OTHER";

            String vendorName = jsonNode.has("vendor_name") && !jsonNode.get("vendor_name").isNull()
                ? jsonNode.get("vendor_name").asText()
                : "";

            String accountNumber = null;
            if (jsonNode.has("account_number") && !jsonNode.get("account_number").isNull()) {
                accountNumber = jsonNode.get("account_number").asText();
            }

            LocalDate receiptDate = null;
            if (jsonNode.has("receipt_date") && !jsonNode.get("receipt_date").isNull()) {
                String dateStr = jsonNode.get("receipt_date").asText();
                if (dateStr != null && !dateStr.isEmpty()) {
                    receiptDate = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
                }
            }

            BigDecimal subtotal = null;
            if (jsonNode.has("subtotal") && !jsonNode.get("subtotal").isNull()) {
                subtotal = new BigDecimal(jsonNode.get("subtotal").asDouble());
            }

            BigDecimal taxAmount = null;
            if (jsonNode.has("tax_amount") && !jsonNode.get("tax_amount").isNull()) {
                taxAmount = new BigDecimal(jsonNode.get("tax_amount").asDouble());
            }

            BigDecimal totalAmount = null;
            if (jsonNode.has("total_amount") && !jsonNode.get("total_amount").isNull()) {
                totalAmount = new BigDecimal(jsonNode.get("total_amount").asDouble());
            }

            String cabNumber = null;
            if (jsonNode.has("cab_number") && !jsonNode.get("cab_number").isNull()) {
                cabNumber = jsonNode.get("cab_number").asText();
            }

            String driverName = null;
            if (jsonNode.has("driver_name") && !jsonNode.get("driver_name").isNull()) {
                driverName = jsonNode.get("driver_name").asText();
            }

            List<LineItem> lineItems = new ArrayList<>();
            if (jsonNode.has("line_items") && jsonNode.get("line_items").isArray()) {
                List<Map<String, Object>> items = objectMapper.convertValue(
                    jsonNode.get("line_items"),
                    new TypeReference<List<Map<String, Object>>>() {}
                );
                for (Map<String, Object> item : items) {
                    LineItem lineItem = new LineItem();
                    lineItem.setDescription((String) item.get("description"));
                    Object qty = item.get("quantity");
                    if (qty != null) {
                        lineItem.setQuantity(new BigDecimal(qty.toString()));
                    }
                    Object unitPrice = item.get("unit_price");
                    if (unitPrice != null) {
                        lineItem.setUnitPrice(new BigDecimal(unitPrice.toString()));
                    }
                    Object total = item.get("total");
                    if (total != null) {
                        lineItem.setTotal(new BigDecimal(total.toString()));
                    }
                    lineItems.add(lineItem);
                }
            }

            ReceiptAnalysisResult result = new ReceiptAnalysisResult(
                receiptId,
                documentType,
                vendorName,
                receiptDate,
                subtotal,
                taxAmount,
                totalAmount,
                lineItems
            );
            result.setAccountNumber(accountNumber);
            result.setCabNumber(cabNumber);
            result.setDriverName(driverName);
            return result;
        } catch (Exception e) {
            logger.error("Failed to parse Claude response", e);
            throw new RuntimeException("Failed to parse Claude analysis result", e);
        }
    }
}
