package com.taxi.web.controller;

import com.taxi.domain.tenant.dto.TenantConfigRequest;
import com.taxi.domain.tenant.model.TenantConfig;
import com.taxi.domain.tenant.service.TenantConfigService;
import com.taxi.infrastructure.multitenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/tenant-config")
@RequiredArgsConstructor
public class TenantConfigController {

    private final TenantConfigService tenantConfigService;

    /**
     * Get current tenant's configuration
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getCurrentConfig() {
        Map<String, Object> response = new HashMap<>();
        String tenantId = TenantContext.getCurrentTenant();
        
        return tenantConfigService.getCurrentTenantConfig()
                .map(config -> {
                    response.put("success", true);
                    response.put("tenantId", config.getTenantId());
                    response.put("companyName", config.getCompanyName());
                    response.put("taxicallerApiKey", maskApiKey(config.getTaxicallerApiKey()));
                    response.put("taxicallerCompanyId", config.getTaxicallerCompanyId());
                    response.put("taxicallerBaseUrl", config.getTaxicallerBaseUrl());
                    response.put("configured", config.hasTaxicallerConfig());
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    response.put("success", false);
                    response.put("tenantId", tenantId);
                    response.put("configured", false);
                    response.put("message", "No configuration found for tenant");
                    return ResponseEntity.ok(response);
                });
    }

    /**
     * Save or update TaxiCaller configuration for a tenant
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> saveConfig(@RequestBody TenantConfigRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        String tenantId = request.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = TenantContext.getCurrentTenant();
        }

        try {
            TenantConfig saved = tenantConfigService.saveTenantConfig(
                    tenantId,
                    request.getCompanyName(),
                    request.getTaxicallerApiKey(),
                    request.getTaxicallerCompanyId(),
                    request.getTaxicallerBaseUrl()
            );

            if (saved != null) {
                response.put("success", true);
                response.put("message", "Configuration saved successfully");
                response.put("tenantId", saved.getTenantId());
                response.put("companyName", saved.getCompanyName());
                response.put("taxicallerApiKey", maskApiKey(saved.getTaxicallerApiKey()));
                response.put("taxicallerCompanyId", saved.getTaxicallerCompanyId());
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Failed to save configuration");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error saving configuration: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get configuration for a specific tenant (super admin only)
     */
    @GetMapping("/{tenantId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getConfigByTenant(@PathVariable String tenantId) {
        Map<String, Object> response = new HashMap<>();
        
        return tenantConfigService.getTenantConfig(tenantId)
                .map(config -> {
                    response.put("success", true);
                    response.put("tenantId", config.getTenantId());
                    response.put("companyName", config.getCompanyName());
                    response.put("taxicallerApiKey", maskApiKey(config.getTaxicallerApiKey()));
                    response.put("taxicallerCompanyId", config.getTaxicallerCompanyId());
                    response.put("taxicallerBaseUrl", config.getTaxicallerBaseUrl());
                    response.put("configured", config.hasTaxicallerConfig());
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    response.put("success", false);
                    response.put("tenantId", tenantId);
                    response.put("configured", false);
                    response.put("message", "No configuration found for tenant");
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
                });
    }

    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 8) {
            return apiKey;
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}
