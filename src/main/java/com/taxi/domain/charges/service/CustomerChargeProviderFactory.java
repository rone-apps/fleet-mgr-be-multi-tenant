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
        String tenantId = TenantContext.getCurrentTenant();

        boolean useLegacy = tenantConfigService.getCurrentTenantConfig()
            .map(config -> config.isUseLegacyChargeSystem())
            .orElse(false);  // Default to modern system if config missing

        CustomerChargeDataProvider selectedProvider = useLegacy ? legacyProvider : modernProvider;

        log.debug("Tenant '{}' using {} charge provider", tenantId, selectedProvider.getImplementationType());

        return selectedProvider;
    }
}
