package com.taxi.domain.account.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "invoice_line_item")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class InvoiceLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    @JsonBackReference
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "charge_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private AccountCharge charge;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "trip_date")
    private LocalDate tripDate;

    @Column(name = "quantity")
    @Builder.Default
    private Integer quantity = 1;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    // Business methods
    public void calculateAmount() {
        if (quantity != null && unitPrice != null) {
            this.amount = unitPrice.multiply(new BigDecimal(quantity));
        }
    }

    public static InvoiceLineItem fromCharge(AccountCharge charge) {
        InvoiceLineItem lineItem = InvoiceLineItem.builder()
                .charge(charge)
                .tripDate(charge.getTripDate())
                .quantity(1)
                .unitPrice(charge.getTotalAmount())
                .amount(charge.getTotalAmount())
                .build();

        // Build description
        StringBuilder desc = new StringBuilder();
        desc.append("Trip on ").append(charge.getTripDate());
        
        if (charge.getJobCode() != null) {
            desc.append(" - Job #").append(charge.getJobCode());
        }
        
        if (charge.getPickupAddress() != null && charge.getDropoffAddress() != null) {
            desc.append(" (").append(charge.getPickupAddress())
                .append(" → ").append(charge.getDropoffAddress()).append(")");
        }
        
        if (charge.getPassengerName() != null) {
            desc.append(" - ").append(charge.getPassengerName());
        }

        lineItem.setDescription(desc.toString());
        
        return lineItem;
    }
}
