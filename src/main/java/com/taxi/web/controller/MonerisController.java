package com.taxi.web.controller;

import com.taxi.domain.moneris.MonerisApiService;
import com.taxi.domain.moneris.MonerisConfig;
import com.taxi.domain.moneris.MonerisConfigRepository;
import com.taxi.domain.payment.model.CreditCardTransaction;
import com.taxi.domain.payment.repository.CreditCardTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/moneris")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
public class MonerisController {

    private final CreditCardTransactionRepository transactionRepository;
    private final MonerisConfigRepository monerisConfigRepository;
    private final MonerisApiService monerisApiService;
    private final EntityManager entityManager;

    // ========================
    // Moneris Config CRUD
    // ========================

    @GetMapping("/configs")
    public ResponseEntity<?> getAllConfigs() {
        List<MonerisConfig> configs = monerisApiService.getAllConfigs();
        List<Map<String, Object>> result = configs.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("cabNumber", c.getCabNumber());
            m.put("shift", c.getShift());
            m.put("merchantNumber", c.getMerchantNumber());
            m.put("monerisStoreId", c.getMonerisStoreId());
            m.put("hasApiToken", c.getMonerisApiToken() != null && !c.getMonerisApiToken().isBlank());
            m.put("monerisEnvironment", c.getMonerisEnvironment());
            m.put("createdAt", c.getCreatedAt());
            m.put("updatedAt", c.getUpdatedAt());
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/configs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createConfig(@RequestBody Map<String, String> body) {
        try {
            MonerisConfig config = MonerisConfig.builder()
                    .cabNumber(requireField(body, "cabNumber"))
                    .shift(body.getOrDefault("shift", "BOTH"))
                    .merchantNumber(requireField(body, "merchantNumber"))
                    .monerisStoreId(requireField(body, "monerisStoreId"))
                    .monerisApiToken(requireField(body, "monerisApiToken"))
                    .monerisEnvironment(body.getOrDefault("monerisEnvironment", "PROD"))
                    .build();

            MonerisConfig saved = monerisApiService.saveConfig(config);
            return ResponseEntity.ok(Map.of("id", saved.getId(), "message", "Moneris config created"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/configs/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateConfig(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            MonerisConfig config = monerisConfigRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Config not found: " + id));

            if (body.containsKey("cabNumber")) config.setCabNumber(body.get("cabNumber"));
            if (body.containsKey("shift")) config.setShift(body.get("shift"));
            if (body.containsKey("merchantNumber")) config.setMerchantNumber(body.get("merchantNumber"));
            if (body.containsKey("monerisStoreId")) config.setMonerisStoreId(body.get("monerisStoreId"));
            if (body.containsKey("monerisApiToken") && !body.get("monerisApiToken").isBlank()) {
                config.setMonerisApiToken(body.get("monerisApiToken"));
            }
            if (body.containsKey("monerisEnvironment")) config.setMonerisEnvironment(body.get("monerisEnvironment"));

            monerisApiService.saveConfig(config);
            return ResponseEntity.ok(Map.of("message", "Moneris config updated"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/configs/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteConfig(@PathVariable Long id) {
        try {
            monerisApiService.deleteConfig(id);
            return ResponseEntity.ok(Map.of("message", "Moneris config deleted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================
    // Connection & Sync
    // ========================

    @PostMapping("/configs/{id}/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> testConnection(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(monerisApiService.testConnection(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("connected", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/test-connection")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> testConnectionDirect(@RequestBody Map<String, String> body) {
        try {
            String storeId = requireField(body, "monerisStoreId");
            String apiToken = requireField(body, "monerisApiToken");
            String env = body.getOrDefault("monerisEnvironment", "PROD");
            return ResponseEntity.ok(monerisApiService.testConnectionWithCredentials(storeId, apiToken, env));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("connected", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/test-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> testAllConnections() {
        return ResponseEntity.ok(monerisApiService.testAllConnections());
    }

    @GetMapping("/configs/{id}/open-totals")
    public ResponseEntity<?> getOpenTotals(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(monerisApiService.getOpenTotals(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/sync")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> syncAllTransactions(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            return ResponseEntity.ok(monerisApiService.syncAllTransactions(startDate, endDate));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================
    // Transaction Browsing
    // ========================

    @GetMapping("/transactions")
    public ResponseEntity<?> searchTransactions(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String merchantId,
            @RequestParam(required = false) String terminalId,
            @RequestParam(required = false) String cardType,
            @RequestParam(required = false) String cabNumber,
            @RequestParam(required = false) String driverNumber,
            @RequestParam(required = false) String authorizationCode,
            @RequestParam(required = false) String transactionStatus,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) String jobId,
            @RequestParam(defaultValue = "transactionDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {

        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<CreditCardTransaction> cq = cb.createQuery(CreditCardTransaction.class);
            Root<CreditCardTransaction> root = cq.from(CreditCardTransaction.class);

            List<Predicate> predicates = buildPredicates(cb, root, startDate, endDate,
                    merchantId, terminalId, cardType, cabNumber, driverNumber,
                    authorizationCode, transactionStatus, minAmount, maxAmount, jobId);

            cq.where(predicates.toArray(new Predicate[0]));

            if ("desc".equalsIgnoreCase(sortDir)) {
                cq.orderBy(cb.desc(root.get(sortBy)), cb.desc(root.get("transactionTime")));
            } else {
                cq.orderBy(cb.asc(root.get(sortBy)), cb.asc(root.get("transactionTime")));
            }

            // Count query
            CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
            Root<CreditCardTransaction> countRoot = countQuery.from(CreditCardTransaction.class);
            List<Predicate> countPredicates = buildPredicates(cb, countRoot, startDate, endDate,
                    merchantId, terminalId, cardType, cabNumber, driverNumber,
                    authorizationCode, transactionStatus, minAmount, maxAmount, jobId);
            countQuery.select(cb.count(countRoot)).where(countPredicates.toArray(new Predicate[0]));
            Long totalCount = entityManager.createQuery(countQuery).getSingleResult();

            TypedQuery<CreditCardTransaction> typedQuery = entityManager.createQuery(cq);
            typedQuery.setFirstResult(page * size);
            typedQuery.setMaxResults(size);
            List<CreditCardTransaction> results = typedQuery.getResultList();

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("content", results.stream().map(this::toMap).collect(Collectors.toList()));
            response.put("totalElements", totalCount);
            response.put("totalPages", (int) Math.ceil((double) totalCount / size));
            response.put("page", page);
            response.put("size", size);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error searching transactions", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/transactions/summary")
    public ResponseEntity<?> getTransactionSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<CreditCardTransaction> txns = transactionRepository.findByTransactionDateBetween(startDate, endDate);

        BigDecimal totalAmount = txns.stream().map(CreditCardTransaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalTips = txns.stream().map(t -> t.getTipAmount() != null ? t.getTipAmount() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalFees = txns.stream().map(t -> t.getProcessingFee() != null ? t.getProcessingFee() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Long> byCardType = txns.stream()
                .collect(Collectors.groupingBy(t -> t.getCardType() != null ? t.getCardType() : "UNKNOWN", Collectors.counting()));
        Map<String, Long> byStatus = txns.stream()
                .collect(Collectors.groupingBy(t -> t.getTransactionStatus() != null ? t.getTransactionStatus().name() : "UNKNOWN", Collectors.counting()));

        long withDriver = txns.stream().filter(t -> t.getDriverNumber() != null && !t.getDriverNumber().isBlank()).count();
        long withCab = txns.stream().filter(t -> t.getCabNumber() != null && !t.getCabNumber().isBlank() && !"N/A".equals(t.getCabNumber())).count();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalTransactions", txns.size());
        summary.put("totalAmount", totalAmount);
        summary.put("totalTips", totalTips);
        summary.put("totalProcessingFees", totalFees);
        summary.put("netAmount", totalAmount.add(totalTips).subtract(totalFees));
        summary.put("byCardType", byCardType);
        summary.put("byStatus", byStatus);
        summary.put("withDriverAssigned", withDriver);
        summary.put("withCabAssigned", withCab);
        summary.put("unassignedCount", txns.size() - withCab);

        return ResponseEntity.ok(summary);
    }

    @GetMapping("/transactions/filters")
    public ResponseEntity<?> getFilterOptions(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<CreditCardTransaction> txns = transactionRepository.findByTransactionDateBetween(startDate, endDate);

        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("merchantIds", txns.stream().map(CreditCardTransaction::getMerchantId).filter(Objects::nonNull).distinct().sorted().collect(Collectors.toList()));
        filters.put("terminalIds", txns.stream().map(CreditCardTransaction::getTerminalId).filter(Objects::nonNull).distinct().sorted().collect(Collectors.toList()));
        filters.put("cardTypes", txns.stream().map(CreditCardTransaction::getCardType).filter(Objects::nonNull).distinct().sorted().collect(Collectors.toList()));
        filters.put("cabNumbers", txns.stream().map(CreditCardTransaction::getCabNumber).filter(s -> s != null && !s.isBlank()).distinct().sorted().collect(Collectors.toList()));
        filters.put("driverNumbers", txns.stream().map(CreditCardTransaction::getDriverNumber).filter(s -> s != null && !s.isBlank()).distinct().sorted().collect(Collectors.toList()));
        filters.put("statuses", Arrays.stream(CreditCardTransaction.TransactionStatus.values()).map(Enum::name).collect(Collectors.toList()));

        return ResponseEntity.ok(filters);
    }

    // ========================
    // Helpers
    // ========================

    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<CreditCardTransaction> root,
            LocalDate startDate, LocalDate endDate,
            String merchantId, String terminalId, String cardType,
            String cabNumber, String driverNumber, String authorizationCode,
            String transactionStatus, BigDecimal minAmount, BigDecimal maxAmount, String jobId) {

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.greaterThanOrEqualTo(root.get("transactionDate"), startDate));
        predicates.add(cb.lessThanOrEqualTo(root.get("transactionDate"), endDate));

        if (merchantId != null && !merchantId.isBlank()) predicates.add(cb.equal(root.get("merchantId"), merchantId));
        if (terminalId != null && !terminalId.isBlank()) predicates.add(cb.equal(root.get("terminalId"), terminalId));
        if (cardType != null && !cardType.isBlank()) predicates.add(cb.equal(root.get("cardType"), cardType));
        if (cabNumber != null && !cabNumber.isBlank()) predicates.add(cb.equal(root.get("cabNumber"), cabNumber));
        if (driverNumber != null && !driverNumber.isBlank()) predicates.add(cb.equal(root.get("driverNumber"), driverNumber));
        if (authorizationCode != null && !authorizationCode.isBlank()) predicates.add(cb.like(root.get("authorizationCode"), "%" + authorizationCode + "%"));
        if (transactionStatus != null && !transactionStatus.isBlank()) predicates.add(cb.equal(root.get("transactionStatus").as(String.class), transactionStatus));
        if (minAmount != null) predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), minAmount));
        if (maxAmount != null) predicates.add(cb.lessThanOrEqualTo(root.get("amount"), maxAmount));
        if (jobId != null && !jobId.isBlank()) predicates.add(cb.like(root.get("jobId"), "%" + jobId + "%"));

        return predicates;
    }

    private String requireField(Map<String, String> body, String field) {
        String val = body.get(field);
        if (val == null || val.isBlank()) throw new IllegalArgumentException(field + " is required");
        return val;
    }

    private Map<String, Object> toMap(CreditCardTransaction t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("transactionDate", t.getTransactionDate());
        m.put("transactionTime", t.getTransactionTime());
        m.put("merchantId", t.getMerchantId());
        m.put("terminalId", t.getTerminalId());
        m.put("authorizationCode", t.getAuthorizationCode());
        m.put("cardType", t.getCardType());
        m.put("cardBrand", t.getCardBrand());
        m.put("cardLastFour", t.getCardLastFour());
        m.put("cardholderNumber", t.getCardholderNumber());
        m.put("amount", t.getAmount());
        m.put("tipAmount", t.getTipAmount());
        m.put("totalAmount", t.getTotalAmount());
        m.put("processingFee", t.getProcessingFee());
        m.put("netAmount", t.getNetAmount());
        m.put("transactionStatus", t.getTransactionStatus());
        m.put("cabNumber", t.getCabNumber());
        m.put("driverNumber", t.getDriverNumber());
        m.put("jobId", t.getJobId());
        m.put("batchNumber", t.getBatchNumber());
        m.put("captureMethod", t.getCaptureMethod());
        m.put("isSettled", t.getIsSettled());
        m.put("isRefunded", t.getIsRefunded());
        m.put("settlementDate", t.getSettlementDate());
        m.put("customerName", t.getCustomerName());
        m.put("referenceNumber", t.getReferenceNumber());
        m.put("receiptNumber", t.getReceiptNumber());
        m.put("uploadBatchId", t.getUploadBatchId());
        m.put("uploadFilename", t.getUploadFilename());
        m.put("notes", t.getNotes());
        return m;
    }
}
