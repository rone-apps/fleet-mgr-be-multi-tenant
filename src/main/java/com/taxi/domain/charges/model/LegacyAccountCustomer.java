package com.taxi.domain.charges.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Legacy account customer entity.
 * Temporary table for tenants transitioning to modern account_customer system.
 * IDs are different from modern account_customer, so this is a separate table.
 */
@Entity
@Table(name = "legacy_account_customer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LegacyAccountCustomer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "db_id", unique = true)
    private Long dbId;  // Original database ID from old system

    @Column(name = "customer_id", nullable = false, unique = true, length = 255)
    private String customerId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 255)
    private String street;

    @Column(length = 255)
    private String city;

    @Column(length = 255)
    private String province;

    @Column(name = "postal_code", length = 255)
    private String postalCode;

    @Column(length = 255)
    private String contact;

    @Column(length = 255)
    private String phone;

    @Column(length = 255)
    private String email;

    @Column(name = "credit_limit")
    private Double creditLimit;

    @Column(name = "date")
    private LocalDate date;

    @Column(length = 255)
    private String notes;
}
