package com.taxi.domain.cab.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Tracks history of cab shift type changes (SINGLE <-> DOUBLE)
 */
@Entity
@Table(name = "cab_shift_type_history",
       indexes = {
           @Index(name = "idx_cab_shift_type_history_cab", columnList = "cab_id"),
           @Index(name = "idx_cab_shift_type_history_changed_at", columnList = "changed_at")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CabShiftTypeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cab_id", nullable = false)
    private Long cabId;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_shift_type", length = 20)
    private CabShiftType oldShiftType;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_shift_type", nullable = false, length = 20)
    private CabShiftType newShiftType;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Column(name = "changed_by", length = 255)
    private String changedBy;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
