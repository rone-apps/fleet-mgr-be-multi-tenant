package com.taxi.domain.tax.repository;

import com.taxi.domain.tax.model.DriverTaxEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DriverTaxEntryRepository extends JpaRepository<DriverTaxEntry, Long> {

    List<DriverTaxEntry> findByDriverIdAndTaxYearOrderByCreatedAtDesc(Long driverId, Integer taxYear);

    List<DriverTaxEntry> findByDriverIdAndTaxYearAndEntryTypeOrderByCreatedAtDesc(
        Long driverId, Integer taxYear, String entryType);

    List<DriverTaxEntry> findByDriverIdAndTaxYear(Long driverId, Integer taxYear);
}
