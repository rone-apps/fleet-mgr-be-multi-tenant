package com.taxi.domain.account.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "statement_audit_log")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatementAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "statement_id", nullable = false)
    private Invoice statement;

    @Column(name = "change_type", nullable = false, length = 50)
    private String changeType; // CREATED, POSTED, LOCKED, PAID, RECALLED, AMENDED

    @Column(name = "previous_status", length = 20)
    private String previousStatus;

    @Column(name = "new_status", length = 20)
    private String newStatus;

    @Column(name = "change_description", columnDefinition = "TEXT")
    private String changeDescription;

    @Column(name = "changed_fields", columnDefinition = "JSON")
    private String changedFields;

    @Column(name = "changed_by", nullable = false)
    private Long changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @PrePersist
    protected void onCreate() {
        changedAt = LocalDateTime.now();
    }
}
