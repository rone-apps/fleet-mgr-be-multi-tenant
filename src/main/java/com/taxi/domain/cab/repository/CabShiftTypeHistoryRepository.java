package com.taxi.domain.cab.repository;

import com.taxi.domain.cab.model.CabShiftTypeHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CabShiftTypeHistoryRepository extends JpaRepository<CabShiftTypeHistory, Long> {

    List<CabShiftTypeHistory> findByCabIdOrderByChangedAtDesc(Long cabId);
}
