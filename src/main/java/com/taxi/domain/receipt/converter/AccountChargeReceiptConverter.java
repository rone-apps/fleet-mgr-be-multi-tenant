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
        logger.debug("AccountChargeReceiptConverter.convert called for receipt {}", receipt.getId());
        // Converter for future use - currently not invoked in confirm step
        return;

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
