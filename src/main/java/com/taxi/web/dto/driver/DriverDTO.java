package com.taxi.web.dto.driver;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.taxi.domain.driver.model.Driver;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for Driver responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverDTO {

    private Long id;
    private String driverNumber;
    private String firstName;
    private String lastName;
    private String fullName;
    private String licenseNumber;
    private LocalDate licenseExpiry;
    
    @JsonProperty("licenseExpired")
    private boolean licenseExpired;
    
    private String phone;
    private String email;
    private String address;
    private String status;
    
    @JsonProperty("isOwner")
    private boolean isOwner;
    
    private LocalDate joinedDate;
    private String notes;

    // Tax & Financial Information
    private String sin;
    private String gstNumber;
    private Double depositAmount;

    // Emergency Contact Information
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelationship;

    // Document & Record Dates
    private LocalDate securityDepositDate;
    private LocalDate refundDate;
    private LocalDate picDate;
    private LocalDate ibcRecordsDate;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // User relationship
    private Long userId;
    private String username;

    /**
     * Convert Driver entity to DTO
     */
    public static DriverDTO fromEntity(Driver driver) {
        if (driver == null) {
            return null;
        }

        return DriverDTO.builder()
                .id(driver.getId())
                .driverNumber(driver.getDriverNumber())
                .firstName(driver.getFirstName())
                .lastName(driver.getLastName())
                .fullName(driver.getFullName())
                .licenseNumber(driver.getLicenseNumber())
                .licenseExpiry(driver.getLicenseExpiry())
                .licenseExpired(driver.isLicenseExpired())
                .phone(driver.getPhone())
                .email(driver.getEmail())
                .address(driver.getAddress())
                .status(driver.getStatus().name())
                .isOwner(Boolean.TRUE.equals(driver.getIsOwner()))
                .joinedDate(driver.getJoinedDate())
                .notes(driver.getNotes())
                .sin(driver.getSin())
                .gstNumber(driver.getGstNumber())
                .depositAmount(driver.getDepositAmount())
                .emergencyContactName(driver.getEmergencyContactName())
                .emergencyContactPhone(driver.getEmergencyContactPhone())
                .emergencyContactRelationship(driver.getEmergencyContactRelationship())
                .securityDepositDate(driver.getSecurityDepositDate())
                .refundDate(driver.getRefundDate())
                .picDate(driver.getPicDate())
                .ibcRecordsDate(driver.getIbcRecordsDate())
                .createdAt(driver.getCreatedAt())
                .updatedAt(driver.getUpdatedAt())
                .build();
    }

    /**
     * Convert Driver entity to DTO with User info
     */
    public static DriverDTO fromEntityWithUser(Driver driver, Long userId, String username) {
        DriverDTO dto = fromEntity(driver);
        if (dto != null) {
            dto.setUserId(userId);
            dto.setUsername(username);
        }
        return dto;
    }
}
