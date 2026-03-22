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
}
