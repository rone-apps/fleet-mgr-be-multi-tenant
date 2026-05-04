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
            // Validate and convert image if necessary
            byte[] processedImageData = validateAndProcessImage(imageData, imageMimeType);
            String mediaType = normalizeMediaType(imageMimeType);

            logger.info("Processing receipt image - Original size: {} bytes, mime type: {}, processed size: {} bytes",
                imageData.length, imageMimeType, processedImageData.length);

            String imageBase64 = Base64.getEncoder().encodeToString(processedImageData);

            // Validate Base64 string (must not have newlines or invalid chars)
            if (!imageBase64.matches("^[A-Za-z0-9+/]*={0,2}$")) {
                throw new IllegalArgumentException("Invalid Base64 encoding - contains invalid characters");
            }

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
        String analysisPrompt = "PARSE THIS DOCUMENT INTO STRUCTURED JSON.\n\n" +
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
        requestBody.put("model", "claude-opus-4-7");
        requestBody.put("max_tokens", 8192); // Increased from 4096 to accommodate full JSON with all line items
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(message);
        requestBody.put("messages", messages);

        return requestBody;
    }

    private byte[] validateAndProcessImage(byte[] imageData, String mimeType) throws IOException {
        if (imageData == null || imageData.length == 0) {
            throw new IllegalArgumentException("Image data is empty");
        }

        // iPhone sometimes reports HEIC as jpeg - detect actual format by magic bytes
        String actualFormat = detectImageFormat(imageData);
        logger.debug("Detected actual image format: {} (reported as: {})", actualFormat, mimeType);

        // Convert HEIC/HEIF to JPEG if needed
        if (actualFormat.equalsIgnoreCase("heic") || actualFormat.equalsIgnoreCase("heif")) {
            logger.info("Converting HEIC/HEIF image to JPEG for compatibility");
            return convertHeicToJpeg(imageData);
        }

        // Try to re-encode as JPEG if image appears corrupted or is unusual format
        if (actualFormat.equalsIgnoreCase("unknown") || imageData.length < 100) {
            logger.warn("Image format unknown or too small ({} bytes), attempting re-encode", imageData.length);
            return reencodeImageAsJpeg(imageData);
        }

        return imageData;
    }

    private String detectImageFormat(byte[] imageData) {
        if (imageData.length < 12) return "unknown";

        // Check magic bytes for common formats
        if (imageData[0] == (byte) 0xFF && imageData[1] == (byte) 0xD8) return "jpeg";
        if (imageData[0] == (byte) 0x89 && imageData[1] == 'P' && imageData[2] == 'N' && imageData[3] == 'G') return "png";
        if (imageData[0] == 'G' && imageData[1] == 'I' && imageData[2] == 'F') return "gif";
        if (imageData[0] == 'R' && imageData[1] == 'I' && imageData[2] == 'F' && imageData[3] == 'F') {
            // Check for WebP or HEIC
            if (imageData.length > 12 && imageData[8] == 'W' && imageData[9] == 'E') return "webp";
            if (imageData.length > 12 && imageData[8] == 'f' && imageData[9] == 't') return "heic"; // ftyp = HEIC
        }

        return "unknown";
    }

    private byte[] convertHeicToJpeg(byte[] heicData) throws IOException {
        try {
            // Try to read as BufferedImage and re-encode
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(heicData));
            if (image != null) {
                return reencodeImage(image, "jpeg");
            }
        } catch (Exception e) {
            logger.warn("Failed to convert HEIC via ImageIO: {}", e.getMessage());
        }

        // If ImageIO fails, return original - Anthropic API might support it
        logger.warn("Could not convert HEIC image, sending as-is to Claude API");
        return heicData;
    }

    private byte[] reencodeImageAsJpeg(byte[] imageData) throws IOException {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
            if (image != null) {
                return reencodeImage(image, "jpeg");
            }
        } catch (Exception e) {
            logger.warn("Failed to re-encode image: {}", e.getMessage());
        }

        // If all else fails, return original data
        return imageData;
    }

    private byte[] reencodeImage(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        byte[] result = baos.toByteArray();
        baos.close();
        logger.info("Re-encoded image to {} - new size: {} bytes", format, result.length);
        return result;
    }

    private String normalizeMediaType(String mimeType) {
        if (mimeType == null) {
            return "image/jpeg";
        }
        if (mimeType.contains("heic") || mimeType.contains("heif")) {
            return "image/jpeg"; // Convert HEIC to JPEG for Claude API compatibility
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
            jsonText = jsonText.trim();

            logger.debug("Parsing Claude response for receipt {}", receiptId);
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
            logger.error("Failed to parse Claude response for receipt {}: {}", receiptId, e.getMessage());
            logger.debug("Raw response text (first 2000 chars): {}", responseText.substring(0, Math.min(2000, responseText.length())));
            throw new RuntimeException("Failed to parse Claude analysis result: " + e.getMessage(), e);
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

    public ReceiptAnalysisResult analyzeText(Long receiptId, String rawText) {
        if (anthropicApiKey == null || anthropicApiKey.trim().isEmpty()) {
            throw new IllegalStateException("ANTHROPIC_API_KEY is not configured");
        }

        try {
            logger.info("Analyzing PDF text for receipt {} - {} characters", receiptId, rawText.length());
            Map<String, Object> requestBody = buildClaudeTextRequest(rawText);

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
            return parseClaudeResponse(receiptId, responseText);

        } catch (RestClientException e) {
            logger.error("Failed to call Claude API for text analysis: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to analyze PDF text with Claude API: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error during PDF text analysis: {}", e.getMessage(), e);
            throw new RuntimeException("Unexpected error during PDF text analysis: " + e.getMessage(), e);
        }
    }

    public ReceiptAnalysisResult analyzeMultiPagePdf(Long receiptId, java.util.List<byte[]> pageImages) {
        if (anthropicApiKey == null || anthropicApiKey.trim().isEmpty()) {
            throw new IllegalStateException("ANTHROPIC_API_KEY is not configured");
        }

        try {
            logger.info("Analyzing multi-page PDF for receipt {} - {} pages", receiptId, pageImages.size());

            // Build content array with all pages as images + prompt
            java.util.List<Map<String, Object>> content = new java.util.ArrayList<>();

            // Add all page images
            for (int i = 0; i < pageImages.size(); i++) {
                String imageBase64 = Base64.getEncoder().encodeToString(pageImages.get(i));
                if (!imageBase64.matches("^[A-Za-z0-9+/]*={0,2}$")) {
                    throw new IllegalArgumentException("Invalid Base64 encoding for page " + (i + 1));
                }

                Map<String, Object> imageContent = new HashMap<>();
                imageContent.put("type", "image");
                Map<String, Object> imageData = new HashMap<>();
                imageData.put("type", "base64");
                imageData.put("media_type", "image/jpeg");
                imageData.put("data", imageBase64);
                imageContent.put("source", imageData);
                content.add(imageContent);
            }

            // Add the analysis prompt (after all images)
            String analysisPrompt = "PARSE THIS MULTI-PAGE DOCUMENT INTO STRUCTURED JSON.\n\n" +
                "This is a " + pageImages.size() + "-page document. You are seeing ALL pages.\n" +
                "Extract information from ALL pages - do not stop after the first page.\n\n" +
                "The document has TWO sections:\n" +
                "1. HEADER - Usually on the first page with summary/metadata information\n" +
                "2. ITEMS - Table or list of detailed line items/rows (may span multiple pages)\n\n" +
                "JSON STRUCTURE (REQUIRED):\n" +
                "{\n" +
                "  \"header\": {\n" +
                "    \"field_name\": \"value\",\n" +
                "    \"total_paid_amount\": \"$1,234.56\",\n" +
                "    \"cheque_date\": \"Jan 15 2026\"\n" +
                "  },\n" +
                "  \"items\": [\n" +
                "    {\"invoice_no\": \"ABC-123\", \"name\": \"John Doe\", \"service_date\": \"Jan 01 2026\", \"service_code\": \"1100546\", \"description\": \"Service Type\", \"units\": \"1.0\", \"rate\": \"$100.00\", \"amount\": \"$100.00\", \"explanation\": null}\n" +
                "  ]\n" +
                "}\n\n" +
                "CRITICAL INSTRUCTIONS:\n" +
                "1. Extract header information from page 1 (or first page with header data)\n" +
                "2. Extract ALL items/rows from ALL pages - go page by page and collect every row\n" +
                "3. Do NOT stop after the first page - continue reading pages 2, 3, etc.\n" +
                "4. Standardize column names: use lowercase with underscores\n" +
                "5. Format ALL currency/money values as strings: \"$1,234.56\"\n" +
                "6. Format ALL dates as readable text: \"Jan 15 2026\" (NOT ISO format)\n" +
                "7. Use null for truly missing/empty values\n" +
                "8. Return ONLY valid JSON (no markdown, no code blocks, no extra text)\n" +
                "9. Close all brackets and braces properly";

            Map<String, Object> textContent = new HashMap<>();
            textContent.put("type", "text");
            textContent.put("text", analysisPrompt);
            content.add(textContent);

            // Build message
            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", content);

            // Build request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "claude-opus-4-7");
            requestBody.put("max_tokens", 16384); // Increased from 8192 to accommodate full JSON response with all line items
            List<Map<String, Object>> messages = new ArrayList<>();
            messages.add(message);
            requestBody.put("messages", messages);

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
            return parseClaudeResponse(receiptId, responseText);

        } catch (RestClientException e) {
            logger.error("Failed to call Claude API for multi-page PDF analysis: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to analyze PDF with Claude API: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error during multi-page PDF analysis: {}", e.getMessage(), e);
            throw new RuntimeException("Unexpected error during PDF analysis: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> buildClaudeTextRequest(String rawText) {
        String textPrompt = "PARSE THIS DOCUMENT INTO STRUCTURED JSON.\n\n" +
            "The document has TWO sections:\n" +
            "1. HEADER - Top section with summary/metadata information\n" +
            "2. ITEMS - Table or list of detailed line items/rows\n\n" +
            "CRITICAL: This document may contain MANY rows (20–100+). " +
            "Extract EVERY SINGLE ROW as a separate object in the items[] array. " +
            "Do NOT aggregate, summarize, or skip any rows. Preserve each row individually.\n\n" +
            "JSON STRUCTURE (REQUIRED):\n" +
            "{\n" +
            "  \"header\": {\n" +
            "    \"field_name\": \"value\",\n" +
            "    \"total_paid_amount\": \"$1,234.56\",\n" +
            "    \"cheque_date\": \"Jan 15 2026\"\n" +
            "  },\n" +
            "  \"items\": [\n" +
            "    {\"invoice_no\": \"ABC-123\", \"name\": \"John Doe\", \"service_date\": \"Jan 01 2026\", " +
            "\"service_code\": \"1100546\", \"description\": \"Service Type\", \"units\": \"1.0\", " +
            "\"rate\": \"$100.00\", \"amount\": \"$100.00\", \"explanation\": null},\n" +
            "    ... (ALL rows)\n" +
            "  ]\n" +
            "}\n\n" +
            "EXTRACTION INSTRUCTIONS:\n" +
            "1. Extract ALL header fields from the top/summary section\n" +
            "2. Extract ALL items/rows — DO NOT STOP EARLY\n" +
            "3. Standardize column names: use lowercase with underscores\n" +
            "4. Format ALL currency/money values as strings: \"$1,234.56\"\n" +
            "5. Format ALL dates as readable text: \"Jan 15 2026\"\n" +
            "6. Keep all text values as strings\n" +
            "7. Use null for truly missing/empty values\n" +
            "8. Return ONLY valid JSON — no markdown, no code blocks, no extra text\n" +
            "9. Close all brackets and braces properly\n\n" +
            "DOCUMENT TEXT:\n" + rawText;

        Map<String, Object> textContent = new HashMap<>();
        textContent.put("type", "text");
        textContent.put("text", textPrompt);

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", List.of(textContent));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "claude-opus-4-7");
        requestBody.put("max_tokens", 16384); // Increased from 8192 to accommodate full JSON response with all line items
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(message);
        requestBody.put("messages", messages);

        return requestBody;
    }

    /**
     * Analyze multi-page PDF in batches to avoid token limits while extracting all items.
     * Processes 3 pages at a time, merges results by keeping header from first batch
     * and combining all items from all batches.
     */
    public ReceiptAnalysisResult analyzeMultiPagePdfInBatches(Long receiptId, java.util.List<byte[]> pageImages) {
        final int BATCH_SIZE = 3;
        logger.info("Analyzing {} pages in batches of {} for receipt {}", pageImages.size(), BATCH_SIZE, receiptId);

        Map<String, Object> mergedHeader = null;
        java.util.List<Map<String, Object>> mergedItems = new java.util.ArrayList<>();

        for (int i = 0; i < pageImages.size(); i += BATCH_SIZE) {
            int endIdx = Math.min(i + BATCH_SIZE, pageImages.size());
            java.util.List<byte[]> batch = pageImages.subList(i, endIdx);
            int batchNum = (i / BATCH_SIZE) + 1;
            int totalBatches = (pageImages.size() + BATCH_SIZE - 1) / BATCH_SIZE;

            logger.info("Processing batch {}/{} (pages {} to {})", batchNum, totalBatches, i + 1, endIdx);

            try {
                ReceiptAnalysisResult batchResult = analyzeMultiPagePdf(receiptId, batch);

                if (batchResult != null && batchResult.getRawJsonData() != null) {
                    Map<String, Object> batchData = batchResult.getRawJsonData();

                    // Keep header from first batch only
                    if (mergedHeader == null && batchData.containsKey("header")) {
                        mergedHeader = (Map<String, Object>) batchData.get("header");
                    }

                    // Merge all items from all batches
                    if (batchData.containsKey("items")) {
                        @SuppressWarnings("unchecked")
                        java.util.List<Map<String, Object>> batchItems =
                            (java.util.List<Map<String, Object>>) batchData.get("items");
                        if (batchItems != null) {
                            mergedItems.addAll(batchItems);
                            logger.debug("Added {} items from batch {}", batchItems.size(), batchNum);
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Error processing batch {}: {}", batchNum, e.getMessage());
                // Continue with next batch instead of failing completely
            }
        }

        logger.info("Batch processing complete for receipt {}. Total items extracted: {}", receiptId, mergedItems.size());

        // Build final result with merged data
        Map<String, Object> finalJson = new HashMap<>();
        if (mergedHeader != null) {
            finalJson.put("header", mergedHeader);
        }
        finalJson.put("items", mergedItems);

        ReceiptAnalysisResult result = new ReceiptAnalysisResult();
        result.setReceiptId(receiptId);
        result.setRawJsonData(finalJson);
        result.setDocumentType("WCB_REMITTANCE"); // Will be classified based on content
        return result;
    }

}
