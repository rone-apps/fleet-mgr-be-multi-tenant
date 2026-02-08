package com.taxi.domain.expense.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.taxi.domain.expense.model.ExpenseCategory.AppliesTo;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ExpenseCategoryRule - Stores attribute-based matching rules for expense categories
 * Supports two configuration modes:
 * 1. AUTO_MATCH: Automatically match cabs based on attribute criteria
 * 2. INDIVIDUAL_CONFIG: Allow manual selection with individual amounts per cab
 */
@Entity
@Table(name = "expense_category_rule",
       indexes = {
           @Index(name = "idx_ecr_category", columnList = "expense_category_id"),
           @Index(name = "idx_ecr_mode", columnList = "configuration_mode"),
           @Index(name = "idx_ecr_share_rule", columnList = "has_share_type_rule"),
           @Index(name = "idx_ecr_airport_rule", columnList = "has_airport_license_rule")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = "id")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ExpenseCategoryRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "expense_category_id", nullable = false)
    private ExpenseCategory expenseCategory;

    /**
     * Configuration mode determines how this rule is applied
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "configuration_mode", nullable = false, length = 20)
    private ConfigurationMode configurationMode;

    /**
     * Structured matching criteria (stored as JSON/JSONB)
     * Contains static attributes (share type, airport license, etc.)
     * and dynamic attributes (custom cab attributes)
     */
    @Column(name = "matching_criteria", columnDefinition = "jsonb")
    private String matchingCriteria; // JSON string

    /**
     * Indexed flags for common queries - improves performance
     */
    @Column(name = "has_share_type_rule")
    @Builder.Default
    private Boolean hasShareTypeRule = false;

    @Column(name = "has_airport_license_rule")
    @Builder.Default
    private Boolean hasAirportLicenseRule = false;

    @Column(name = "has_cab_shift_type_rule")
    @Builder.Default
    private Boolean hasCabShiftTypeRule = false;

    @Column(name = "has_cab_type_rule")
    @Builder.Default
    private Boolean hasCabTypeRule = false;

    @Column(name = "is_active")
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Configuration mode enum
     */
    public enum ConfigurationMode {
        AUTO_MATCH("Auto-Match Based on Criteria"),
        INDIVIDUAL_CONFIG("Individual Configuration Per Cab");

        private final String displayName;

        ConfigurationMode(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

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
     * Activate rule
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * Deactivate rule
     */
    public void deactivate() {
        this.isActive = false;
    }
}
