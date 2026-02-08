package com.taxi.domain.profile.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.taxi.domain.cab.model.CabAttributeType;
import jakarta.persistence.*;
import lombok.*;

/**
 * ShiftProfileAttribute - Associates dynamic attributes with shift profiles
 *
 * Links a shift profile to a specific attribute type with matching requirements:
 * - isRequired = true: Shift MUST have this attribute (positive match)
 * - isRequired = false: Shift MUST NOT have this attribute (negative match)
 * - expectedValue = null: Any value acceptable (if attribute exists)
 * - expectedValue = specific value: Attribute must have this exact value
 *
 * Example:
 * - PREMIUM_SEDAN_VOTING requires TRANSPONDER attribute (isRequired=true)
 * - HANDICAP_VAN_VOTING requires WHEELCHAIR_LIFT attribute (isRequired=true)
 * - STANDARD_SEDAN requires NOT having SPECIALTY_PERMIT (isRequired=false)
 */
@Entity
@Table(name = "shift_profile_attribute",
       uniqueConstraints = @UniqueConstraint(columnNames = {"profile_id", "attribute_type_id"}),
       indexes = {
           @Index(name = "idx_profile_attr_profile", columnList = "profile_id"),
           @Index(name = "idx_profile_attr_type", columnList = "attribute_type_id")
       })
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"profile"})
@EqualsAndHashCode(of = "id")
public class ShiftProfileAttribute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private ShiftProfile profile;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "attribute_type_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private CabAttributeType attributeType;

    @Column(name = "is_required")
    @Builder.Default
    private Boolean isRequired = true;  // true = must have, false = must not have

    @Column(name = "expected_value", length = 255)
    private String expectedValue;  // Specific value match (null = any value acceptable)

    // ============================================================================
    // Business Methods
    // ============================================================================

    /**
     * Check if a shift attribute value matches this requirement
     *
     * @param attributeValue The attribute value to check (null = attribute not present)
     * @return true if the attribute value matches this requirement
     */
    public boolean matches(String attributeValue) {
        // If attribute is not present (null)
        if (attributeValue == null) {
            // Required attributes must be present - so this is a non-match
            if (Boolean.TRUE.equals(isRequired)) {
                return false;
            }
            // Non-required attributes (must not have) - null value means requirement met
            return true;
        }

        // Attribute is present
        // If attribute is required to NOT be present, this is a non-match
        if (Boolean.FALSE.equals(isRequired)) {
            return false;
        }

        // Attribute is present and required
        // Check if expected value constraint exists
        if (expectedValue != null) {
            return expectedValue.equals(attributeValue);
        }

        // No expected value constraint, any value acceptable
        return true;
    }
}
