package com.taxi.infrastructure.multitenancy;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Hibernate CurrentTenantIdentifierResolver that resolves the current tenant
 * from the TenantContext thread-local storage.
 */
@Component
@Slf4j
public class TenantIdentifierResolver 
        implements CurrentTenantIdentifierResolver, HibernatePropertiesCustomizer {
    
    @Override
    public String resolveCurrentTenantIdentifier() {
        try {
            String tenant = TenantContext.getCurrentTenant();
            log.debug("Resolved current tenant: {}", tenant);
            return tenant;
        } catch (IllegalStateException e) {
            // During startup/non-request contexts, fall back to system tenant
            log.debug("No tenant set, using system tenant: {}", TenantContext.SYSTEM_TENANT);
            return TenantContext.SYSTEM_TENANT;
        }
    }
    
    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
    
    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, this);
    }
}
