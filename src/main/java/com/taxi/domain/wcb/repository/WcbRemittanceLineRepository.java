package com.taxi.domain.wcb.repository;

import com.taxi.domain.wcb.model.WcbRemittanceLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WcbRemittanceLineRepository extends JpaRepository<WcbRemittanceLine, Long> {
    List<WcbRemittanceLine> findByRemittanceId(Long remittanceId);
    List<WcbRemittanceLine> findByClaimNumber(String claimNumber);
}
