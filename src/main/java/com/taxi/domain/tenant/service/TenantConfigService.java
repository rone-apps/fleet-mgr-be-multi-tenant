package com.taxi.domain.tenant.service;

import com.taxi.domain.tenant.exception.TenantConfigurationException;
import com.taxi.domain.tenant.model.TenantConfig;
import com.taxi.infrastructure.multitenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Optional;

/**
 * Service for accessing tenant configuration.
 * Note: tenant_config table exists ONLY in the default schema (fareflow).
 * This service queries the default schema directly to avoid circular dependency with tenant resolution.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantConfigService {

    private final DataSource dataSource;
    private static final String DEFAULT_SCHEMA = "fareflow";

    /**
     * Get config for the current tenant.
     * Queries the default schema directly since tenant_config is a shared table.
     */
    @Cacheable(cacheNames = "tenant_config", key = "T(com.taxi.infrastructure.multitenancy.TenantContext).getCurrentTenant()")
    public Optional<TenantConfig> getCurrentTenantConfig() {
        String tenantId = TenantContext.getCurrentTenant();
        log.debug("Fetching config for tenant: {}", tenantId);
        return getTenantConfigFromDefaultSchema(tenantId);
    }

    /**
     * Get config for a specific tenant
     */
    @Cacheable(cacheNames = "tenant_config", key = "#tenantId")
    public Optional<TenantConfig> getTenantConfig(String tenantId) {
        return getTenantConfigFromDefaultSchema(tenantId);
    }

    /**
     * Query tenant_config from the default schema directly
     */
    private Optional<TenantConfig> getTenantConfigFromDefaultSchema(String tenantId) {
        String sql = "SELECT id, tenant_id, company_name, taxicaller_api_key, taxicaller_company_id, " +
                     "taxicaller_base_url, created_at, updated_at " +
                     "FROM " + DEFAULT_SCHEMA + ".tenant_config WHERE tenant_id = ?";

        log.info("Querying tenant_config for tenantId: {} with SQL: {}", tenantId, sql);

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, tenantId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String apiKey = rs.getString("taxicaller_api_key");
                    Integer companyId = rs.getObject("taxicaller_company_id", Integer.class);
                    log.info("Found tenant_config for {}: apiKey={}, companyId={}", 
                            tenantId, apiKey != null ? "***" : "null", companyId);
                    
                    TenantConfig config = TenantConfig.builder()
                            .id(rs.getLong("id"))
                            .tenantId(rs.getString("tenant_id"))
                            .companyName(rs.getString("company_name"))
                            .taxicallerApiKey(apiKey)
                            .taxicallerCompanyId(companyId)
                            .taxicallerBaseUrl(rs.getString("taxicaller_base_url"))
                            .build();
                    return Optional.of(config);
                } else {
                    log.warn("No tenant_config row found for tenantId: {}", tenantId);
                }
            }
        } catch (Exception e) {
            log.error("Error fetching tenant config for: {} - {}", tenantId, e.getMessage(), e);
        }
        
        return Optional.empty();
    }

    /**
     * Get TaxiCaller API key for current tenant
     */
    public String getTaxicallerApiKey() {
        String tenantId = TenantContext.getCurrentTenant();
        return getCurrentTenantConfig()
                .map(TenantConfig::getTaxicallerApiKey)
                .filter(key -> key != null && !key.isBlank())
                .orElseThrow(() -> new TenantConfigurationException(
                        "TaxiCaller API key is not configured for this company. Please contact your administrator.",
                        tenantId, "taxicaller_api_key"));
    }

    /**
     * Get TaxiCaller company ID for current tenant
     */
    public Integer getTaxicallerCompanyId() {
        String tenantId = TenantContext.getCurrentTenant();
        return getCurrentTenantConfig()
                .map(TenantConfig::getTaxicallerCompanyId)
                .orElseThrow(() -> new TenantConfigurationException(
                        "TaxiCaller company ID is not configured for this company. Please contact your administrator.",
                        tenantId, "taxicaller_company_id"));
    }

    /**
     * Get TaxiCaller base URL for current tenant (with default fallback)
     */
    public String getTaxicallerBaseUrl() {
        return getCurrentTenantConfig()
                .map(TenantConfig::getTaxicallerBaseUrl)
                .orElse("https://api.taxicaller.net");
    }

    /**
     * Save or update tenant configuration
     */
    @CacheEvict(cacheNames = "tenant_config", key = "#tenantId")
    public TenantConfig saveTenantConfig(String tenantId, String companyName, 
                                          String apiKey, Integer companyId, String baseUrl) {
        Optional<TenantConfig> existing = getTenantConfigFromDefaultSchema(tenantId);
        
        if (existing.isPresent()) {
            return updateTenantConfig(tenantId, companyName, apiKey, companyId, baseUrl);
        } else {
            return insertTenantConfig(tenantId, companyName, apiKey, companyId, baseUrl);
        }
    }

    private TenantConfig insertTenantConfig(String tenantId, String companyName,
                                             String apiKey, Integer companyId, String baseUrl) {
        String sql = "INSERT INTO " + DEFAULT_SCHEMA + ".tenant_config " +
                     "(tenant_id, company_name, taxicaller_api_key, taxicaller_company_id, taxicaller_base_url) " +
                     "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            ps.setString(1, tenantId);
            ps.setString(2, companyName);
            ps.setString(3, apiKey);
            ps.setObject(4, companyId);
            ps.setString(5, baseUrl != null ? baseUrl : "https://api.taxicaller.net");
            
            ps.executeUpdate();
            
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    log.info("Created tenant_config for tenant: {}", tenantId);
                    return getTenantConfigFromDefaultSchema(tenantId).orElse(null);
                }
            }
        } catch (Exception e) {
            log.error("Error inserting tenant config for: {}", tenantId, e);
            throw new RuntimeException("Failed to create tenant configuration", e);
        }
        return null;
    }

    private TenantConfig updateTenantConfig(String tenantId, String companyName,
                                             String apiKey, Integer companyId, String baseUrl) {
        String sql = "UPDATE " + DEFAULT_SCHEMA + ".tenant_config SET " +
                     "company_name = ?, taxicaller_api_key = ?, taxicaller_company_id = ?, " +
                     "taxicaller_base_url = ?, updated_at = NOW() WHERE tenant_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, companyName);
            ps.setString(2, apiKey);
            ps.setObject(3, companyId);
            ps.setString(4, baseUrl != null ? baseUrl : "https://api.taxicaller.net");
            ps.setString(5, tenantId);
            
            int updated = ps.executeUpdate();
            if (updated > 0) {
                log.info("Updated tenant_config for tenant: {}", tenantId);
                return getTenantConfigFromDefaultSchema(tenantId).orElse(null);
            }
        } catch (Exception e) {
            log.error("Error updating tenant config for: {}", tenantId, e);
            throw new RuntimeException("Failed to update tenant configuration", e);
        }
        return null;
    }
}
