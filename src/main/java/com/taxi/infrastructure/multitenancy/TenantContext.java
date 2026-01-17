package com.taxi.infrastructure.multitenancy;

import lombok.extern.slf4j.Slf4j;

/**
 * Thread-local context for holding the current tenant identifier.
 * Used for schema-based multi-tenancy.
 */
@Slf4j
public final class TenantContext {

    private TenantContext() {}

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    /**
     * Default schema used ONLY for system/background operations
     * (migrations, bootstrap, cron jobs).
     */
    public static final String SYSTEM_TENANT = "fareflow";

    /**
     * Set tenant for the current request thread
     */
    public static void setCurrentTenant(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("Tenant ID cannot be null or blank");
        }
        log.debug("Setting tenant: {}", tenantId);
        CURRENT_TENANT.set(tenantId);
    }

    /**
     * Get tenant for the current request thread.
     * Fails fast if tenant is missing.
     */
    public static String getCurrentTenant() {
        String tenant = CURRENT_TENANT.get();
        if (tenant == null) {
            throw new IllegalStateException(
                "Tenant not set for request. This is a bug."
            );
        }
        return tenant;
    }

    /**
     * Explicitly set system tenant (non-HTTP contexts only).
     */
    public static void setSystemTenant() {
        CURRENT_TENANT.set(SYSTEM_TENANT);
    }

    /**
     * Clear tenant (MANDATORY for thread pool reuse).
     */
    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
