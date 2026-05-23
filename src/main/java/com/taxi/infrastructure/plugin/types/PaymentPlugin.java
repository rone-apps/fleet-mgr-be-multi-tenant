package com.taxi.infrastructure.plugin.types;

import com.taxi.domain.payment.model.CreditCardTransaction;
import com.taxi.infrastructure.plugin.config.PluginConfig;
import com.taxi.infrastructure.plugin.core.Plugin;

import java.time.LocalDate;
import java.util.List;

/**
 * Plugin interface for payment processor integrations (Moneris, Chase, Square, Stripe, etc.).
 * Supports synchronizing credit card transactions from payment processors.
 */
public interface PaymentPlugin extends Plugin {

    /**
     * Synchronize credit card transactions for a date range.
     *
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of synchronized transactions
     */
    List<CreditCardTransaction> syncTransactions(LocalDate startDate, LocalDate endDate);

    /**
     * Test connection to payment processor using provided configuration.
     *
     * @param config Plugin configuration
     * @return true if connection successful
     */
    boolean testConnection(PluginConfig config);
}
