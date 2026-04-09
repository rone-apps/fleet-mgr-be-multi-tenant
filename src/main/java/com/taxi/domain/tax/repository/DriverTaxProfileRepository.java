package com.taxi.domain.tax.repository;

import com.taxi.domain.tax.model.DriverTaxProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DriverTaxProfileRepository extends JpaRepository<DriverTaxProfile, Long> {

    Optional<DriverTaxProfile> findByDriverIdAndTaxYear(Long driverId, Integer taxYear);
}
