package com.taxi.domain.moneris;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Stores Moneris Go Portal credentials per cab/merchant terminal.
 * Each cab can have its own store_id and api_token.
 */
@Entity
@Table(name = "moneris_config",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_moneris_cab_shift_merchant",
                        columnNames = {"cab_number", "shift", "merchant_number"})
        },
        indexes = {
                @Index(name = "idx_moneris_cab", columnList = "cab_number"),
                @Index(name = "idx_moneris_store", columnList = "moneris_store_id"),
                @Index(name = "idx_moneris_merchant", columnList = "merchant_number")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonerisConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cab_number", nullable = false, length = 20)
    private String cabNumber;

    @Column(name = "shift", nullable = false, length = 10)
    @Builder.Default
    private String shift = "BOTH";

    @Column(name = "merchant_number", nullable = false, length = 100)
    private String merchantNumber;

    @Column(name = "moneris_store_id", nullable = false, length = 50)
    private String monerisStoreId;

    @Column(name = "moneris_api_token", nullable = false, length = 100)
    private String monerisApiToken;

    @Column(name = "moneris_environment", nullable = false, length = 10)
    @Builder.Default
    private String monerisEnvironment = "PROD";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}
