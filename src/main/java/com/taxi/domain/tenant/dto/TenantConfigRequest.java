package com.taxi.domain.tenant.dto;

import lombok.Data;

@Data
public class TenantConfigRequest {
    private String tenantId;
    private String companyName;
    private String taxicallerApiKey;
    private Integer taxicallerCompanyId;
    private String taxicallerBaseUrl;
}
