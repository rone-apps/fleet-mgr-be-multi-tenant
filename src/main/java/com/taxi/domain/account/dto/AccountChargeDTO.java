package com.taxi.domain.account.dto;

import com.taxi.domain.account.model.AccountCharge;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountChargeDTO {
    private Long id;
    private String accountId;
    private Long customerId;
    private String customerName;
    private String jobCode;
    private LocalDate tripDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String pickupAddress;
    private String dropoffAddress;
    private String passengerName;
    private CabDTO cab;
    private DriverDTO driver;
    private BigDecimal fareAmount;
    private BigDecimal tipAmount;
    private String notes;
    private boolean paid;
    private LocalDate paidDate;
    private String invoiceNumber;
    private String subAccount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CabDTO {
        private Long id;
        private String cabNumber;
        private String cabType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DriverDTO {
        private Long id;
        private String firstName;
        private String lastName;
    }

    public static AccountChargeDTO fromEntity(AccountCharge charge) {
        return AccountChargeDTO.builder()
                .id(charge.getId())
                .accountId(charge.getAccountId())
                .customerId(charge.getAccountCustomer() != null ? charge.getAccountCustomer().getId() : null)
                .customerName(charge.getAccountCustomer() != null ? charge.getAccountCustomer().getCompanyName() : null)
                .jobCode(charge.getJobCode())
                .tripDate(charge.getTripDate())
                .startTime(charge.getStartTime())
                .endTime(charge.getEndTime())
                .pickupAddress(charge.getPickupAddress())
                .dropoffAddress(charge.getDropoffAddress())
                .passengerName(charge.getPassengerName())
                .cab(charge.getCab() != null ? CabDTO.builder()
                        .id(charge.getCab().getId())
                        .cabNumber(charge.getCab().getCabNumber())
                        .cabType(null)  // Attributes moved to shift level
                        .build() : null)
                .driver(charge.getDriver() != null ? DriverDTO.builder()
                        .id(charge.getDriver().getId())
                        .firstName(charge.getDriver().getFirstName())
                        .lastName(charge.getDriver().getLastName())
                        .build() : null)
                .fareAmount(charge.getFareAmount())
                .tipAmount(charge.getTipAmount())
                .notes(charge.getNotes())
                .paid(charge.isPaid())
                .paidDate(charge.getPaidDate())
                .invoiceNumber(charge.getInvoiceNumber()).subAccount(charge.getSubAccount())
                .build();
    }

    public BigDecimal getTotalAmount() {
        return fareAmount.add(tipAmount != null ? tipAmount : BigDecimal.ZERO);
    }
}