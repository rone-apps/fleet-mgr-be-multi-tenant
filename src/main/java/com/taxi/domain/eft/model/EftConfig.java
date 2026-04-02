package com.taxi.domain.eft.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "eft_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EftConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "originator_id", nullable = false, length = 10)
    private String originatorId;

    @Column(name = "originator_short_name", nullable = false, length = 15)
    private String originatorShortName;

    @Column(name = "originator_long_name", nullable = false, length = 30)
    private String originatorLongName;

    @Column(name = "processing_centre", length = 5)
    private String processingCentre;

    @Column(name = "currency_code", nullable = false, length = 3)
    @Builder.Default
    private String currencyCode = "CAD";

    @Column(name = "transaction_code", nullable = false, length = 3)
    @Builder.Default
    private String transactionCode = "200";

    @Column(name = "return_institution_id", nullable = false, length = 4)
    private String returnInstitutionId;

    @Column(name = "return_transit_number", nullable = false, length = 5)
    private String returnTransitNumber;

    @Column(name = "return_account_number", nullable = false, length = 12)
    private String returnAccountNumber;

    @Column(name = "file_creation_number", nullable = false)
    @Builder.Default
    private Integer fileCreationNumber = 1;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Get the next file creation number and increment.
     * Wraps around at 9999.
     */
    public int getNextFileCreationNumber() {
        int current = fileCreationNumber;
        fileCreationNumber = (fileCreationNumber % 9999) + 1;
        return current;
    }
}
