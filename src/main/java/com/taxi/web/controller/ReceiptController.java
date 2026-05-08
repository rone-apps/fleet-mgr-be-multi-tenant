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
import com.taxi.domain.receipt.service.PdfTextExtractorService;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.io.IOException;
import java.io.ByteArrayInputStream;
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
    private final PdfTextExtractorService pdfTextExtractorService;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@smartfleets.ai}")
    private String mailFrom;

    @Value("${spring.mail.sender-name:Smart Fleets}")
    private String senderName;

    public ReceiptController(ReceiptRepository receiptRepository,
                            ReceiptAnalysisService receiptAnalysisService,
                            GeminiAnalysisService geminiAnalysisService,
                            ObjectMapper objectMapper,
                            ReceiptClassifierService receiptClassifierService,
                            ReceiptTypeConverterRegistry converterRegistry,
                            PdfTextExtractorService pdfTextExtractorService) {
        this.receiptRepository = receiptRepository;
        this.receiptAnalysisService = receiptAnalysisService;
        this.geminiAnalysisService = geminiAnalysisService;
        this.objectMapper = objectMapper;
        this.receiptClassifierService = receiptClassifierService;
        this.converterRegistry = converterRegistry;
        this.pdfTextExtractorService = pdfTextExtractorService;
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
            boolean isPdf = "application/pdf".equals(mimeType);
            boolean isImage = mimeType != null && mimeType.startsWith("image/");

            if (!isPdf && !isImage) {
                return ResponseEntity.badRequest().body(Map.of("error", "File must be an image or PDF"));
            }

            // Read file data
            byte[] fileData = imageFile.getBytes();
            int maxBytes = isPdf ? 30 * 1024 * 1024 : 10 * 1024 * 1024;  // 30 MB for PDF, 10 MB for images
            if (fileData.length > maxBytes) {
                String limitLabel = isPdf ? "30MB" : "10MB";
                return ResponseEntity.badRequest().body(Map.of("error", "File too large (max " + limitLabel + ")"));
            }

            // Validate model choice
            model = model.toLowerCase().trim();
            if (!model.equals("claude") && !model.equals("gemini")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Model must be 'claude' or 'gemini'"));
            }

            // Create pending receipt record
            // IMPORTANT: Store original file data (PDF or image) - never overwrite with rendered JPEGs
            Receipt receipt = new Receipt();
            receipt.setImageData(fileData); // Original PDF/image binary data
            receipt.setImageMimeType(mimeType); // Original MIME type (application/pdf or image/*)
            receipt.setStatus("PENDING");
            Receipt savedReceipt = receiptRepository.save(receipt);

            logger.info("Receipt {} created: mimeType={}, dataSize={}MB",
                savedReceipt.getId(), mimeType, fileData.length / (1024*1024));

            // Call AI API to analyze (Claude or Gemini, or PDF text-mode)
            ReceiptAnalysisResult analysisResult;
            String usedModel = model;

            if (isPdf) {
                // PDF path: always use Claude text-mode (or multi-page image fallback for scanned PDFs)
                usedModel = "claude";
                try {
                    String extractedText = pdfTextExtractorService.extract(fileData);
                    if (extractedText.length() >= PdfTextExtractorService.MIN_TEXT_LENGTH) {
                        logger.info("PDF text mode: {} chars extracted for receipt {}", extractedText.length(), savedReceipt.getId());
                        analysisResult = receiptAnalysisService.analyzeText(savedReceipt.getId(), extractedText);
                    } else {
                        // Scanned PDF: render ALL pages and analyze multi-page
                        logger.info("PDF image fallback (multi-page scanned PDF) for receipt {}", savedReceipt.getId());
                        java.util.List<byte[]> pageImages = pdfTextExtractorService.renderAllPages(fileData);
                        if (pageImages.isEmpty()) {
                            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(Map.of("error", "Could not extract pages from PDF"));
                        }
                        analysisResult = receiptAnalysisService.analyzeMultiPagePdfInBatches(savedReceipt.getId(), pageImages);
                    }

                    // CRITICAL FIX: Replace PDF bytes with rendered JPEG for display
                    // PDFs cannot be displayed in <img> tags, so we render the first page as JPEG
                    logger.info("Converting PDF to JPEG for display purposes (receipt {})", savedReceipt.getId());
                    byte[] renderedJpeg = pdfTextExtractorService.renderFirstPage(fileData);
                    savedReceipt.setImageData(renderedJpeg);
                    savedReceipt.setImageMimeType("image/jpeg");
                    receiptRepository.save(savedReceipt);
                    logger.info("✅ PDF converted to JPEG: {} bytes (receipt {})", renderedJpeg.length, savedReceipt.getId());

                } catch (IOException e) {
                    logger.error("PDF extraction failed for receipt {}: {}", savedReceipt.getId(), e.getMessage(), e);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Could not read PDF file: " + e.getMessage()));
                }
            } else if ("gemini".equals(model)) {
                try {
                    logger.info("Analyzing receipt with Gemini: {}", savedReceipt.getId());
                    analysisResult = geminiAnalysisService.analyzeReceipt(
                        savedReceipt.getId(),
                        fileData,
                        mimeType
                    );
                } catch (RuntimeException e) {
                    if (fallbackToClaude && e.getMessage() != null && e.getMessage().contains("429")) {
                        logger.warn("Gemini quota exceeded, falling back to Claude for receipt: {}", savedReceipt.getId());
                        usedModel = "claude";
                        analysisResult = receiptAnalysisService.analyzeReceipt(
                            savedReceipt.getId(),
                            fileData,
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
                    fileData,
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

            // Pre-validate for type-specific fields BEFORE saving
            Map<String, Object> parsedJsonMap = null;
            if (receipt.getParsedDataJson() != null) {
                try {
                    parsedJsonMap = objectMapper.readValue(receipt.getParsedDataJson(), new TypeReference<>() {});
                } catch (Exception e) {
                    logger.warn("Could not deserialize parsedDataJson for receipt {}", request.getReceiptId());
                }
            }

            ReceiptType confirmedType = ReceiptType.fromString(request.getDocumentType());
            java.util.Optional<com.taxi.domain.receipt.converter.ReceiptTypeConverter> converterOpt = converterRegistry.getConverter(confirmedType);

            if (converterOpt.isPresent()) {
                java.util.List<String> validationErrors = converterOpt.get().validate(parsedJsonMap);
                if (!validationErrors.isEmpty()) {
                    return ResponseEntity.unprocessableEntity().body(Map.of("errors", validationErrors));
                }
            }

            // Update receipt with confirmed data
            receipt.setReceiptType(request.getDocumentType());
            receipt.setShiftType(request.getShiftType());
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

            // Capture the user who updated this receipt
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                receipt.setUpdatedBy(auth.getName());
            }

            Receipt updatedReceipt = receiptRepository.save(receipt);

            // Run type-specific converter (Step 2 of the two-step flow)
            final Map<String, Object> finalParsedJson = parsedJsonMap;
            Map<String, Object> conversionDetails = null;

            if (converterOpt.isPresent()) {
                try {
                    com.taxi.domain.receipt.converter.ReceiptTypeConverter converter = converterOpt.get();
                    converter.convert(updatedReceipt, finalParsedJson, request);
                    logger.info("Converter ran for receipt {} type {}", updatedReceipt.getId(), confirmedType);

                    // If WCB converter, get detailed processing result
                    if (converter instanceof com.taxi.domain.receipt.converter.WcbRemittanceReceiptConverter) {
                        com.taxi.domain.wcb.dto.WcbProcessingResult result =
                            ((com.taxi.domain.receipt.converter.WcbRemittanceReceiptConverter) converter).getLastProcessingResult();
                        conversionDetails = result.toMap();
                    }
                } catch (Exception e) {
                    logger.error("Converter failed for receipt {}: {}", updatedReceipt.getId(), e.getMessage(), e);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Failed to process receipt: " + e.getMessage()));
                }
            }

            logger.info("Receipt confirmed: {}", updatedReceipt.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("id", updatedReceipt.getId());
            response.put("status", updatedReceipt.getStatus());
            if (conversionDetails != null) {
                response.put("conversionDetails", conversionDetails);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error confirming receipt", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to confirm receipt: " + e.getMessage()));
        }
    }

    @PostMapping("/send-email")
    public ResponseEntity<?> sendReceiptEmail(@RequestBody Map<String, String> request) {
        try {
            String recipientEmail = request.get("recipientEmail");
            String subject = request.get("subject");
            String message = request.get("message");
            String receiptId = request.get("receiptId");
            String imageData = request.get("imageData");

            if (recipientEmail == null || recipientEmail.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Recipient email is required"));
            }

            if (mailSender == null) {
                logger.warn("Email service not configured");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Email service not configured"));
            }

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            helper.setTo(recipientEmail);
            helper.setFrom(mailFrom, senderName);
            helper.setSubject(subject != null ? subject : "Receipt");

            StringBuilder body = new StringBuilder();
            body.append("Receipt ID: ").append(receiptId).append("\n\n");
            if (message != null && !message.isEmpty()) {
                body.append("Note:\n").append(message).append("\n\n");
            }
            body.append("Sent from Receipt Scanner");

            helper.setText(body.toString());

            // Attach image if provided
            if (imageData != null && !imageData.isEmpty()) {
                try {
                    // Extract base64 data (remove data:image/...;base64, prefix if present)
                    String base64Data = imageData;
                    if (base64Data.contains(",")) {
                        base64Data = base64Data.split(",")[1];
                    }

                    byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Data);
                    String filename = "receipt-" + receiptId + ".jpg";

                    helper.addAttachment(filename, new org.springframework.core.io.ByteArrayResource(imageBytes), "image/jpeg");
                    logger.debug("Attached image to email: {}", filename);
                } catch (Exception e) {
                    logger.warn("Failed to attach image to email: {}", e.getMessage());
                    // Continue sending email without attachment
                }
            }

            mailSender.send(mimeMessage);

            logger.info("Email sent successfully to {} for receipt {}", recipientEmail, receiptId);
            return ResponseEntity.ok(Map.of("message", "Email sent successfully"));

        } catch (MessagingException e) {
            logger.error("Messaging error sending email: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to send email: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Error sending email: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to send email: " + e.getMessage()));
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
                    // Filter by owner ID, BUT also include receipts with null owner (unassigned)
                    predicates.add(cb.or(
                        cb.isNull(root.get("owner")),
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
                        map.put("shiftType", receipt.getShiftType());
                        map.put("status", receipt.getStatus());

                        // Safely get cab info
                        String cabNumber = null;
                        Long cabIdValue = null;
                        try {
                            if (receipt.getCab() != null) {
                                cabNumber = receipt.getCab().getCabNumber();
                                cabIdValue = receipt.getCab().getId();
                            }
                        } catch (Exception e) {
                            logger.debug("Error loading cab for receipt {}: {}", receipt.getId(), e.getMessage());
                        }
                        map.put("cabNumber", cabNumber);
                        map.put("cabId", cabIdValue);

                        // Safely get owner/driver info
                        String ownerName = null;
                        Long ownerIdValue = null;
                        try {
                            if (receipt.getOwner() != null) {
                                ownerName = receipt.getOwner().getFirstName() + " " + receipt.getOwner().getLastName();
                                ownerIdValue = receipt.getOwner().getId();
                            }
                        } catch (Exception e) {
                            logger.debug("Error loading owner for receipt {}: {}", receipt.getId(), e.getMessage());
                        }
                        map.put("ownerName", ownerName);
                        map.put("ownerId", ownerIdValue);

                        map.put("createdAt", receipt.getCreatedAt());
                        map.put("updatedAt", receipt.getUpdatedAt());
                        map.put("updatedBy", receipt.getUpdatedBy());

                        // Safely encode image data
                        if (receipt.getImageData() != null && receipt.getImageData().length > 0) {
                            try {
                                int dataSize = receipt.getImageData().length;
                                String mimeType = receipt.getImageMimeType() != null ? receipt.getImageMimeType() : "image/jpeg";
                                String imageDataBase64 = "data:" + mimeType + ";base64," +
                                    java.util.Base64.getEncoder().encodeToString(receipt.getImageData());
                                map.put("imageData", imageDataBase64);
                                logger.debug("✅ Image encoded for receipt {}: {} bytes, mimeType: {}",
                                    receipt.getId(), dataSize, mimeType);
                            } catch (Exception e) {
                                logger.warn("❌ Error encoding image for receipt {}: {}", receipt.getId(), e.getMessage());
                                map.put("imageData", null);
                            }
                        } else {
                            if (receipt.getImageData() == null) {
                                logger.warn("⚠️ Receipt {} has NULL imageData in database", receipt.getId());
                            } else {
                                logger.warn("⚠️ Receipt {} has EMPTY imageData (0 bytes)", receipt.getId());
                            }
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
                        map.put("updatedBy", receipt.getUpdatedBy());
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
