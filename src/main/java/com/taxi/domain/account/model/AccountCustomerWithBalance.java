package com.taxi.domain.account.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO to return Account Customer with their outstanding balance amount
 */
@Data
@NoArgsConstructor
public class AccountCustomerWithBalance {
    private Long id;
    private String accountId;
    private String companyName;
    private String contactPerson;
    private String streetAddress;
    private String city;
    private String province;
    private String postalCode;
    private String country;
    private String phoneNumber;
    private String email;
    private String billingPeriod;
    private BigDecimal creditLimit;
    private String notes;
    private String accountType;
    private boolean active;
    private BigDecimal outstandingBalance;

    /**
     * Constructor with all fields from query result
     */
    public AccountCustomerWithBalance(
            Long id, String accountId, String companyName, String contactPerson,
            String streetAddress, String city, String province, String postalCode,
            String country, String phoneNumber, String email, String billingPeriod,
            BigDecimal creditLimit, String notes, String accountType, Boolean active,
            BigDecimal outstandingBalance) {
        this.id = id;
        this.accountId = accountId;
        this.companyName = companyName;
        this.contactPerson = contactPerson;
        this.streetAddress = streetAddress;
        this.city = city;
        this.province = province;
        this.postalCode = postalCode;
        this.country = country;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.billingPeriod = billingPeriod;
        this.creditLimit = creditLimit;
        this.notes = notes;
        this.accountType = accountType;
        this.active = active != null ? active : false;
        this.outstandingBalance = outstandingBalance != null ? outstandingBalance : BigDecimal.ZERO;
    }
}
