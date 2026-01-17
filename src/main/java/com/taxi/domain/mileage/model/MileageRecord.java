package com.taxi.domain.mileage.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing mileage records imported from CSV.
 * CSV format: cab, A, B, C, Driver number, logon_time, Logoff_time
 */
@Entity
@Table(name = "mileage_records",
       indexes = {
           @Index(name = "idx_mileage_cab_number", columnList = "cab_number"),
           @Index(name = "idx_mileage_driver_number", columnList = "driver_number"),
           @Index(name = "idx_mileage_logon_time", columnList = "logon_time"),
           @Index(name = "idx_mileage_upload_batch", columnList = "upload_batch_id")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MileageRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cab_number", length = 50, nullable = false)
    private String cabNumber;

    @Column(name = "driver_number", length = 50)
    private String driverNumber;

    @Column(name = "logon_time", nullable = false)
    private LocalDateTime logonTime;

    @Column(name = "logoff_time")
    private LocalDateTime logoffTime;

    // A = Flag fall / Tariff 1 mileage
    @Column(name = "mileage_a", precision = 10, scale = 3)
    private BigDecimal mileageA;

    // B = Tariff 2 mileage
    @Column(name = "mileage_b", precision = 10, scale = 3)
    private BigDecimal mileageB;

    // C = Paid mileage
    @Column(name = "mileage_c", precision = 10, scale = 3)
    private BigDecimal mileageC;

    // Calculated total mileage
    @Column(name = "total_mileage", precision = 10, scale = 3)
    private BigDecimal totalMileage;

    // Shift duration in hours
    @Column(name = "shift_hours", precision = 5, scale = 2)
    private BigDecimal shiftHours;

    // Upload tracking
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
}
