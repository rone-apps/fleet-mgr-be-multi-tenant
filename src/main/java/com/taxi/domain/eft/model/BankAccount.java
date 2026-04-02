package com.taxi.domain.eft.model;

import com.taxi.domain.driver.model.Driver;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "bank_account")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @Column(name = "account_holder_name", nullable = false, length = 30)
    private String accountHolderName;

    @Column(name = "institution_number", nullable = false, length = 4)
    private String institutionNumber;

    @Column(name = "transit_number", nullable = false, length = 5)
    private String transitNumber;

    @Column(name = "account_number", nullable = false, length = 12)
    private String accountNumber;

    @Column(name = "account_type", nullable = false, length = 10)
    @Builder.Default
    private String accountType = "CHEQUING";

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private Boolean isVerified = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * CPA routing number format: 0 + institution(3) + transit(5) = 9 digits
     */
    public String getRoutingNumber() {
        return "0" + padLeft(institutionNumber, 3, '0') + padLeft(transitNumber, 5, '0');
    }

    /**
     * Masked account number for display (e.g., ****1234)
     */
    public String getMaskedAccountNumber() {
        if (accountNumber == null || accountNumber.length() <= 4) return "****";
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }

    private static String padLeft(String str, int len, char pad) {
        if (str == null) str = "";
        while (str.length() < len) str = pad + str;
        return str;
    }
}
