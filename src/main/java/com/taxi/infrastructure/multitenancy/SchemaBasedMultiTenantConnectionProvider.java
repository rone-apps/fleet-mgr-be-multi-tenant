package com.taxi.infrastructure.multitenancy;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

/**
 * Hibernate MultiTenantConnectionProvider that switches schemas based on tenant context.
 * 
 * This implementation uses a shared database with separate schemas per tenant.
 * Each tenant's data is isolated in their own schema (e.g., fareflow, yellowcab, citytaxi, etc.)
 */
@Component
@Slf4j
public class SchemaBasedMultiTenantConnectionProvider 
        implements MultiTenantConnectionProvider, HibernatePropertiesCustomizer {
    
    private final DataSource dataSource;
    
    @Autowired
    public SchemaBasedMultiTenantConnectionProvider(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }
    
    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }
    
    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        log.debug("Getting connection for tenant: {}", tenantIdentifier);
        
        Connection connection = dataSource.getConnection();
        
        // Determine the schema to use
        String schema = sanitizeSchemaName(tenantIdentifier);
        
        // If schema is empty or null, use the default
        if (schema == null || schema.isEmpty()) {
            schema = TenantContext.SYSTEM_TENANT;
        }
        
        try {
            // For MySQL, use the database/schema
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("USE `" + schema + "`");
                log.debug("Switched to schema: {}", schema);
            }
        } catch (SQLException e) {
            // Schema doesn't exist, fall back to default (fareflow)
            log.warn("Schema '{}' not found or access denied, using default: {}", schema, TenantContext.SYSTEM_TENANT);
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("USE `" + TenantContext.SYSTEM_TENANT + "`");
                log.debug("Switched to default schema: {}", TenantContext.SYSTEM_TENANT);
            } catch (SQLException e2) {
                log.error("Error switching to default schema: {}", TenantContext.SYSTEM_TENANT, e2);
                throw e2;
            }
        }
        
        return connection;
    }
    
    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        log.debug("Releasing connection for tenant: {}", tenantIdentifier);
        connection.close();
    }
    
    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }
    
    @Override
    public boolean isUnwrappableAs(Class<?> unwrapType) {
        return MultiTenantConnectionProvider.class.isAssignableFrom(unwrapType);
    }
    
    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        if (isUnwrappableAs(unwrapType)) {
            return unwrapType.cast(this);
        }
        throw new IllegalArgumentException("Cannot unwrap to " + unwrapType);
    }
    
    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, this);
    }
    
    /**
     * Sanitize schema name to prevent SQL injection
     */
    private String sanitizeSchemaName(String tenantIdentifier) {
        if (tenantIdentifier == null || tenantIdentifier.isEmpty()) {
            return TenantContext.SYSTEM_TENANT;
        }
        
        // Only allow alphanumeric, underscore, and hyphen (but replace hyphen with underscore for MySQL compatibility)
        String sanitized = tenantIdentifier.toLowerCase()
                .replaceAll("[^a-z0-9_]", "");
        
        // Limit length (MySQL schema name limit is 64 chars)
        if (sanitized.length() > 63) {
            sanitized = sanitized.substring(0, 63);
        }
        
        return sanitized.isEmpty() ? TenantContext.SYSTEM_TENANT : sanitized;
    }
}
