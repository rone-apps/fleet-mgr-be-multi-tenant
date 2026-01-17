package com.taxi.domain.account.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.taxi.domain.cab.model.Cab;
import com.taxi.domain.driver.model.Driver;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "account_charge",
       uniqueConstraints = {
           @UniqueConstraint(
               name = "uk_account_charge_trip",
               columnNames = {"account_id", "cab_id", "driver_id", "trip_date", "start_time", "job_code"}
           ),
       },
       indexes = {
           @Index(name = "idx_charge_customer", columnList = "customer_id"),
           @Index(name = "idx_charge_date", columnList = "trip_date"),
           @Index(name = "idx_charge_job_code", columnList = "job_code"),
           @Index(name = "idx_charge_paid", columnList = "is_paid"),
           @Index(name = "idx_charge_cab", columnList = "cab_id"),
           @Index(name = "idx_charge_driver", columnList = "driver_id"),
           @Index(name = "idx_charge_account_id", columnList = "account_id"),
           @Index(name = "idx_charge_invoice", columnList = "invoice_id")
       })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AccountCharge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false, length = 50)
    private String accountId;

    @Column(name = "sub_account", length = 50)
    private String subAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private AccountCustomer accountCustomer;

    @Column(name = "job_code", length = 50)
    private String jobCode;

    @Column(name = "trip_date", nullable = false)
    private LocalDate tripDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "pickup_address", length = 300)
    private String pickupAddress;

    @Column(name = "dropoff_address", length = 300)
    private String dropoffAddress;

    @Column(name = "passenger_name", length = 100)
    private String passengerName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cab_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Cab cab;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Driver driver;

    @Column(name = "fare_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal fareAmount;

    @Column(name = "tip_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal tipAmount = BigDecimal.ZERO;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "is_paid", nullable = false)
    @Builder.Default
    private boolean paid = false;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Column(name = "invoice_number", length = 50)
    private String invoiceNumber;

    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "created_at")
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

    // Business methods
    public BigDecimal getTotalAmount() {
        return fareAmount.add(tipAmount != null ? tipAmount : BigDecimal.ZERO);
    }

    public void markAsPaid(String invoiceNumber) {
        this.paid = true;
        this.paidDate = LocalDate.now();
        this.invoiceNumber = invoiceNumber;
    }

    public void markAsUnpaid() {
        this.paid = false;
        this.paidDate = null;
        this.invoiceNumber = null;
    }

    public Long getTripDurationMinutes() {
        if (startTime != null && endTime != null) {
            return ChronoUnit.MINUTES.between(startTime, endTime);
        }
        return null;
    }

    public boolean isOverdue() {
        if (!paid && tripDate != null) {
            LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
            return tripDate.isBefore(thirtyDaysAgo);
        }
        return false;
    }
}