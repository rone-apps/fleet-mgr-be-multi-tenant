package com.taxi.domain.cab.repository;

import com.taxi.domain.cab.model.Cab;
import com.taxi.domain.cab.model.CabOwnerHistory;
import com.taxi.domain.driver.model.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for CabOwnerHistory entity
 */
@Repository
public interface CabOwnerHistoryRepository extends JpaRepository<CabOwnerHistory, Long> {

    /**
     * Get all ownership history for a cab
     */
    List<CabOwnerHistory> findByCabOrderByStartDateDesc(Cab cab);

    /**
     * Get current owner record for a cab (where endDate is null)
     */
    @Query("SELECT h FROM CabOwnerHistory h WHERE h.cab = :cab AND h.endDate IS NULL")
    Optional<CabOwnerHistory> findCurrentOwner(@Param("cab") Cab cab);

    /**
     * Get all cabs currently owned by a driver
     */
    @Query("SELECT h FROM CabOwnerHistory h WHERE h.ownerDriver = :driver AND h.endDate IS NULL")
    List<CabOwnerHistory> findCurrentCabsByOwner(@Param("driver") Driver driver);

    /**
     * Get all ownership history for a driver
     */
    List<CabOwnerHistory> findByOwnerDriverOrderByStartDateDesc(Driver driver);
}
