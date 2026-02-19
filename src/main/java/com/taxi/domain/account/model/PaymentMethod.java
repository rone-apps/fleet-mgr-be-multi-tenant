package com.taxi.domain.account.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "payment_method")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "method_name", nullable = false, length = 100)
    private String methodName;

    @Column(name = "method_code", nullable = false, length = 20)
    private String methodCode;

    @Column(name = "requires_reference", nullable = false)
    @Builder.Default
    private Boolean requiresReference = true;

    @Column(name = "requires_bank_details", nullable = false)
    @Builder.Default
    private Boolean requiresBankDetails = false;

    @Column(name = "is_automatic", nullable = false)
    @Builder.Default
    private Boolean isAutomatic = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "display_order")
    private Integer displayOrder;
}
