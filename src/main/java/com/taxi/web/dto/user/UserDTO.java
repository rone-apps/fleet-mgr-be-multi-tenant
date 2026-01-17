package com.taxi.web.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.taxi.domain.user.model.User;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO for User responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {

    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String role;
    
    @JsonProperty("isActive")
    private boolean isActive;
    
    // Driver relationship
    private Long driverId;
    private String driverNumber;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Convert User entity to DTO (without password)
     */
    public static UserDTO fromEntity(User user) {
        if (user == null) {
            return null;
        }

        UserDTOBuilder builder = UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .isActive(user.isActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt());

        // Add driver info if user is linked to a driver
        if (user.getDriver() != null) {
            builder.driverId(user.getDriver().getId())
                   .driverNumber(user.getDriver().getDriverNumber());
        }

        return builder.build();
    }
}
