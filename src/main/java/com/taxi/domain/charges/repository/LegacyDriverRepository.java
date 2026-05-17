package com.taxi.domain.charges.repository;

import com.taxi.domain.charges.model.LegacyDriver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LegacyDriverRepository extends JpaRepository<LegacyDriver, Long> {

    /**
     * Find legacy driver by driver number (stable business key)
     */
    Optional<LegacyDriver> findByDriverNumber(String driverNumber);
}
