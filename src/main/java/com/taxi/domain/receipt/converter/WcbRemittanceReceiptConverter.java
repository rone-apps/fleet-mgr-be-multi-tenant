package com.taxi.domain.receipt.converter;

import com.taxi.domain.receipt.model.Receipt;
import com.taxi.domain.receipt.model.ReceiptType;
import com.taxi.domain.wcb.dto.WcbProcessingResult;
import com.taxi.domain.wcb.model.WcbRemittance;
import com.taxi.domain.wcb.model.WcbRemittanceLine;
import com.taxi.domain.wcb.repository.WcbRemittanceLineRepository;
import com.taxi.domain.wcb.repository.WcbRemittanceRepository;
import com.taxi.web.dto.receipt.ConfirmReceiptRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class WcbRemittanceReceiptConverter implements ReceiptTypeConverter {

    private static final Logger logger = LoggerFactory.getLogger(WcbRemittanceReceiptConverter.class);

    private final WcbRemittanceRepository remittanceRepository;
    private final WcbRemittanceLineRepository lineRepository;
    private WcbProcessingResult lastProcessingResult; // Store result for retrieval

    public WcbRemittanceReceiptConverter(WcbRemittanceRepository remittanceRepository,
                                       WcbRemittanceLineRepository lineRepository) {
        this.remittanceRepository = remittanceRepository;
        this.lineRepository = lineRepository;
    }

    @Override
    public ReceiptType getSupportedType() {
        return ReceiptType.WCB_REMITTANCE;
    }

    @Override
    public List<String> validate(Map<String, Object> parsedJson) {
        List<String> errors = new ArrayList<>();

        if (parsedJson == null) {
            errors.add("No parsed data found. Please re-scan the document.");
            return errors;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> header = (Map<String, Object>) parsedJson.get("header");
        if (header == null || header.isEmpty()) {
            errors.add("Document header could not be extracted. Please re-scan.");
            return errors;
        }

        // At least one payee identifier required
        boolean hasPayeeId = getStr(header, "payee_name", "payee", "company_name") != null
                          || getStr(header, "payee_number", "payee_no", "vendor_number") != null;
        if (!hasPayeeId) {
            errors.add("Could not find payee name or payee number in document header.");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) parsedJson.get("items");
        if (items == null || items.isEmpty()) {
            errors.add("No payment lines found in document. Ensure the document contains a table of payment items.");
        }

        return errors;
    }

    @Override
    public void convert(Receipt receipt, Map<String, Object> parsedJson, ConfirmReceiptRequest request) {
        lastProcessingResult = new WcbProcessingResult();

        if (parsedJson == null) {
            logger.warn("No parsed JSON for WCB remittance receipt {}", receipt.getId());
            lastProcessingResult.setStatus("FAILED");
            lastProcessingResult.getSummary().status = "REJECTED";
            lastProcessingResult.getSummary().reason = "No parsed data found";
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> header = (Map<String, Object>) parsedJson.get("header");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) parsedJson.get("items");

        // Extract key fields
        String payeeNumber = getStr(header, "payee_number", "payee_no", "vendor_number");
        String chequeNumber = getStr(header, "cheque_number", "chq_number", "chq_no", "cheque_no");

        // Validate required fields for uniqueness
        if (payeeNumber == null || chequeNumber == null) {
            logger.warn("Missing payee_number or cheque_number for receipt {}", receipt.getId());
            lastProcessingResult.setStatus("FAILED");
            lastProcessingResult.getSummary().status = "REJECTED";
            lastProcessingResult.getSummary().reason = "Missing payee number or cheque number — cannot process WCB remittance";
            return;
        }

        // Check for duplicate summary - all-or-nothing: if exists, reject everything
        java.util.List<WcbRemittance> existing =
            remittanceRepository.findByPayeeNumberAndChequeNumber(payeeNumber, chequeNumber);
        if (!existing.isEmpty()) {
            WcbRemittance dup = existing.get(0);
            logger.warn("Duplicate WCB summary: payeeNumber={}, chequeNumber={}, existingReceiptId={}",
                payeeNumber, chequeNumber, dup.getReceiptId());
            lastProcessingResult.setStatus("DUPLICATE_SUMMARY");
            lastProcessingResult.getSummary().status = "REJECTED";
            lastProcessingResult.getSummary().reason =
                "A payment for payee " + payeeNumber + ", cheque " + chequeNumber +
                " already exists. Delete the existing entry and re-upload.";
            lastProcessingResult.getSummary().existingReceiptId = dup.getReceiptId();
            return;
        }

        // No duplicate: create new remittance
        WcbRemittance remittance = new WcbRemittance();
        remittance.setReceiptId(receipt.getId());
        remittance.setPayeeName(getStr(header, "payee_name", "payee", "company_name"));
        remittance.setPayeeNumber(payeeNumber);
        remittance.setChequeDate(parseDate(getStr(header, "cheque_date", "chq_date", "payment_date", "date")));
        remittance.setChequeNumber(chequeNumber);
        remittance.setTotalAmount(parseAmount(getStr(header, "total_amount", "total_paid_amount", "total", "amount")));
        String currency = getStr(header, "currency");
        remittance.setCurrency(currency != null ? currency : "CAD");

        remittance = remittanceRepository.save(remittance);
        logger.info("✅ Created new WCB remittance {} for receipt {}", remittance.getId(), receipt.getId());
        lastProcessingResult.getSummary().status = "SAVED";
        lastProcessingResult.getSummary().reason = "New summary created";
        lastProcessingResult.setRemittanceId(remittance.getId());
        lastProcessingResult.setPayeeName(remittance.getPayeeName());
        lastProcessingResult.setTotalAmount(remittance.getTotalAmount());

        // Process line items - save new ones, reject duplicates
        if (items != null && !items.isEmpty()) {
            lastProcessingResult.getLineItems().totalProcessed = items.size();

            for (Map<String, Object> item : items) {
                String invoiceNo      = getStr(item, "invoice_no", "invoice_number");
                String claimNumber    = getStr(item, "claim_no", "claim_number", "claim");
                String customerName   = getStr(item, "name", "customer_name", "patient_name");
                LocalDate serviceDate = parseDate(getStr(item, "service_date", "date"));
                String serviceCode    = getStr(item, "service_code", "service", "code");
                BigDecimal invoiceAmt = parseAmount(getStr(item, "invoice_amount", "invoice_amt"));
                String unitDesc       = getStr(item, "unit_description", "units", "description");
                BigDecimal rate       = parseAmount(getStr(item, "rate"));
                BigDecimal amount     = parseAmount(getStr(item, "amount"));
                String explanation    = getStr(item, "explanation", "notes");

                String hash = computeContentHash(
                    claimNumber, invoiceNo, customerName, serviceDate, serviceCode,
                    invoiceAmt, unitDesc, rate, amount, explanation
                );

                WcbRemittanceLine line = new WcbRemittanceLine();
                line.setRemittance(remittance);
                line.setContentHash(hash);
                line.setInvoiceNo(invoiceNo);
                line.setClaimNumber(claimNumber);
                line.setCustomerName(customerName);
                line.setServiceDate(serviceDate);
                line.setServiceCode(serviceCode);
                line.setInvoiceAmount(invoiceAmt);
                line.setUnitDescription(unitDesc);
                line.setRate(rate);
                line.setAmount(amount);
                line.setExplanation(explanation);

                lineRepository.save(line);
                lastProcessingResult.getLineItems().saved++;
                logger.debug("✅ Saved line item: claim_number={}, invoice_no={}", claimNumber, invoiceNo);
            }

            logger.info("WCB processing complete for receipt {}: {} line items saved, {} rejected",
                receipt.getId(),
                lastProcessingResult.getLineItems().saved,
                lastProcessingResult.getLineItems().rejected);

            // Validate total amount matches sum of line items
            BigDecimal lineSum = items.stream()
                .map(item -> parseAmount(getStr(item, "amount")))
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal headerTotal = parseAmount(getStr(header, "total_amount", "total_paid_amount", "total", "amount"));

            if (headerTotal != null && lineSum.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal diff = headerTotal.subtract(lineSum).abs();
                BigDecimal threshold = headerTotal.multiply(new BigDecimal("0.01"));
                if (diff.compareTo(threshold) > 0) {
                    lastProcessingResult.setTotalMismatchWarning(
                        "Header total " + headerTotal + " does not match sum of line amounts " + lineSum);
                    logger.warn("Total mismatch for receipt {}: header={}, lineSum={}",
                        receipt.getId(), headerTotal, lineSum);
                }
            }
        }

        // Determine overall status
        if ("SAVED".equals(lastProcessingResult.getSummary().status)) {
            lastProcessingResult.setStatus(
                lastProcessingResult.getTotalMismatchWarning() != null ? "PARTIAL" : "SUCCESS"
            );
        }
    }

    public WcbProcessingResult getLastProcessingResult() {
        return lastProcessingResult;
    }

    private static String computeContentHash(
            String claimNumber, String invoiceNo, String customerName,
            LocalDate serviceDate, String serviceCode,
            BigDecimal invoiceAmount, String unitDescription,
            BigDecimal rate, BigDecimal amount, String explanation) {

        String canonical = String.join("|",
            s(claimNumber), s(invoiceNo), s(customerName),
            serviceDate != null ? serviceDate.toString() : "",
            s(serviceCode), d(invoiceAmount), s(unitDescription), d(rate), d(amount), s(explanation)
        );
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                              .digest(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static String s(String v) { return v == null ? "" : v; }
    private static String d(BigDecimal v) {
        return v == null ? "" : v.stripTrailingZeros().toPlainString();
    }

    private String getStr(Map<String, Object> map, String... keys) {
        if (map == null) return null;
        for (String key : keys) {
            Object v = map.get(key);
            if (v != null && !v.toString().trim().isEmpty() && !"null".equalsIgnoreCase(v.toString())) {
                return v.toString().trim();
            }
        }
        return null;
    }

    private BigDecimal parseAmount(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            String cleaned = value.replaceAll("[$,\\s]", "").replaceAll("[^0-9.-]", "");
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            logger.debug("Could not parse amount: {}", value);
            return null;
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.trim().isEmpty()) return null;

        String[] formats = {"MMM d yyyy", "MMM dd yyyy", "yyyy-MM-dd", "MM/dd/yyyy", "dd/MM/yyyy"};
        for (String fmt : formats) {
            try {
                return LocalDate.parse(value.trim(), DateTimeFormatter.ofPattern(fmt));
            } catch (Exception ignored) {
            }
        }
        logger.debug("Could not parse date: {}", value);
        return null;
    }
}
