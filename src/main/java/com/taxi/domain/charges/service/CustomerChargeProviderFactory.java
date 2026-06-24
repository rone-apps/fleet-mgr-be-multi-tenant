package com.taxi.domain.charges.service;

import com.taxi.domain.tenant.service.TenantConfigService;
import com.taxi.infrastructure.multitenancy.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Factory that selects the appropriate CustomerChargeDataProvider
 * based on the current tenant's feature flag configuration.
 *
 * This is the SINGLE POINT OF DECISION for charge system selection.
 */
@Component
@Slf4j
public class CustomerChargeProviderFactory {

    private final TenantConfigService tenantConfigService;
    private final CustomerChargeDataProvider legacyProvider;
    private final CustomerChargeDataProvider modernProvider;

    /**
     * Constructor with explicit qualifiers for proper Spring bean injection
     */
    public CustomerChargeProviderFactory(
            TenantConfigService tenantConfigService,
            @Qualifier("legacyCustomerChargeProvider") CustomerChargeDataProvider legacyProvider,
            @Qualifier("modernAccountChargeProvider") CustomerChargeDataProvider modernProvider) {
        this.tenantConfigService = tenantConfigService;
        this.legacyProvider = legacyProvider;
        this.modernProvider = modernProvider;
    }

    /**
     * Get the appropriate charge provider for the current tenant.
     *
     * @return Provider instance based on tenant configuration
     */
    public CustomerChargeDataProvider getProvider() {
        return getProvider(null);
    }

    /**
     * Get the appropriate charge provider with optional request-scoped override.
     *
     * This allows per-request override of the tenant-wide configuration without
     * modifying the tenant config itself. Useful for UI toggles that let users
     * compare legacy vs modern charge systems.
     *
     * @param useModernOverride Optional override: null = use tenant config,
     *                          true = force modern provider,
     *                          false = force legacy provider
     * @return Provider instance based on override or tenant configuration
     */
    public CustomerChargeDataProvider getProvider(Boolean useModernOverride) {
        String tenantId = TenantContext.getCurrentTenant();

        boolean useModern;
        if (useModernOverride != null) {
            // Request-scoped override takes precedence
            useModern = useModernOverride;
            log.debug("Tenant '{}' using {} charge provider (request override)",
                tenantId, useModern ? "MODERN" : "LEGACY");
        } else {
            // Fall back to tenant config
            boolean useLegacy = tenantConfigService.getCurrentTenantConfig()
                .map(config -> config.isUseLegacyChargeSystem())
                .orElse(false);  // Default to modern system if config missing
            useModern = !useLegacy;
            log.debug("Tenant '{}' using {} charge provider (tenant config)",
                tenantId, useModern ? "MODERN" : "LEGACY");
        }

        return useModern ? modernProvider : legacyProvider;
    }
}
