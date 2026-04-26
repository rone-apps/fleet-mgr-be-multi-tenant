package com.taxi.web.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxi.domain.receipt.converter.ReceiptTypeConverterRegistry;
import com.taxi.domain.receipt.dto.LineItem;
import com.taxi.domain.receipt.dto.ReceiptAnalysisResult;
import com.taxi.domain.receipt.model.Receipt;
import com.taxi.domain.receipt.model.ReceiptType;
import com.taxi.domain.receipt.repository.ReceiptRepository;
import com.taxi.domain.receipt.service.GeminiAnalysisService;
import com.taxi.domain.receipt.service.ReceiptAnalysisService;
import com.taxi.domain.receipt.service.ReceiptClassifierService;
import com.taxi.web.dto.receipt.ConfirmReceiptRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/receipts")
public class ReceiptController {
    private static final Logger logger = LoggerFactory.getLogger(ReceiptController.class);

    private final ReceiptRepository receiptRepository;
    private final ReceiptAnalysisService receiptAnalysisService;
    private final GeminiAnalysisService geminiAnalysisService;
    private final ObjectMapper objectMapper;
    private final ReceiptClassifierService receiptClassifierService;
    private final ReceiptTypeConverterRegistry converterRegistry;

    public ReceiptController(ReceiptRepository receiptRepository,
                            ReceiptAnalysisService receiptAnalysisService,
                            GeminiAnalysisService geminiAnalysisService,
                            ObjectMapper objectMapper,
                            ReceiptClassifierService receiptClassifierService,
                            ReceiptTypeConverterRegistry converterRegistry) {
        this.receiptRepository = receiptRepository;
        this.receiptAnalysisService = receiptAnalysisService;
        this.geminiAnalysisService = geminiAnalysisService;
        this.objectMapper = objectMapper;
        this.receiptClassifierService = receiptClassifierService;
        this.converterRegistry = converterRegistry;
    }

    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeReceipt(@RequestParam("image") MultipartFile imageFile,
                                           @RequestParam(value = "model", defaultValue = "claude") String model,
                                           @RequestParam(value = "fallbackToClaude", defaultValue = "true") boolean fallbackToClaude) {
        try {
            // Validate file
            if (imageFile.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Image file is required"));
            }

            String mimeType = imageFile.getContentType();
            if (mimeType == null || !mimeType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(Map.of("error", "File must be an image"));
            }

            // Read image data
            byte[] imageData = imageFile.getBytes();
            if (imageData.length > 10 * 1024 * 1024) { // 10MB limit
                return ResponseEntity.badRequest().body(Map.of("error", "Image file too large (max 10MB)"));
            }

            // Validate model choice
            model = model.toLowerCase().trim();
            if (!model.equals("claude") && !model.equals("gemini")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Model must be 'claude' or 'gemini'"));
            }

            // Create pending receipt record
            Receipt receipt = new Receipt();
            receipt.setImageData(imageData);
            receipt.setImageMimeType(mimeType);
            receipt.setStatus("PENDING");
            Receipt savedReceipt = receiptRepository.save(receipt);

            // Call AI API to analyze (Claude or Gemini)
            ReceiptAnalysisResult analysisResult;
            String usedModel = model;

            if ("gemini".equals(model)) {
                try {
                    logger.info("Analyzing receipt with Gemini: {}", savedReceipt.getId());
                    analysisResult = geminiAnalysisService.analyzeReceipt(
                        savedReceipt.getId(),
                        imageData,
                        mimeType
                    );
                } catch (RuntimeException e) {
                    if (fallbackToClaude && e.getMessage() != null && e.getMessage().contains("429")) {
                        logger.warn("Gemini quota exceeded, falling back to Claude for receipt: {}", savedReceipt.getId());
                        usedModel = "claude";
                        analysisResult = receiptAnalysisService.analyzeReceipt(
                            savedReceipt.getId(),
                            imageData,
                            mimeType
                        );
                    } else {
                        throw e;
                    }
                }
            } else {
                logger.info("Analyzing receipt with Claude: {}", savedReceipt.getId());
                analysisResult = receiptAnalysisService.analyzeReceipt(
                    savedReceipt.getId(),
                    imageData,
                    mimeType
                );
            }

            // Classify receipt type and store parsed JSON
            logger.info("📊 Before classification - rawJsonData: {}, documentType: {}",
                analysisResult.getRawJsonData() != null ? "✓ present" : "✗ null",
                analysisResult.getDocumentType());

            ReceiptType detectedType = receiptClassifierService.classify(
                analysisResult.getDocumentType(),
                analysisResult.getRawJsonData()
            );
            analysisResult.setClassifiedType(detectedType.name());

            logger.info("✅ Classification result: {} (raw JSON size: {} bytes)",
                detectedType.name(),
                analysisResult.getRawJsonData() != null ? objectMapper.writeValueAsString(analysisResult.getRawJsonData()).length() : 0);

            if (analysisResult.getRawJsonData() != null) {
                String jsonStr = objectMapper.writeValueAsString(analysisResult.getRawJsonData());
                savedReceipt.setParsedDataJson(jsonStr);
                logger.info("💾 Stored parsed JSON: {} bytes", jsonStr.length());
            }
            savedReceipt.setReceiptType(detectedType.name());
            receiptRepository.save(savedReceipt);

            logger.info("📤 Returning analysis result with classifiedType: {}", analysisResult.getClassifiedType());

            logger.info("Receipt analyzed successfully with {}: {}", usedModel.toUpperCase(), savedReceipt.getId());
            return ResponseEntity.ok(analysisResult);

        } catch (IOException e) {
            logger.error("IO error while reading image file: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Failed to read image file: " + e.getMessage()));
        } catch (IllegalArgumentException e) {
            logger.error("Image validation error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            logger.error("Configuration error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            // Handle Claude API errors like "string did not match expected pattern"
            if (e.getMessage() != null && e.getMessage().contains("pattern")) {
                logger.error("Claude API rejected image (pattern mismatch): {}", e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Image format issue - try retaking the photo or using a different device"));
            }
            logger.error("Error analyzing receipt: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to analyze receipt: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error analyzing receipt: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to analyze receipt: " + e.getMessage()));
        }
    }

    @PostMapping("/confirm")
    public ResponseEntity<?> confirmReceipt(@RequestBody ConfirmReceiptRequest request) {
        try {
            // Validate request
            if (request.getReceiptId() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Receipt ID is required"));
            }

            // Fetch receipt
            Receipt receipt = receiptRepository.findById(request.getReceiptId())
                .orElse(null);

            if (receipt == null) {
                return ResponseEntity.notFound().build();
            }

            // Update receipt with confirmed data (minimal fields only)
            receipt.setReceiptType(request.getDocumentType());
            receipt.setStatus("CONFIRMED");

            // Set cab and owner if provided
            if (request.getCabId() != null) {
                receipt.setCab(new com.taxi.domain.cab.model.Cab());
                receipt.getCab().setId(request.getCabId());
            }

            if (request.getOwnerId() != null) {
                receipt.setOwner(new com.taxi.domain.driver.model.Driver());
                receipt.getOwner().setId(request.getOwnerId());
            }

            Receipt updatedReceipt = receiptRepository.save(receipt);

            // Run type-specific converter (Step 2 of the two-step flow)
            ReceiptType type = ReceiptType.fromString(updatedReceipt.getReceiptType());
            converterRegistry.getConverter(type).ifPresent(converter -> {
                try {
                    Map<String, Object> parsedJson = null;
                    if (updatedReceipt.getParsedDataJson() != null) {
                        parsedJson = objectMapper.readValue(updatedReceipt.getParsedDataJson(), new TypeReference<Map<String, Object>>() {});
                    }
                    converter.convert(updatedReceipt, parsedJson, request);
                    logger.info("Converter ran for receipt {} type {}", updatedReceipt.getId(), type);
                } catch (Exception e) {
                    logger.error("Converter failed for receipt {}: {}", updatedReceipt.getId(), e.getMessage(), e);
                }
            });

            logger.info("Receipt confirmed: {}", updatedReceipt.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("id", updatedReceipt.getId());
            response.put("status", updatedReceipt.getStatus());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error confirming receipt", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to confirm receipt: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getReceipts(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) Long cabId,
            @RequestParam(required = false) Long shiftId,
            @RequestParam(required = false) Long ownerId,
            @RequestParam(required = false) String vendorName,
            @RequestParam(required = false) String documentType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            logger.info("GET /api/receipts - Filters: ownerId={}, documentType={}",
                ownerId, documentType);

            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

            Specification<Receipt> spec = (root, query, cb) -> {
                var predicates = new java.util.ArrayList<>();

                if (cabId != null) {
                    predicates.add(cb.equal(root.get("cab").get("id"), cabId));
                }
                if (ownerId != null) {
                    // Filter by owner ID, excluding null owners
                    predicates.add(cb.and(
                        cb.isNotNull(root.get("owner")),
                        cb.equal(root.get("owner").get("id"), ownerId)
                    ));
                }
                if (documentType != null && !documentType.isEmpty()) {
                    predicates.add(cb.equal(root.get("receiptType"), documentType));
                }

                return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
            };

            Page<Receipt> receipts = receiptRepository.findAll(spec, pageable);

            logger.info("Receipts query result: {} receipts found (page {} of {}, total: {})",
                receipts.getContent().size(), page, receipts.getTotalPages(), receipts.getTotalElements());

            if (ownerId != null) {
                // Log details about owner filtering
                long totalReceipts = receiptRepository.count();
                long reciptsWithOwner = receipts.getTotalElements();
                logger.info("Owner filter ownerId={}: {} receipts found with this owner (total receipts: {})",
                    ownerId, reciptsWithOwner, totalReceipts);

                // Log the first few receipts' owner info for debugging
                for (Receipt r : receipts.getContent()) {
                    if (r.getOwner() != null) {
                        logger.debug("Receipt {}: owner.id={}, owner.name={}", r.getId(), r.getOwner().getId(), r.getOwner().getFirstName());
                    } else {
                        logger.debug("Receipt {}: owner is NULL", r.getId());
                    }
                }
            }

            List<Map<String, Object>> response = receipts.getContent().stream()
                .map(receipt -> {
                    try {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", receipt.getId());
                        map.put("receiptType", receipt.getReceiptType());
                        map.put("status", receipt.getStatus());

                        // Safely get cab number
                        String cabNumber = null;
                        try {
                            if (receipt.getCab() != null) {
                                cabNumber = receipt.getCab().getCabNumber();
                            }
                        } catch (Exception e) {
                            logger.debug("Error loading cab for receipt {}: {}", receipt.getId(), e.getMessage());
                        }
                        map.put("cabNumber", cabNumber);

                        // Safely get owner name
                        String ownerName = null;
                        try {
                            if (receipt.getOwner() != null) {
                                ownerName = receipt.getOwner().getFirstName() + " " + receipt.getOwner().getLastName();
                            }
                        } catch (Exception e) {
                            logger.debug("Error loading owner for receipt {}: {}", receipt.getId(), e.getMessage());
                        }
                        map.put("ownerName", ownerName);

                        map.put("createdAt", receipt.getCreatedAt());

                        // Safely encode image data
                        if (receipt.getImageData() != null) {
                            try {
                                String imageDataBase64 = "data:" + receipt.getImageMimeType() + ";base64," +
                                    java.util.Base64.getEncoder().encodeToString(receipt.getImageData());
                                map.put("imageData", imageDataBase64);
                            } catch (Exception e) {
                                logger.debug("Error encoding image for receipt {}: {}", receipt.getId(), e.getMessage());
                                map.put("imageData", null);
                            }
                        } else {
                            map.put("imageData", null);
                        }
                        return map;
                    } catch (Exception e) {
                        logger.error("Error mapping receipt {}: {}", receipt.getId(), e.getMessage());
                        // Return minimal response for this receipt
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", receipt.getId());
                        map.put("receiptType", receipt.getReceiptType());
                        map.put("status", receipt.getStatus());
                        return map;
                    }
                })
                .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                "content", response,
                "totalElements", receipts.getTotalElements(),
                "totalPages", receipts.getTotalPages(),
                "currentPage", page,
                "pageSize", size
            ));

        } catch (Exception e) {
            logger.error("Error fetching receipts", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to fetch receipts: " + e.getMessage()));
        }
    }
}
