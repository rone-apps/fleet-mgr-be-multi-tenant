package com.taxi.domain.tax.service;

import com.taxi.domain.tax.model.*;
import com.taxi.domain.tax.repository.*;
import com.taxi.web.dto.tax.*;
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
public class TaxRatesConfigService {

    private final DriverTaxProfileRepository driverTaxProfileRepository;
    private final IncomeTaxBracketRepository incomeTaxBracketRepository;
    private final TaxCreditRepository taxCreditRepository;
    private final CppEiRateRepository cppEiRateRepository;

    // ===== Driver Tax Profile =====
    public DriverTaxProfileDTO getOrCreateProfile(Long driverId, Integer taxYear) {
        return driverTaxProfileRepository.findByDriverIdAndTaxYear(driverId, taxYear)
            .map(this::toProfileDTO)
            .orElse(DriverTaxProfileDTO.builder()
                .driverId(driverId)
                .taxYear(taxYear)
                .language("EN")
                .maritalStatus("SINGLE")
                .numDependents(0)
                .build());
    }

    public DriverTaxProfileDTO saveProfile(DriverTaxProfileDTO dto) {
        DriverTaxProfile profile = driverTaxProfileRepository.findByDriverIdAndTaxYear(dto.getDriverId(), dto.getTaxYear())
            .orElse(new DriverTaxProfile());

        profile.setDriverId(dto.getDriverId());
        profile.setTaxYear(dto.getTaxYear());
        profile.setProvince(dto.getProvince());
        profile.setLanguage(dto.getLanguage() != null ? dto.getLanguage() : "EN");
        profile.setMaritalStatus(dto.getMaritalStatus() != null ? dto.getMaritalStatus() : "SINGLE");
        profile.setNumDependents(dto.getNumDependents() != null ? dto.getNumDependents() : 0);
        profile.setBirthYear(dto.getBirthYear());
        profile.setHasDisability(dto.getHasDisability() != null ? dto.getHasDisability() : false);
        profile.setSpouseDisability(dto.getSpouseDisability() != null ? dto.getSpouseDisability() : false);

        profile = driverTaxProfileRepository.save(profile);
        return toProfileDTO(profile);
    }

    // ===== Tax Brackets =====
    public List<IncomeTaxBracketDTO> getBrackets(Integer taxYear, String jurisdiction) {
        return incomeTaxBracketRepository.findByTaxYearAndJurisdictionOrderByBracketOrderAsc(taxYear, jurisdiction)
            .stream().map(this::toBracketDTO).collect(Collectors.toList());
    }

    public IncomeTaxBracketDTO saveBracket(IncomeTaxBracketDTO dto) {
        IncomeTaxBracket bracket = new IncomeTaxBracket();
        if (dto.getId() != null) {
            bracket = incomeTaxBracketRepository.findById(dto.getId()).orElse(new IncomeTaxBracket());
        }
        bracket.setTaxYear(dto.getTaxYear());
        bracket.setJurisdiction(dto.getJurisdiction());
        bracket.setBracketOrder(dto.getBracketOrder());
        bracket.setMinIncome(dto.getMinIncome());
        bracket.setMaxIncome(dto.getMaxIncome());
        bracket.setRate(dto.getRate());
        bracket = incomeTaxBracketRepository.save(bracket);
        return toBracketDTO(bracket);
    }

    public void deleteBracket(Long id) {
        incomeTaxBracketRepository.deleteById(id);
    }

    // ===== Tax Credits =====
    public List<TaxCreditDTO> getCredits(Integer taxYear, String jurisdiction) {
        return taxCreditRepository.findByTaxYearAndJurisdictionOrderByIdAsc(taxYear, jurisdiction)
            .stream().map(this::toCreditDTO).collect(Collectors.toList());
    }

    public TaxCreditDTO saveCredit(TaxCreditDTO dto) {
        TaxCredit credit = new TaxCredit();
        if (dto.getId() != null) {
            credit = taxCreditRepository.findById(dto.getId()).orElse(new TaxCredit());
        }
        credit.setTaxYear(dto.getTaxYear());
        credit.setJurisdiction(dto.getJurisdiction());
        credit.setCreditCode(dto.getCreditCode());
        credit.setCreditName(dto.getCreditName());
        credit.setAmount(dto.getAmount());
        credit.setRate(dto.getRate());
        credit.setDescription(dto.getDescription());
        credit.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        credit = taxCreditRepository.save(credit);
        return toCreditDTO(credit);
    }

    public void deleteCredit(Long id) {
        taxCreditRepository.deleteById(id);
    }

    // ===== CPP/EI Rates =====
    public CppEiRateDTO getRates(Integer taxYear) {
        return cppEiRateRepository.findByTaxYear(taxYear)
            .map(this::toRateDTO)
            .orElse(null);
    }

    public CppEiRateDTO saveRates(CppEiRateDTO dto) {
        CppEiRate rates = new CppEiRate();
        if (dto.getId() != null) {
            rates = cppEiRateRepository.findById(dto.getId()).orElse(new CppEiRate());
        }
        rates.setTaxYear(dto.getTaxYear());
        rates.setCppEmployeeRate(dto.getCppEmployeeRate());
        rates.setCppMaxPensionable(dto.getCppMaxPensionable());
        rates.setCppBasicExemption(dto.getCppBasicExemption());
        rates.setEiEmployeeRate(dto.getEiEmployeeRate());
        rates.setEiMaxInsurable(dto.getEiMaxInsurable());
        rates = cppEiRateRepository.save(rates);
        return toRateDTO(rates);
    }

    // ===== Mappers =====
    private DriverTaxProfileDTO toProfileDTO(DriverTaxProfile profile) {
        return DriverTaxProfileDTO.builder()
            .id(profile.getId())
            .driverId(profile.getDriverId())
            .taxYear(profile.getTaxYear())
            .province(profile.getProvince())
            .language(profile.getLanguage())
            .maritalStatus(profile.getMaritalStatus())
            .numDependents(profile.getNumDependents())
            .birthYear(profile.getBirthYear())
            .hasDisability(profile.getHasDisability())
            .spouseDisability(profile.getSpouseDisability())
            .createdAt(profile.getCreatedAt())
            .updatedAt(profile.getUpdatedAt())
            .build();
    }

    private IncomeTaxBracketDTO toBracketDTO(IncomeTaxBracket bracket) {
        return IncomeTaxBracketDTO.builder()
            .id(bracket.getId())
            .taxYear(bracket.getTaxYear())
            .jurisdiction(bracket.getJurisdiction())
            .bracketOrder(bracket.getBracketOrder())
            .minIncome(bracket.getMinIncome())
            .maxIncome(bracket.getMaxIncome())
            .rate(bracket.getRate())
            .createdAt(bracket.getCreatedAt())
            .build();
    }

    private TaxCreditDTO toCreditDTO(TaxCredit credit) {
        return TaxCreditDTO.builder()
            .id(credit.getId())
            .taxYear(credit.getTaxYear())
            .jurisdiction(credit.getJurisdiction())
            .creditCode(credit.getCreditCode())
            .creditName(credit.getCreditName())
            .amount(credit.getAmount())
            .rate(credit.getRate())
            .description(credit.getDescription())
            .isActive(credit.getIsActive())
            .createdAt(credit.getCreatedAt())
            .build();
    }

    private CppEiRateDTO toRateDTO(CppEiRate rate) {
        return CppEiRateDTO.builder()
            .id(rate.getId())
            .taxYear(rate.getTaxYear())
            .cppEmployeeRate(rate.getCppEmployeeRate())
            .cppMaxPensionable(rate.getCppMaxPensionable())
            .cppBasicExemption(rate.getCppBasicExemption())
            .eiEmployeeRate(rate.getEiEmployeeRate())
            .eiMaxInsurable(rate.getEiMaxInsurable())
            .createdAt(rate.getCreatedAt())
            .build();
    }
}
