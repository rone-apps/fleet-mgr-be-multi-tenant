package com.taxi.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxi.domain.account.repository.AccountCustomerRepository;
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
    private final ObjectMapper objectMapper;
    private final AccountCustomerRepository accountCustomerRepository;

    public ReceiptController(ReceiptRepository receiptRepository,
                            ReceiptAnalysisService receiptAnalysisService,
                            ObjectMapper objectMapper,
                            AccountCustomerRepository accountCustomerRepository) {
        this.receiptRepository = receiptRepository;
        this.receiptAnalysisService = receiptAnalysisService;
        this.objectMapper = objectMapper;
        this.accountCustomerRepository = accountCustomerRepository;
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
            receipt.setNotes(request.getNotes());
            receipt.setAccountCustomerId(request.getAccountCustomerId());
            receipt.setShiftType(request.getShiftType());
            receipt.setStatus("CONFIRMED");

            // Serialize line items to JSON
            if (request.getLineItems() != null) {
                String lineItemsJson = objectMapper.writeValueAsString(request.getLineItems());
                receipt.setLineItemsJson(lineItemsJson);
            }

            // Set cab and owner if provided (these are optional)
            if (request.getCabId() != null) {
                // Note: We're setting the ID only; the entity will be lazy-loaded if needed
                receipt.setCab(new com.taxi.domain.cab.model.Cab());
                receipt.getCab().setId(request.getCabId());
            }

            if (request.getOwnerId() != null) {
                // Note: We're setting the ID only; the entity will be lazy-loaded if needed
                receipt.setOwner(new com.taxi.domain.driver.model.Driver());
                receipt.getOwner().setId(request.getOwnerId());
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
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

            Specification<Receipt> spec = (root, query, cb) -> {
                var predicates = new java.util.ArrayList<>();

                if (startDate != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("receiptDate"), startDate));
                }
                if (endDate != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("receiptDate"), endDate));
                }
                if (cabId != null) {
                    predicates.add(cb.equal(root.get("cab").get("id"), cabId));
                }
                if (shiftId != null) {
                    predicates.add(cb.equal(root.get("shift").get("id"), shiftId));
                }
                if (ownerId != null) {
                    predicates.add(cb.equal(root.get("owner").get("id"), ownerId));
                }
                if (vendorName != null && !vendorName.isEmpty()) {
                    predicates.add(cb.like(cb.lower(root.get("vendorName")), "%" + vendorName.toLowerCase() + "%"));
                }
                if (documentType != null && !documentType.isEmpty()) {
                    predicates.add(cb.equal(root.get("documentType"), documentType));
                }

                return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
            };

            Page<Receipt> receipts = receiptRepository.findAll(spec, pageable);

            List<Map<String, Object>> response = receipts.getContent().stream()
                .map(receipt -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", receipt.getId());
                    map.put("vendorName", receipt.getVendorName());
                    map.put("totalAmount", receipt.getTotalAmount());
                    map.put("taxAmount", receipt.getTaxAmount());
                    map.put("receiptDate", receipt.getReceiptDate());
                    map.put("documentType", receipt.getDocumentType());
                    map.put("status", receipt.getStatus());
                    map.put("cabNumber", receipt.getCab() != null ? receipt.getCab().getCabNumber() : null);
                    map.put("driverNumber", receipt.getShift() != null ? receipt.getShift().getDriverNumber() : null);
                    map.put("ownerName", receipt.getOwner() != null ?
                        receipt.getOwner().getFirstName() + " " + receipt.getOwner().getLastName() : null);
                    map.put("accountCustomerId", receipt.getAccountCustomerId());
                    String accountName = null;
                    if (receipt.getAccountCustomerId() != null) {
                        accountName = accountCustomerRepository.findById(receipt.getAccountCustomerId())
                            .map(ac -> ac.getCompanyName())
                            .orElse(null);
                    }
                    map.put("accountName", accountName);
                    map.put("shiftType", receipt.getShiftType());
                    map.put("createdAt", receipt.getCreatedAt());
                    if (receipt.getImageData() != null) {
                        String imageDataBase64 = "data:" + receipt.getImageMimeType() + ";base64," +
                            java.util.Base64.getEncoder().encodeToString(receipt.getImageData());
                        map.put("imageData", imageDataBase64);
                    } else {
                        map.put("imageData", null);
                    }
                    return map;
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
