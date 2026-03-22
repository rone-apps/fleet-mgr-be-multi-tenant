package com.taxi.domain.drivertrip.model;

import com.taxi.domain.cab.model.Cab;
import com.taxi.domain.driver.model.Driver;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Entity representing a driver trip/job imported from TaxiCaller.
 * Stores individual trip records for all drivers (not just account jobs).
 */
@Entity
@Table(name = "driver_trips",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_driver_trip_job",
            columnNames = {"job_code", "driver_id", "cab_id", "trip_date"})
    },
    indexes = {
        @Index(name = "idx_dtrip_driver", columnList = "driver_id"),
        @Index(name = "idx_dtrip_cab", columnList = "cab_id"),
        @Index(name = "idx_dtrip_date", columnList = "trip_date"),
        @Index(name = "idx_dtrip_job_code", columnList = "job_code"),
        @Index(name = "idx_dtrip_driver_username", columnList = "driver_username"),
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverTrip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_code", length = 50)
    private String jobCode;

    @Column(name = "driver_username", length = 50)
    private String driverUsername;

    @Column(name = "driver_name", length = 200)
    private String driverName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cab_id")
    private Cab cab;

    @Column(name = "trip_date", nullable = false)
    private LocalDate tripDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "pickup_address", length = 500)
    private String pickupAddress;

    @Column(name = "dropoff_address", length = 500)
    private String dropoffAddress;

    @Column(name = "passenger_name", length = 200)
    private String passengerName;

    @Column(name = "account_number", length = 100)
    private String accountNumber;

    @Column(name = "company_id", length = 50)
    private String companyId;

    @Column(name = "fare_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal fareAmount = BigDecimal.ZERO;

    @Column(name = "tip_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal tipAmount = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public BigDecimal getTotalAmount() {
        BigDecimal fare = fareAmount != null ? fareAmount : BigDecimal.ZERO;
        BigDecimal tip = tipAmount != null ? tipAmount : BigDecimal.ZERO;
        return fare.add(tip);
    }
}
