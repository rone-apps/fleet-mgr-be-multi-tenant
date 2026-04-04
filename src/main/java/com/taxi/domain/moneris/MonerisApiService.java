package com.taxi.domain.moneris;

import com.taxi.domain.payment.model.CreditCardTransaction;
import com.taxi.domain.payment.repository.CreditCardTransactionRepository;
import com.taxi.domain.shift.model.DriverShift;
import com.taxi.domain.shift.repository.DriverShiftRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service for communicating with the Moneris Gateway API.
 * Uses per-cab credentials from the moneris_config table.
 *
 * Moneris Gateway endpoints:
 *   Production: https://www3.moneris.com/gateway2/servlet/MpgRequest
 *   Test:       https://esqa.moneris.com/gateway2/servlet/MpgRequest
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MonerisApiService {

    private static final String PROD_URL = "https://www3.moneris.com/gateway2/servlet/MpgRequest";
    private static final String TEST_URL = "https://esqa.moneris.com/gateway2/servlet/MpgRequest";

    private final MonerisConfigRepository monerisConfigRepository;
    private final CreditCardTransactionRepository transactionRepository;
    private final DriverShiftRepository driverShiftRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    // ========================
    // CRUD for Moneris Config
    // ========================

    public List<MonerisConfig> getAllConfigs() {
        return monerisConfigRepository.findAll();
    }

    public MonerisConfig saveConfig(MonerisConfig config) {
        return monerisConfigRepository.save(config);
    }

    public void deleteConfig(Long id) {
        monerisConfigRepository.deleteById(id);
    }

    public Optional<MonerisConfig> getConfig(Long id) {
        return monerisConfigRepository.findById(id);
    }

    // ========================
    // Connection Testing
    // ========================

    /**
     * Test connection for a specific moneris_config entry
     */
    public Map<String, Object> testConnection(Long configId) {
        MonerisConfig config = monerisConfigRepository.findById(configId)
                .orElseThrow(() -> new RuntimeException("Moneris config not found: " + configId));
        return testConnectionWithCredentials(config.getMonerisStoreId(), config.getMonerisApiToken(), config.getMonerisEnvironment());
    }

    /**
     * Test connection with raw credentials (before saving)
     */
    public Map<String, Object> testConnectionWithCredentials(String storeId, String apiToken, String environment) {
        Map<String, Object> result = new LinkedHashMap<>();
        String xml = buildXml(storeId, apiToken,
                "<open_totals><ecr_number>1</ecr_number></open_totals>");

        try {
            String response = sendRequest(xml, environment);
            Document doc = parseXml(response);
            String responseCode = getXmlValue(doc, "ResponseCode");
            String message = getXmlValue(doc, "Message");

            result.put("storeId", storeId);
            result.put("environment", environment);
            result.put("responseCode", responseCode);
            result.put("message", message);
            result.put("rawResponse", response);

            boolean success = responseCode != null;
            result.put("connected", success);
            result.put("status", success ? "Connected" : "Failed");
        } catch (Exception e) {
            log.error("Moneris connection test failed for store {}", storeId, e);
            result.put("connected", false);
            result.put("status", "Error");
            result.put("error", e.getMessage());
        }
        return result;
    }

    /**
     * Test all configured Moneris connections
     */
    public List<Map<String, Object>> testAllConnections() {
        List<Map<String, Object>> results = new ArrayList<>();
        for (MonerisConfig config : monerisConfigRepository.findAll()) {
            Map<String, Object> result = testConnectionWithCredentials(
                    config.getMonerisStoreId(), config.getMonerisApiToken(), config.getMonerisEnvironment());
            result.put("configId", config.getId());
            result.put("cabNumber", config.getCabNumber());
            result.put("merchantNumber", config.getMerchantNumber());
            results.add(result);
        }
        return results;
    }

    // ========================
    // Transaction Sync
    // ========================

    /**
     * Sync transactions from all configured Moneris accounts for a date range.
     * Iterates through each moneris_config entry and attempts to pull transactions.
     */
    @Transactional
    public Map<String, Object> syncAllTransactions(LocalDate startDate, LocalDate endDate) {
        List<MonerisConfig> configs = monerisConfigRepository.findAll();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("startDate", startDate.toString());
        result.put("endDate", endDate.toString());
        result.put("configCount", configs.size());

        int totalNew = 0;
        int totalDuplicates = 0;
        int totalErrors = 0;
        List<Map<String, Object>> perCabResults = new ArrayList<>();

        for (MonerisConfig config : configs) {
            Map<String, Object> cabResult = syncTransactionsForConfig(config, startDate, endDate);
            perCabResults.add(cabResult);
            totalNew += (int) cabResult.getOrDefault("newTransactions", 0);
            totalDuplicates += (int) cabResult.getOrDefault("duplicates", 0);
            totalErrors += (int) cabResult.getOrDefault("errors", 0);
        }

        result.put("totalNewTransactions", totalNew);
        result.put("totalDuplicates", totalDuplicates);
        result.put("totalErrors", totalErrors);
        result.put("perCabResults", perCabResults);
        return result;
    }

    /**
     * Sync transactions for a single Moneris config (one cab/store).
     */
    @Transactional
    public Map<String, Object> syncTransactionsForConfig(MonerisConfig config, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("configId", config.getId());
        result.put("cabNumber", config.getCabNumber());
        result.put("storeId", config.getMonerisStoreId());
        result.put("merchantNumber", config.getMerchantNumber());

        int newCount = 0;
        int duplicateCount = 0;
        int errorCount = 0;

        try {
            // Try open_totals to verify connection and get batch info
            String xml = buildXml(config.getMonerisStoreId(), config.getMonerisApiToken(),
                    "<open_totals><ecr_number>1</ecr_number></open_totals>");
            String response = sendRequest(xml, config.getMonerisEnvironment());
            log.info("Moneris open_totals for cab {} (store {}): {}", config.getCabNumber(), config.getMonerisStoreId(), response);

            Document doc = parseXml(response);
            String responseCode = getXmlValue(doc, "ResponseCode");
            result.put("responseCode", responseCode);
            result.put("connected", responseCode != null);

            // The legacy Gateway API doesn't have a transaction-listing endpoint.
            // open_totals gives batch-level summaries only.
            // We log the connection status and return guidance.
            result.put("message",
                    "Connected to Moneris (Store: " + config.getMonerisStoreId() + "). " +
                    "The Gateway API returns batch totals only — individual transactions require CSV upload from the Moneris Go Portal, " +
                    "or migration to the Moneris Unified API (api.moneris.io) with OAuth credentials.");
        } catch (Exception e) {
            log.error("Moneris sync failed for cab {} store {}", config.getCabNumber(), config.getMonerisStoreId(), e);
            result.put("connected", false);
            result.put("error", e.getMessage());
            errorCount++;
        }

        result.put("newTransactions", newCount);
        result.put("duplicates", duplicateCount);
        result.put("errors", errorCount);
        return result;
    }

    // ========================
    // Open Totals
    // ========================

    /**
     * Get open batch totals for a specific config
     */
    public Map<String, Object> getOpenTotals(Long configId) {
        MonerisConfig config = monerisConfigRepository.findById(configId)
                .orElseThrow(() -> new RuntimeException("Moneris config not found: " + configId));

        String xml = buildXml(config.getMonerisStoreId(), config.getMonerisApiToken(),
                "<open_totals><ecr_number>1</ecr_number></open_totals>");
        String response = sendRequest(xml, config.getMonerisEnvironment());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cabNumber", config.getCabNumber());
        result.put("storeId", config.getMonerisStoreId());
        result.put("rawResponse", response);

        try {
            Document doc = parseXml(response);
            result.put("responseCode", getXmlValue(doc, "ResponseCode"));
            result.put("message", getXmlValue(doc, "Message"));

            NodeList cardTotals = doc.getElementsByTagName("CardType");
            List<Map<String, String>> cards = new ArrayList<>();
            for (int i = 0; i < cardTotals.getLength(); i++) {
                Element el = (Element) cardTotals.item(i);
                Map<String, String> card = new LinkedHashMap<>();
                card.put("type", getChildText(el, "Type"));
                card.put("count", getChildText(el, "Count"));
                card.put("amount", getChildText(el, "Amount"));
                cards.add(card);
            }
            result.put("cardTotals", cards);
        } catch (Exception e) {
            result.put("parseError", e.getMessage());
        }
        return result;
    }

    // ========================
    // XML Helpers
    // ========================

    private String buildXml(String storeId, String apiToken, String operation) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "<request>" +
                "<store_id>" + escapeXml(storeId) + "</store_id>" +
                "<api_token>" + escapeXml(apiToken) + "</api_token>" +
                operation +
                "</request>";
    }

    private String sendRequest(String xml, String environment) {
        String url = "PROD".equals(environment) ? PROD_URL : TEST_URL;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        HttpEntity<String> entity = new HttpEntity<>(xml, headers);

        log.debug("Sending Moneris request to {}", url);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Moneris API returned HTTP " + response.getStatusCode());
        }
        return response.getBody();
    }

    private Document parseXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private String getXmlValue(Document doc, String tagName) {
        NodeList nodes = doc.getElementsByTagName(tagName);
        return nodes.getLength() > 0 ? nodes.item(0).getTextContent() : null;
    }

    private String getChildText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        return nodes.getLength() > 0 ? nodes.item(0).getTextContent() : null;
    }

    private String escapeXml(String val) {
        if (val == null) return "";
        return val.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
