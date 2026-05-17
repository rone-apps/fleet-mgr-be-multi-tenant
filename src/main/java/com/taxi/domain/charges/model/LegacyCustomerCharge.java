package com.taxi.domain.charges.model;

import com.taxi.domain.cab.model.Cab;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Legacy customer charge entity.
 * Temporary table for tenants transitioning to modern account_charge system.
 * References legacy_account_customer and legacy_driver (not account_customer/driver)
 * because IDs differ between systems.
 * Cab reference is kept for historical data but nullable (not used for charge attribution).
 */
@Entity
@Table(name = "legacy_customer_charge")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LegacyCustomerCharge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double amount;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "payment")
    private Double payment;

    // Cab reference - no FK constraint (cab IDs from legacy system may not exist)
    @Column(name = "cab_id")
    private Long cabId;  // Changed from Cab entity to Long to avoid FK creation

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", referencedColumnName = "db_id")
    private LegacyAccountCustomer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private LegacyDriver driver;

    @Column(length = 255)
    private String notes;

    @Column(length = 255)
    private String type;
}
