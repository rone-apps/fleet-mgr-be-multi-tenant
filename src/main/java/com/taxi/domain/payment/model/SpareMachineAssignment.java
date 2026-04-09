package com.taxi.domain.payment.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "spare_machine_assignment",
       indexes = {
           @Index(name = "idx_real_cab_time", columnList = "real_cab_number, assigned_at, returned_at"),
           @Index(name = "idx_active_assignments", columnList = "returned_at")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class SpareMachineAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "spare_machine_id", nullable = false)
    private Long spareMachineId;

    @Column(name = "real_cab_number", nullable = false)
    private Integer realCabNumber;

    @Column(name = "shift", length = 10)
    private String shift;  // "Day", "Night", or "BOTH"

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @Column(name = "returned_at")
    private LocalDateTime returnedAt;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_by", length = 255)
    private String createdBy;

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

    public boolean isActive(LocalDateTime timestamp) {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
        return assignedAt.compareTo(timestamp) <= 0 &&
               (returnedAt == null || returnedAt.compareTo(timestamp) > 0);
    }

    public boolean isCurrentlyActive() {
        return isActive(LocalDateTime.now());
    }
}
