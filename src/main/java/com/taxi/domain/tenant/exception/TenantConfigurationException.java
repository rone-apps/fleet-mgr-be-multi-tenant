package com.taxi.domain.tenant.exception;

/**
 * Exception thrown when tenant configuration is missing or invalid.
 */
public class TenantConfigurationException extends RuntimeException {

    private final String tenantId;
    private final String configKey;

    public TenantConfigurationException(String message, String tenantId, String configKey) {
        super(message);
        this.tenantId = tenantId;
        this.configKey = configKey;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getConfigKey() {
        return configKey;
    }
}
