package com.taxi.domain.shift.repository;

import com.taxi.domain.shift.model.CabShift;
import com.taxi.domain.shift.model.ShiftType;
import com.taxi.domain.cab.model.Cab;
import com.taxi.domain.driver.model.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for CabShift entity
 */
@Repository
public interface CabShiftRepository extends JpaRepository<CabShift, Long> {

    /**
     * Find all shifts for a specific cab
     */
    List<CabShift> findByCab(Cab cab);

    /**
     * Find all shifts for a cab by cab ID
     */
    @Query("SELECT cs FROM CabShift cs WHERE cs.cab.id = :cabId")
    List<CabShift> findByCabId(@Param("cabId") Long cabId);

    /**
     * Find a specific shift for a cab
     */
    Optional<CabShift> findByCabAndShiftType(Cab cab, ShiftType shiftType);

    /**
     * Find all shifts owned by a driver
     */
    List<CabShift> findByCurrentOwner(Driver owner);

    /**
     * Find all shifts owned by a driver ID
     */
    @Query("SELECT cs FROM CabShift cs WHERE cs.currentOwner.id = :ownerId")
    List<CabShift> findByCurrentOwnerId(@Param("ownerId") Long ownerId);

    /**
     * Find all active shifts for a cab
     */
    @Query("SELECT cs FROM CabShift cs WHERE cs.cab.id = :cabId AND cs.status = 'ACTIVE'")
    List<CabShift> findActiveByCabId(@Param("cabId") Long cabId);

    /**
     * Find all active shifts owned by a driver
     */
    @Query("SELECT cs FROM CabShift cs WHERE cs.currentOwner.id = :ownerId AND cs.status = 'ACTIVE'")
    List<CabShift> findActiveByOwnerId(@Param("ownerId") Long ownerId);

    /**
     * Count shifts by owner
     */
    long countByCurrentOwner(Driver owner);

    /**
     * Check if cab has a specific shift type
     */
    boolean existsByCabAndShiftType(Cab cab, ShiftType shiftType);

    /**
     * Find shift by cab number and shift type
     */
    @Query("SELECT cs FROM CabShift cs WHERE cs.cab.cabNumber = :cabNumber AND cs.shiftType = :shiftType")
    Optional<CabShift> findByCabNumberAndShiftType(@Param("cabNumber") String cabNumber, @Param("shiftType") ShiftType shiftType);

    /**
     * Find shift by ID with cab eagerly loaded (to avoid LazyInitializationException)
     */
    @Query("SELECT cs FROM CabShift cs LEFT JOIN FETCH cs.cab WHERE cs.id = :id")
    Optional<CabShift> findByIdWithCab(@Param("id") Long id);

    /**
     * Find all shifts with a specific current profile
     */
    List<CabShift> findByCurrentProfileId(Long profileId);

    /**
     * Find all active shifts with a specific current profile
     */
    @Query("SELECT cs FROM CabShift cs WHERE cs.currentProfile.id = :profileId AND cs.status = 'ACTIVE'")
    List<CabShift> findActiveByCurrentProfileId(@Param("profileId") Long profileId);

    /**
     * Find all currently active shifts (for ALL_OWNERS application type)
     */
    @Query("SELECT cs FROM CabShift cs WHERE cs.status = 'ACTIVE'")
    List<CabShift> findAllActiveShifts();

    /**
     * Find shifts by owner ID and status
     */
    @Query("SELECT cs FROM CabShift cs WHERE cs.currentOwner.id = :ownerId AND cs.status = :status")
    List<CabShift> findByCurrentOwnerIdAndShiftActive(@Param("ownerId") Long ownerId, @Param("status") boolean active);
}
