package com.taxi.domain.wcb.repository;

import com.taxi.domain.wcb.model.WcbRemittance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WcbRemittanceRepository extends JpaRepository<WcbRemittance, Long> {
    List<WcbRemittance> findByReceiptId(Long receiptId);
    List<WcbRemittance> findByPayeeNumber(String payeeNumber);
    List<WcbRemittance> findByPayeeNumberAndChequeNumber(String payeeNumber, String chequeNumber);
}
