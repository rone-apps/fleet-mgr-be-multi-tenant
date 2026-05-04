package com.taxi.web.controller;

import com.taxi.domain.wcb.model.WcbRemittance;
import com.taxi.domain.wcb.model.WcbRemittanceLine;
import com.taxi.domain.wcb.repository.WcbRemittanceRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/wcb-remittances")
public class WcbRemittanceController {

    private final WcbRemittanceRepository remittanceRepository;

    public WcbRemittanceController(WcbRemittanceRepository remittanceRepository) {
        this.remittanceRepository = remittanceRepository;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllRemittances() {
        List<WcbRemittance> remittances = remittanceRepository.findAll();
        List<Map<String, Object>> response = new ArrayList<>();

        for (WcbRemittance rem : remittances) {
            Map<String, Object> remMap = new LinkedHashMap<>();
            remMap.put("id", rem.getId());
            remMap.put("payeeName", rem.getPayeeName());
            remMap.put("payeeNumber", rem.getPayeeNumber());
            remMap.put("chequeNumber", rem.getChequeNumber());
            remMap.put("chequeDate", rem.getChequeDate());
            remMap.put("totalAmount", rem.getTotalAmount());
            remMap.put("currency", rem.getCurrency());
            remMap.put("createdAt", rem.getCreatedAt());
            remMap.put("lineItemCount", rem.getLines().size());

            List<Map<String, Object>> linesMaps = new ArrayList<>();
            for (WcbRemittanceLine line : rem.getLines()) {
                Map<String, Object> lineMap = new LinkedHashMap<>();
                lineMap.put("id", line.getId());
                lineMap.put("claimNumber", line.getClaimNumber());
                lineMap.put("invoiceNo", line.getInvoiceNo());
                lineMap.put("customerName", line.getCustomerName());
                lineMap.put("serviceDate", line.getServiceDate());
                lineMap.put("serviceCode", line.getServiceCode());
                lineMap.put("invoiceAmount", line.getInvoiceAmount());
                lineMap.put("amount", line.getAmount());
                lineMap.put("explanation", line.getExplanation());
                linesMaps.add(lineMap);
            }
            remMap.put("lines", linesMaps);
            response.add(remMap);
        }

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRemittance(@PathVariable Long id) {
        if (remittanceRepository.existsById(id)) {
            remittanceRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
