package com.taxi.domain.drivertrip.dto;

import com.taxi.domain.drivertrip.model.DriverTrip;
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
public class DriverTripDTO {
    private Long id;
    private String jobCode;
    private String driverUsername;
    private String driverName;
    private LocalDate tripDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String pickupAddress;
    private String dropoffAddress;
    private String passengerName;
    private String accountNumber;
    private String companyId;
    private BigDecimal fareAmount;
    private BigDecimal tipAmount;
    private CabDTO cab;
    private DriverDTO driver;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CabDTO {
        private Long id;
        private String cabNumber;
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

    public static DriverTripDTO fromEntity(DriverTrip trip) {
        return DriverTripDTO.builder()
                .id(trip.getId())
                .jobCode(trip.getJobCode())
                .driverUsername(trip.getDriverUsername())
                .driverName(trip.getDriverName())
                .tripDate(trip.getTripDate())
                .startTime(trip.getStartTime())
                .endTime(trip.getEndTime())
                .pickupAddress(trip.getPickupAddress())
                .dropoffAddress(trip.getDropoffAddress())
                .passengerName(trip.getPassengerName())
                .accountNumber(trip.getAccountNumber())
                .companyId(trip.getCompanyId())
                .fareAmount(trip.getFareAmount())
                .tipAmount(trip.getTipAmount())
                .cab(trip.getCab() != null ? CabDTO.builder()
                        .id(trip.getCab().getId())
                        .cabNumber(trip.getCab().getCabNumber())
                        .build() : null)
                .driver(trip.getDriver() != null ? DriverDTO.builder()
                        .id(trip.getDriver().getId())
                        .firstName(trip.getDriver().getFirstName())
                        .lastName(trip.getDriver().getLastName())
                        .build() : null)
                .build();
    }

    public BigDecimal getTotalAmount() {
        BigDecimal fare = fareAmount != null ? fareAmount : BigDecimal.ZERO;
        BigDecimal tip = tipAmount != null ? tipAmount : BigDecimal.ZERO;
        return fare.add(tip);
    }
}
