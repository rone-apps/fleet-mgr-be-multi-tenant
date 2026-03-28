package com.taxi.domain.airport.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Stores per-driver breakdown of airport trips.
 * Each row = one driver's trips for one hour on one day for one cab.
 * Populated during CSV upload by matching hourly trips to driver_shifts.
 */
@Entity
@Table(name = "airport_trip_driver",
       indexes = {
           @Index(name = "idx_atd_airport_trip", columnList = "airport_trip_id"),
           @Index(name = "idx_atd_driver_number", columnList = "driver_number"),
           @Index(name = "idx_atd_trip_date", columnList = "trip_date"),
           @Index(name = "idx_atd_driver_date", columnList = "driver_number, trip_date")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AirportTripDriver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "airport_trip_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private AirportTrip airportTrip;

    @Column(name = "cab_number", length = 50, nullable = false)
    private String cabNumber;

    @Column(name = "driver_number", length = 50, nullable = false)
    private String driverNumber;

    @Column(name = "trip_date", nullable = false)
    private LocalDate tripDate;

    @Column(name = "hour", nullable = false)
    private Integer hour;

    @Column(name = "trip_count", nullable = false)
    private Integer tripCount;

    @Column(name = "total_daily_trips")
    private Integer totalDailyTrips;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_method", length = 20, nullable = false)
    private AssignmentMethod assignmentMethod;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum AssignmentMethod {
        MIDPOINT,        // Driver was active at hour:30
        CLOSEST,         // No driver at midpoint; assigned to closest shift by time
        OWNER_FALLBACK   // No driver shifts found; assigned to cab/shift owner
    }
}
