package com.taxi.domain.tax.repository;

import com.taxi.domain.tax.model.IncomeTaxBracket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncomeTaxBracketRepository extends JpaRepository<IncomeTaxBracket, Long> {

    List<IncomeTaxBracket> findByTaxYearAndJurisdictionOrderByBracketOrderAsc(Integer taxYear, String jurisdiction);

    List<IncomeTaxBracket> findByTaxYearOrderByJurisdictionAscBracketOrderAsc(Integer taxYear);
}
