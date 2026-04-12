package com.taxi.domain.drivertrip.repository;

import com.taxi.domain.drivertrip.model.DriverTrip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DriverTripRepository extends JpaRepository<DriverTrip, Long> {

    @EntityGraph(attributePaths = {"driver", "cab"})
    Page<DriverTrip> findAllBy(Pageable pageable);

    /**
     * Find existing trip by unique constraint fields (for duplicate detection)
     */
    @Query("SELECT dt FROM DriverTrip dt WHERE dt.jobCode = :jobCode " +
           "AND (dt.driver.id = :driverId OR (dt.driver IS NULL AND :driverId IS NULL)) " +
           "AND (dt.cab.id = :cabId OR (dt.cab IS NULL AND :cabId IS NULL)) " +
           "AND dt.tripDate = :tripDate")
    Optional<DriverTrip> findByUniqueConstraint(
            @Param("jobCode") String jobCode,
            @Param("driverId") Long driverId,
            @Param("cabId") Long cabId,
            @Param("tripDate") LocalDate tripDate
    );

    /**
     * Find trips by driver within a date range
     */
    @EntityGraph(attributePaths = {"driver", "cab"})
    @Query("SELECT dt FROM DriverTrip dt " +
           "WHERE dt.driver.id = :driverId " +
           "AND dt.tripDate >= :startDate " +
           "AND dt.tripDate <= :endDate " +
           "ORDER BY dt.tripDate DESC, dt.startTime DESC")
    List<DriverTrip> findByDriverIdAndDateRange(
            @Param("driverId") Long driverId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Find trips by driver username within a date range
     */
    @EntityGraph(attributePaths = {"driver", "cab"})
    @Query("SELECT dt FROM DriverTrip dt " +
           "WHERE dt.driverUsername = :driverUsername " +
           "AND dt.tripDate >= :startDate " +
           "AND dt.tripDate <= :endDate " +
           "ORDER BY dt.tripDate DESC, dt.startTime DESC")
    List<DriverTrip> findByDriverUsernameAndDateRange(
            @Param("driverUsername") String driverUsername,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Find trips by cab within a date range
     */
    @EntityGraph(attributePaths = {"driver", "cab"})
    List<DriverTrip> findByCabIdAndTripDateBetween(Long cabId, LocalDate startDate, LocalDate endDate);

    /**
     * Find trips by date range
     */
    @EntityGraph(attributePaths = {"driver", "cab"})
    List<DriverTrip> findByTripDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Find by job code
     */
    @EntityGraph(attributePaths = {"driver", "cab"})
    Optional<DriverTrip> findByJobCode(String jobCode);

    /**
     * Search trips with filters and pagination
     */
    @EntityGraph(attributePaths = {"driver", "cab"})
    @Query(
            value = "SELECT dt FROM DriverTrip dt " +
                    "WHERE (:driverUsername IS NULL OR dt.driverUsername = :driverUsername) " +
                    "AND (:cabId IS NULL OR dt.cab.id = :cabId) " +
                    "AND (:startDate IS NULL OR dt.tripDate >= :startDate) " +
                    "AND (:endDate IS NULL OR dt.tripDate <= :endDate) " +
                    "AND (:driverName IS NULL OR LOWER(dt.driverName) LIKE LOWER(CONCAT('%', :driverName, '%')))",
            countQuery = "SELECT COUNT(dt) FROM DriverTrip dt " +
                    "WHERE (:driverUsername IS NULL OR dt.driverUsername = :driverUsername) " +
                    "AND (:cabId IS NULL OR dt.cab.id = :cabId) " +
                    "AND (:startDate IS NULL OR dt.tripDate >= :startDate) " +
                    "AND (:endDate IS NULL OR dt.tripDate <= :endDate) " +
                    "AND (:driverName IS NULL OR LOWER(dt.driverName) LIKE LOWER(CONCAT('%', :driverName, '%')))"
    )
    Page<DriverTrip> searchTrips(
            @Param("driverUsername") String driverUsername,
            @Param("cabId") Long cabId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("driverName") String driverName,
            Pageable pageable);

    /**
     * Get top pickup addresses for heatmap analytics
     */
    @Query(value = """
            SELECT pickup_address, COUNT(*) AS cnt
            FROM driver_trips
            WHERE (:startDate IS NULL OR trip_date >= :startDate)
              AND (:endDate IS NULL OR trip_date <= :endDate)
              AND (:accountNumber IS NULL OR account_number = :accountNumber)
              AND (:driverId IS NULL OR driver_id = :driverId)
              AND (:startHour IS NULL OR HOUR(start_time) >= :startHour)
              AND (:endHour IS NULL OR HOUR(start_time) <= :endHour)
              AND pickup_address IS NOT NULL AND pickup_address != ''
            GROUP BY pickup_address
            ORDER BY cnt DESC
            LIMIT 60
            """, nativeQuery = true)
    List<Object[]> findTopPickupAddresses(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("accountNumber") String accountNumber,
            @Param("driverId") Long driverId,
            @Param("startHour") Integer startHour,
            @Param("endHour") Integer endHour);

    /**
     * Get trips aggregated by hour of day
     */
    @Query(value = """
            SELECT HOUR(start_time) AS hr, COUNT(*) AS cnt
            FROM driver_trips
            WHERE (:startDate IS NULL OR trip_date >= :startDate)
              AND (:endDate IS NULL OR trip_date <= :endDate)
              AND (:accountNumber IS NULL OR account_number = :accountNumber)
              AND (:driverId IS NULL OR driver_id = :driverId)
              AND start_time IS NOT NULL
            GROUP BY HOUR(start_time)
            ORDER BY hr
            """, nativeQuery = true)
    List<Object[]> findTripsByHour(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("accountNumber") String accountNumber,
            @Param("driverId") Long driverId);

    /**
     * Get trips aggregated by day of week (MySQL DAYOFWEEK: 1=Sun ... 7=Sat)
     */
    @Query(value = """
            SELECT DAYOFWEEK(trip_date) AS dow, COUNT(*) AS cnt
            FROM driver_trips
            WHERE (:startDate IS NULL OR trip_date >= :startDate)
              AND (:endDate IS NULL OR trip_date <= :endDate)
              AND (:accountNumber IS NULL OR account_number = :accountNumber)
              AND (:driverId IS NULL OR driver_id = :driverId)
            GROUP BY DAYOFWEEK(trip_date)
            ORDER BY dow
            """, nativeQuery = true)
    List<Object[]> findTripsByDayOfWeek(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("accountNumber") String accountNumber,
            @Param("driverId") Long driverId);

    /**
     * Get distinct account numbers in driver_trips (for filter dropdown)
     */
    @Query(value = """
            SELECT DISTINCT account_number FROM driver_trips
            WHERE account_number IS NOT NULL AND account_number != ''
            ORDER BY account_number
            """, nativeQuery = true)
    List<String> findDistinctAccountNumbers();
}
