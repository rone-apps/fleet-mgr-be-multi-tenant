package com.taxi.domain.tenant.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Tenant-specific configuration settings.
 * Stores API credentials and other tenant-specific settings.
 */
@Entity
@Table(name = "tenant_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, unique = true, length = 63)
    private String tenantId;

    @Column(name = "company_name", length = 100)
    private String companyName;

    // TaxiCaller API Configuration
    @Column(name = "taxicaller_api_key", length = 100)
    private String taxicallerApiKey;

    @Column(name = "taxicaller_company_id")
    private Integer taxicallerCompanyId;

    @Column(name = "taxicaller_base_url", length = 255)
    @Builder.Default
    private String taxicallerBaseUrl = "https://api.taxicaller.net";

    // Audit fields
    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean hasTaxicallerConfig() {
        return taxicallerApiKey != null && !taxicallerApiKey.isBlank()
                && taxicallerCompanyId != null;
    }
}
