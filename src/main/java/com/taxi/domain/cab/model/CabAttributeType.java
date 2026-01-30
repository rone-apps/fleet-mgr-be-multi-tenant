package com.taxi.domain.cab.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * CabAttributeType - Defines types of attributes that can be assigned to cabs
 * Examples: Airport License, Transponder, Van Type, Special Equipment, etc.
 * Admins can dynamically create new attribute types
 */
@Entity
@Table(name = "cab_attribute_type",
       uniqueConstraints = @UniqueConstraint(columnNames = {"attribute_code"}),
       indexes = {
           @Index(name = "idx_attr_type_active", columnList = "is_active"),
           @Index(name = "idx_attr_type_category", columnList = "category")
       })
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = "id")
public class CabAttributeType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Unique code identifier (e.g., "AIRPORT_LICENSE", "TRANSPONDER")
    @Column(name = "attribute_code", nullable = false, unique = true, length = 50)
    private String attributeCode;

    // Display name (e.g., "Airport License", "Transponder")
    @Column(name = "attribute_name", nullable = false, length = 100)
    private String attributeName;

    @Column(name = "description", length = 500)
    private String description;

    // Category for grouping (LICENSE, EQUIPMENT, TYPE, PERMIT)
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private AttributeCategory category;

    // Data type for validation (STRING, NUMBER, DATE, BOOLEAN)
    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 20)
    private DataType dataType;

    // Whether this attribute requires a value (or just presence/absence)
    @Column(name = "requires_value")
    @Builder.Default
    private boolean requiresValue = false;

    // Validation regex pattern (optional)
    @Column(name = "validation_pattern", length = 255)
    private String validationPattern;

    // Help text for users
    @Column(name = "help_text", length = 500)
    private String helpText;

    @Column(name = "is_active")
    @Builder.Default
    private boolean isActive = true;

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

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    /**
     * Attribute category for organization
     */
    public enum AttributeCategory {
        LICENSE("License/Permit"),
        EQUIPMENT("Equipment/Hardware"),
        TYPE("Vehicle Type/Classification"),
        PERMIT("Special Permits"),
        CERTIFICATION("Certifications");

        private final String displayName;

        AttributeCategory(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Data type for attribute values
     */
    public enum DataType {
        STRING("Text"),
        NUMBER("Number"),
        DATE("Date"),
        BOOLEAN("Yes/No");

        private final String displayName;

        DataType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
