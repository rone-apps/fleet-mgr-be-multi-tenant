package com.taxi.domain.shift.model;

import com.taxi.domain.driver.model.Driver;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing ownership history of a shift
 * Maintains complete audit trail of ownership transfers
 */
@Entity
@Table(name = "shift_ownership", indexes = {
        @Index(name = "idx_shift_owner", columnList = "shift_id, owner_id"),
        @Index(name = "idx_ownership_dates", columnList = "shift_id, start_date, end_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"shift", "owner", "transferredTo"})
@EqualsAndHashCode(of = "id")
public class ShiftOwnership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id", nullable = false)
    private CabShift shift;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Driver owner;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "acquisition_type", length = 30)
    private AcquisitionType acquisitionType;

    @Column(name = "acquisition_price", precision = 10, scale = 2)
    private BigDecimal acquisitionPrice;

    @Column(name = "sale_price", precision = 10, scale = 2)
    private BigDecimal salePrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transferred_to_driver_id")
    private Driver transferredTo;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Business logic: Check if this is the current ownership (no end date)
     */
    public boolean isCurrent() {
        return endDate == null;
    }

    /**
     * Business logic: Close this ownership record (when selling/transferring)
     */
    public void close(LocalDate endDate, Driver newOwner, BigDecimal salePrice) {
        if (!isCurrent()) {
            throw new IllegalStateException("Cannot close ownership that is already closed");
        }
        this.endDate = endDate;
        this.transferredTo = newOwner;
        this.salePrice = salePrice;
    }

    public enum AcquisitionType {
        PURCHASE,           // Bought from another owner
        INITIAL_ASSIGNMENT, // Initial assignment when shift was created
        TRANSFER,           // Transferred from another owner
        INHERITANCE         // Inherited ownership
    }
}
