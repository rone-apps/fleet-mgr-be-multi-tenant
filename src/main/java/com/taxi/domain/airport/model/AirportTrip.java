package com.taxi.domain.airport.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing airport trips imported from CSV.
 * CSV format: Vehicle,year,month,Day,0,1,2,...,23,Grand Total
 * 
 * Trips are split by shift:
 * - DAY shift: 4am - 4pm (hours 4-15)
 * - NIGHT shift: 4pm - 4am (hours 16-23, 0-3)
 */
@Entity
@Table(name = "airport_trips",
       indexes = {
           @Index(name = "idx_airport_cab_number", columnList = "cab_number"),
           @Index(name = "idx_airport_trip_date", columnList = "trip_date"),
           @Index(name = "idx_airport_shift", columnList = "shift"),
           @Index(name = "idx_airport_upload_batch", columnList = "upload_batch_id")
       },
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_airport_cab_shift_date", columnNames = {"cab_number", "shift", "trip_date"})
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AirportTrip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cab_number", length = 50, nullable = false)
    private String cabNumber;

    @Column(name = "shift", length = 10, nullable = false)
    private String shift; // "DAY" or "NIGHT"

    @Column(name = "driver_number", length = 50)
    private String driverNumber;

    @Column(name = "vehicle_name", length = 100)
    private String vehicleName;

    @Column(name = "trip_date", nullable = false)
    private LocalDate tripDate;

    @Column(name = "hour_00") private Integer hour00 = 0;
    @Column(name = "hour_01") private Integer hour01 = 0;
    @Column(name = "hour_02") private Integer hour02 = 0;
    @Column(name = "hour_03") private Integer hour03 = 0;
    @Column(name = "hour_04") private Integer hour04 = 0;
    @Column(name = "hour_05") private Integer hour05 = 0;
    @Column(name = "hour_06") private Integer hour06 = 0;
    @Column(name = "hour_07") private Integer hour07 = 0;
    @Column(name = "hour_08") private Integer hour08 = 0;
    @Column(name = "hour_09") private Integer hour09 = 0;
    @Column(name = "hour_10") private Integer hour10 = 0;
    @Column(name = "hour_11") private Integer hour11 = 0;
    @Column(name = "hour_12") private Integer hour12 = 0;
    @Column(name = "hour_13") private Integer hour13 = 0;
    @Column(name = "hour_14") private Integer hour14 = 0;
    @Column(name = "hour_15") private Integer hour15 = 0;
    @Column(name = "hour_16") private Integer hour16 = 0;
    @Column(name = "hour_17") private Integer hour17 = 0;
    @Column(name = "hour_18") private Integer hour18 = 0;
    @Column(name = "hour_19") private Integer hour19 = 0;
    @Column(name = "hour_20") private Integer hour20 = 0;
    @Column(name = "hour_21") private Integer hour21 = 0;
    @Column(name = "hour_22") private Integer hour22 = 0;
    @Column(name = "hour_23") private Integer hour23 = 0;

    @Column(name = "grand_total", nullable = false)
    private Integer grandTotal = 0;

    @Column(name = "upload_batch_id", length = 100)
    private String uploadBatchId;

    @Column(name = "upload_filename", length = 255)
    private String uploadFilename;

    @Column(name = "upload_date")
    private LocalDateTime uploadDate;

    @Column(name = "uploaded_by", length = 100)
    private String uploadedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Determine shift based on hour.
     * DAY: 4am-4pm (hours 4-15)
     * NIGHT: 4pm-4am (hours 16-23, 0-3)
     */
    public static String getShiftForHour(int hour) {
        if (hour >= 4 && hour < 16) {
            return "DAY";
        } else {
            return "NIGHT";
        }
    }

    /**
     * Check if this hour belongs to DAY shift
     */
    public static boolean isDayShift(int hour) {
        return hour >= 4 && hour < 16;
    }

    /**
     * Check if this hour belongs to NIGHT shift
     */
    public static boolean isNightShift(int hour) {
        return hour >= 16 || hour < 4;
    }

    public void setTripsByHour(int hour, Integer count) {
        int value = count != null ? count : 0;
        switch (hour) {
            case 0 -> hour00 = value;
            case 1 -> hour01 = value;
            case 2 -> hour02 = value;
            case 3 -> hour03 = value;
            case 4 -> hour04 = value;
            case 5 -> hour05 = value;
            case 6 -> hour06 = value;
            case 7 -> hour07 = value;
            case 8 -> hour08 = value;
            case 9 -> hour09 = value;
            case 10 -> hour10 = value;
            case 11 -> hour11 = value;
            case 12 -> hour12 = value;
            case 13 -> hour13 = value;
            case 14 -> hour14 = value;
            case 15 -> hour15 = value;
            case 16 -> hour16 = value;
            case 17 -> hour17 = value;
            case 18 -> hour18 = value;
            case 19 -> hour19 = value;
            case 20 -> hour20 = value;
            case 21 -> hour21 = value;
            case 22 -> hour22 = value;
            case 23 -> hour23 = value;
        }
    }

    public Integer getTripsByHour(int hour) {
        return switch (hour) {
            case 0 -> hour00 != null ? hour00 : 0;
            case 1 -> hour01 != null ? hour01 : 0;
            case 2 -> hour02 != null ? hour02 : 0;
            case 3 -> hour03 != null ? hour03 : 0;
            case 4 -> hour04 != null ? hour04 : 0;
            case 5 -> hour05 != null ? hour05 : 0;
            case 6 -> hour06 != null ? hour06 : 0;
            case 7 -> hour07 != null ? hour07 : 0;
            case 8 -> hour08 != null ? hour08 : 0;
            case 9 -> hour09 != null ? hour09 : 0;
            case 10 -> hour10 != null ? hour10 : 0;
            case 11 -> hour11 != null ? hour11 : 0;
            case 12 -> hour12 != null ? hour12 : 0;
            case 13 -> hour13 != null ? hour13 : 0;
            case 14 -> hour14 != null ? hour14 : 0;
            case 15 -> hour15 != null ? hour15 : 0;
            case 16 -> hour16 != null ? hour16 : 0;
            case 17 -> hour17 != null ? hour17 : 0;
            case 18 -> hour18 != null ? hour18 : 0;
            case 19 -> hour19 != null ? hour19 : 0;
            case 20 -> hour20 != null ? hour20 : 0;
            case 21 -> hour21 != null ? hour21 : 0;
            case 22 -> hour22 != null ? hour22 : 0;
            case 23 -> hour23 != null ? hour23 : 0;
            default -> 0;
        };
    }
}
