package com.taxi.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxi.domain.receipt.dto.LineItem;
import com.taxi.domain.receipt.dto.ReceiptAnalysisResult;
import com.taxi.domain.receipt.model.Receipt;
import com.taxi.domain.receipt.repository.ReceiptRepository;
import com.taxi.domain.receipt.service.ReceiptAnalysisService;
import com.taxi.web.dto.receipt.ConfirmReceiptRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/receipts")
public class ReceiptController {
    private static final Logger logger = LoggerFactory.getLogger(ReceiptController.class);

    private final ReceiptRepository receiptRepository;
    private final ReceiptAnalysisService receiptAnalysisService;
    private final ObjectMapper objectMapper;

    public ReceiptController(ReceiptRepository receiptRepository,
                            ReceiptAnalysisService receiptAnalysisService,
                            ObjectMapper objectMapper) {
        this.receiptRepository = receiptRepository;
        this.receiptAnalysisService = receiptAnalysisService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeReceipt(@RequestParam("image") MultipartFile imageFile) {
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

            // Create pending receipt record
            Receipt receipt = new Receipt();
            receipt.setImageData(imageData);
            receipt.setImageMimeType(mimeType);
            receipt.setStatus("PENDING");
            receipt.setTotalAmount(BigDecimal.ZERO);
            receipt.setTaxAmount(BigDecimal.ZERO);
            Receipt savedReceipt = receiptRepository.save(receipt);

            // Call Claude API to analyze
            ReceiptAnalysisResult analysisResult = receiptAnalysisService.analyzeReceipt(
                savedReceipt.getId(),
                imageData,
                mimeType
            );

            // Store raw Claude response for debugging
            savedReceipt.setRawClaudeResponse(objectMapper.writeValueAsString(analysisResult));
            receiptRepository.save(savedReceipt);

            logger.info("Receipt analyzed successfully: {}", savedReceipt.getId());
            return ResponseEntity.ok(analysisResult);

        } catch (IOException e) {
            logger.error("IO error while reading image file", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Failed to read image file: " + e.getMessage()));
        } catch (IllegalArgumentException e) {
            logger.error("Unsupported image format", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            logger.error("Configuration error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error analyzing receipt", e);
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

            // Update receipt with confirmed data
            receipt.setDocumentType(request.getDocumentType());
            receipt.setVendorName(request.getVendorName());
            receipt.setReceiptDate(request.getReceiptDate());
            receipt.setTaxAmount(request.getTaxAmount());
            receipt.setTotalAmount(request.getTotalAmount());
            receipt.setStatus("CONFIRMED");

            // Serialize line items to JSON
            if (request.getLineItems() != null) {
                String lineItemsJson = objectMapper.writeValueAsString(request.getLineItems());
                receipt.setLineItemsJson(lineItemsJson);
            }

            Receipt updatedReceipt = receiptRepository.save(receipt);

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
}
