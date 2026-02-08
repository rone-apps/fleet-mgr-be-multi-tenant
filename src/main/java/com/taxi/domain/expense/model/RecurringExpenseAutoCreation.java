package com.taxi.domain.expense.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * RecurringExpenseAutoCreation - Audit trail for auto-created recurring expenses
 * Tracks which expenses were created via auto-matching or bulk configuration
 * Preserves snapshot of matching criteria at time of creation
 */
@Entity
@Table(name = "recurring_expense_auto_creation",
       indexes = {
           @Index(name = "idx_reac_rule", columnList = "expense_category_rule_id"),
           @Index(name = "idx_reac_expense", columnList = "recurring_expense_id"),
           @Index(name = "idx_reac_type", columnList = "creation_type"),
           @Index(name = "idx_reac_created", columnList = "created_at")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = "id")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RecurringExpenseAutoCreation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_category_rule_id", nullable = false)
    private ExpenseCategoryRule categoryRule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurring_expense_id", nullable = false)
    private RecurringExpense recurringExpense;

    /**
     * Type of creation
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "creation_type", nullable = false, length = 20)
    private CreationType creationType;

    /**
     * Snapshot of matching criteria and cab attributes at time of creation
     * Stored as JSON for historical reference
     */
    @Column(name = "matching_snapshot", columnDefinition = "jsonb")
    private String matchingSnapshot;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    /**
     * Creation type enum
     */
    public enum CreationType {
        AUTO_MATCHED("Auto-matched based on rules"),
        BULK_CONFIGURED("Bulk configured with individual amounts"),
        MANUAL("Manually created");

        private final String displayName;

        CreationType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
