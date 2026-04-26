package com.taxi.domain.receipt.converter;

import com.taxi.domain.account.model.AccountCharge;
import com.taxi.domain.account.model.AccountCustomer;
import com.taxi.domain.account.repository.AccountCustomerRepository;
import com.taxi.domain.account.service.AccountChargeService;
import com.taxi.domain.receipt.model.Receipt;
import com.taxi.domain.receipt.model.ReceiptType;
import com.taxi.web.dto.receipt.ConfirmReceiptRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class AccountChargeReceiptConverter implements ReceiptTypeConverter {
    private static final Logger logger = LoggerFactory.getLogger(AccountChargeReceiptConverter.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d yyyy");

    private final AccountChargeService accountChargeService;
    private final AccountCustomerRepository accountCustomerRepository;

    public AccountChargeReceiptConverter(AccountChargeService accountChargeService,
                                       AccountCustomerRepository accountCustomerRepository) {
        this.accountChargeService = accountChargeService;
        this.accountCustomerRepository = accountCustomerRepository;
    }

    @Override
    public ReceiptType getSupportedType() {
        return ReceiptType.ACCOUNT_CHARGE;
    }

    @Override
    public void convert(Receipt receipt, Map<String, Object> parsedJson, ConfirmReceiptRequest request) {
        // Only convert if accountCustomerId is provided
        if (request.getAccountCustomerId() == null) {
            logger.warn("No accountCustomerId provided for ACCOUNT_CHARGE receipt {}, skipping conversion", receipt.getId());
            return;
        }

        // Load customer
        AccountCustomer customer = accountCustomerRepository.findById(request.getAccountCustomerId())
            .orElse(null);
        if (customer == null) {
            logger.error("Customer not found with id: {}", request.getAccountCustomerId());
            return;
        }

        if (parsedJson == null) {
            logger.warn("No parsed JSON data for receipt {}, skipping conversion", receipt.getId());
            return;
        }

        // Extract items from parsed JSON
        List<Map<String, Object>> items = null;
        if (parsedJson.get("items") instanceof List) {
            items = (List<Map<String, Object>>) parsedJson.get("items");
        }

        if (items == null || items.isEmpty()) {
            logger.warn("No items found in parsed JSON for receipt {}", receipt.getId());
            return;
        }

        // Convert each item to an AccountCharge
        for (Map<String, Object> item : items) {
            try {
                AccountCharge charge = buildAccountCharge(item, customer, request);
                if (charge != null) {
                    accountChargeService.createCharge(charge);
                    logger.info("Created AccountCharge from receipt item: invoiceNo={}, amount={}",
                        item.get("invoice_no"), item.get("amount"));
                }
            } catch (Exception e) {
                logger.error("Failed to convert receipt item for receipt {}: {}", receipt.getId(), e.getMessage(), e);
            }
        }
    }

    private AccountCharge buildAccountCharge(Map<String, Object> item, AccountCustomer customer, ConfirmReceiptRequest request) {
        // Parse amount from currency string (e.g. "$1,234.56" -> 1234.56)
        BigDecimal amount = parseAmount(item.get("amount"));
        if (amount == null || amount.signum() == 0) {
            logger.debug("Skipping item with null or zero amount: {}", item.get("invoice_no"));
            return null;
        }

        // Parse service_date from human-readable format (e.g. "Jan 15 2026")
        LocalDate tripDate = parseDate(item.get("service_date"));
        if (tripDate == null) {
            logger.warn("Could not parse service_date for item: {}", item.get("invoice_no"));
            tripDate = LocalDate.now();
        }

        AccountCharge charge = new AccountCharge();
        charge.setAccountCustomer(customer);
        charge.setAccountId(customer.getAccountId());
        charge.setFareAmount(amount);
        charge.setTipAmount(BigDecimal.ZERO);
        charge.setTripDate(tripDate);
        charge.setJobCode((String) item.get("service_code"));
        charge.setPassengerName((String) item.get("name"));
        charge.setIsManual(true);

        // Optional fields from the item
        if (item.get("invoice_no") != null) {
            charge.setInvoiceNumber((String) item.get("invoice_no"));
        }

        return charge;
    }

    private BigDecimal parseAmount(Object amountObj) {
        if (amountObj == null) {
            return null;
        }

        String amountStr = amountObj.toString().trim();
        if (amountStr.isEmpty()) {
            return null;
        }

        // Remove $ and commas
        amountStr = amountStr.replaceAll("[^0-9.]", "");
        try {
            return new BigDecimal(amountStr);
        } catch (NumberFormatException e) {
            logger.debug("Failed to parse amount: {}", amountObj);
            return null;
        }
    }

    private LocalDate parseDate(Object dateObj) {
        if (dateObj == null) {
            return null;
        }

        String dateStr = dateObj.toString().trim();
        if (dateStr.isEmpty()) {
            return null;
        }

        try {
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (Exception e) {
            logger.debug("Failed to parse date: {}", dateObj);
            return null;
        }
    }
}
