package com.taxi.domain.tax.repository;

import com.taxi.domain.tax.model.TaxCredit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaxCreditRepository extends JpaRepository<TaxCredit, Long> {

    List<TaxCredit> findByTaxYearAndJurisdictionAndIsActiveTrueOrderByIdAsc(Integer taxYear, String jurisdiction);

    List<TaxCredit> findByTaxYearAndJurisdictionOrderByIdAsc(Integer taxYear, String jurisdiction);

    List<TaxCredit> findByTaxYear(Integer taxYear);
}
