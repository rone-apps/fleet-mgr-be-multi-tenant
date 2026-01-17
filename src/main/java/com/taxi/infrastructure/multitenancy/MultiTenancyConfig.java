package com.taxi.infrastructure.multitenancy;

import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Configuration for schema-based multi-tenancy with Hibernate.
 * 
 * Note: In Hibernate 6.x, multi-tenancy is configured via the 
 * MultiTenantConnectionProvider and CurrentTenantIdentifierResolver beans
 * which are already registered in SchemaBasedMultiTenantConnectionProvider
 * and TenantIdentifierResolver classes.
 */
@Configuration
public class MultiTenancyConfig {
    
    /**
     * Configure Hibernate for SCHEMA-based multi-tenancy
     */
    @Bean
    public HibernatePropertiesCustomizer multiTenancyCustomizer() {
        return (Map<String, Object> hibernateProperties) -> {
            // In Hibernate 6.x, multi-tenancy mode is auto-detected based on
            // the presence of MultiTenantConnectionProvider bean
            // No explicit MULTI_TENANT setting needed
        };
    }
}
