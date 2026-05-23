package com.taxi.infrastructure.plugin.core;

/**
 * Types of plugins supported by FareFlow.
 */
public enum PluginType {
    /**
     * Dispatch system integrations (TaxiCaller, iCabbi, TripMaster, etc.)
     */
    DISPATCH,

    /**
     * Payment processor integrations (Moneris, Chase, Square, Stripe, etc.)
     */
    PAYMENT,

    /**
     * Data import plugins (CSV, Excel, API-based data sources)
     */
    DATA_IMPORT,

    /**
     * Real-time webhook receivers
     */
    WEBHOOK
}
