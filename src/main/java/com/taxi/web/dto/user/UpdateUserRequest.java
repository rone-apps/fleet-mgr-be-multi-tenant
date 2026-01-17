package com.taxi.web.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * DTO for updating an existing user
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserRequest {

    @Email(message = "Email must be valid")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @Size(max = 50, message = "First name must not exceed 50 characters")
    private String firstName;

    @Size(max = 50, message = "Last name must not exceed 50 characters")
    private String lastName;

    @Size(max = 20, message = "Phone must not exceed 20 characters")
    private String phone;

    private String role;  // ADMIN, DRIVER, ACCOUNTANT, DISPATCHER, MANAGER, VIEWER

    // Password is optional - only update if provided
    @Size(min = 6, max = 255, message = "Password must be at least 6 characters")
    private String password;
}
