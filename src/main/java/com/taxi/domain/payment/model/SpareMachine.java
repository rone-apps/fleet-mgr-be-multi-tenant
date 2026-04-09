package com.taxi.domain.payment.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "spare_machine",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"machine_name"}),
           @UniqueConstraint(columnNames = {"virtual_cab_id"}),
           @UniqueConstraint(columnNames = {"merchant_number"})
       },
       indexes = {
           @Index(name = "idx_virtual_cab", columnList = "virtual_cab_id"),
           @Index(name = "idx_merchant", columnList = "merchant_number")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class SpareMachine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "machine_name", nullable = false, unique = true, length = 50)
    private String machineName;

    @Column(name = "virtual_cab_id", nullable = false, unique = true)
    private Integer virtualCabId;

    @Column(name = "merchant_number", nullable = false, unique = true, length = 50)
    private String merchantNumber;

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
}
