package com.taxi.web.dto.driver;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

/**
 * DTO for updating an existing driver
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateDriverRequest {

    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @Size(max = 50, message = "License number must not exceed 50 characters")
    private String licenseNumber;

    private LocalDate licenseExpiry;

    @Size(max = 20, message = "Phone must not exceed 20 characters")
    private String phone;

    @Email(message = "Email must be valid")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;

    private LocalDate joinedDate;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;

    private Boolean isOwner;

    // Tax & Financial Information
    @Size(max = 50, message = "SIN must not exceed 50 characters")
    private String sin;

    @Size(max = 50, message = "GST Number must not exceed 50 characters")
    private String gstNumber;

    private Double depositAmount;

    // Emergency Contact Information
    @Size(max = 100, message = "Emergency contact name must not exceed 100 characters")
    private String emergencyContactName;

    @Size(max = 20, message = "Emergency contact phone must not exceed 20 characters")
    private String emergencyContactPhone;

    @Size(max = 50, message = "Relationship must not exceed 50 characters")
    private String emergencyContactRelationship;

    // Document & Record Dates
    private LocalDate securityDepositDate;
    private LocalDate refundDate;
    private LocalDate picDate;
    private LocalDate ibcRecordsDate;

    private String status;  // ACTIVE, INACTIVE, SUSPENDED, TERMINATED
}
