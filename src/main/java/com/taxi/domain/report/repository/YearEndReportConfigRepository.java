package com.taxi.domain.report.repository;

import com.taxi.domain.report.model.YearEndReportConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface YearEndReportConfigRepository extends JpaRepository<YearEndReportConfig, Long> {

    List<YearEndReportConfig> findAllByOrderBySectionAscDisplayOrderAsc();

    List<YearEndReportConfig> findByIsVisibleTrueOrderBySectionAscDisplayOrderAsc();

    List<YearEndReportConfig> findBySectionOrderByDisplayOrderAsc(String section);

    Optional<YearEndReportConfig> findBySectionAndItemKey(String section, String itemKey);

    boolean existsBySectionAndItemKey(String section, String itemKey);
}
