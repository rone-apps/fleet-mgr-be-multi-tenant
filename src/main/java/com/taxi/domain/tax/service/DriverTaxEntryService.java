package com.taxi.domain.tax.service;

import com.taxi.domain.tax.model.DriverTaxEntry;
import com.taxi.domain.tax.repository.DriverTaxEntryRepository;
import com.taxi.web.dto.tax.DriverTaxEntryDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DriverTaxEntryService {

    private final DriverTaxEntryRepository driverTaxEntryRepository;

    public List<DriverTaxEntryDTO> getEntriesForDriver(Long driverId, Integer taxYear) {
        log.debug("Fetching tax entries for driver {} for tax year {}", driverId, taxYear);
        List<DriverTaxEntry> entries = driverTaxEntryRepository.findByDriverIdAndTaxYearOrderByCreatedAtDesc(driverId, taxYear);
        return entries.stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    public List<DriverTaxEntryDTO> getEntriesForDriverByType(Long driverId, Integer taxYear, String entryType) {
        log.debug("Fetching {} tax entries for driver {} for tax year {}", entryType, driverId, taxYear);
        List<DriverTaxEntry> entries = driverTaxEntryRepository.findByDriverIdAndTaxYearAndEntryTypeOrderByCreatedAtDesc(driverId, taxYear, entryType);
        return entries.stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    public DriverTaxEntryDTO createEntry(DriverTaxEntryDTO dto) {
        log.info("Creating tax entry for driver {} - type: {}, amount: {}", dto.getDriverId(), dto.getEntryType(), dto.getAmount());

        DriverTaxEntry entry = DriverTaxEntry.builder()
            .driverId(dto.getDriverId())
            .driverName(dto.getDriverName())
            .taxYear(dto.getTaxYear())
            .entryType(dto.getEntryType())
            .slipType(dto.getSlipType())
            .boxLabel(dto.getBoxLabel())
            .issuerName(dto.getIssuerName())
            .amount(dto.getAmount())
            .notes(dto.getNotes())
            .build();

        entry = driverTaxEntryRepository.save(entry);
        return toDTO(entry);
    }

    public DriverTaxEntryDTO updateEntry(Long id, DriverTaxEntryDTO dto) {
        log.info("Updating tax entry ID: {}", id);

        DriverTaxEntry entry = driverTaxEntryRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Tax entry not found: " + id));

        if (dto.getEntryType() != null) {
            entry.setEntryType(dto.getEntryType());
        }
        if (dto.getSlipType() != null) {
            entry.setSlipType(dto.getSlipType());
        }
        if (dto.getBoxLabel() != null) {
            entry.setBoxLabel(dto.getBoxLabel());
        }
        if (dto.getIssuerName() != null) {
            entry.setIssuerName(dto.getIssuerName());
        }
        if (dto.getAmount() != null) {
            entry.setAmount(dto.getAmount());
        }
        if (dto.getNotes() != null) {
            entry.setNotes(dto.getNotes());
        }

        entry = driverTaxEntryRepository.save(entry);
        return toDTO(entry);
    }

    public void deleteEntry(Long id) {
        log.info("Deleting tax entry ID: {}", id);
        DriverTaxEntry entry = driverTaxEntryRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Tax entry not found: " + id));
        driverTaxEntryRepository.deleteById(id);
    }

    private DriverTaxEntryDTO toDTO(DriverTaxEntry entry) {
        return DriverTaxEntryDTO.builder()
            .id(entry.getId())
            .driverId(entry.getDriverId())
            .driverName(entry.getDriverName())
            .taxYear(entry.getTaxYear())
            .entryType(entry.getEntryType())
            .slipType(entry.getSlipType())
            .boxLabel(entry.getBoxLabel())
            .issuerName(entry.getIssuerName())
            .amount(entry.getAmount())
            .notes(entry.getNotes())
            .createdAt(entry.getCreatedAt())
            .updatedAt(entry.getUpdatedAt())
            .build();
    }
}
