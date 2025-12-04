package org.example.audio_ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "return_shipping_fees")
public class ReturnShippingFee extends BaseEntity {

    @Column(name = "return_request_id", nullable = false, columnDefinition = "CHAR(36)")
    private UUID returnRequestId;

    @Column(name = "ghn_order_code", length = 64)
    private String ghnOrderCode;

    @Column(name = "shipping_fee", nullable = false, precision = 18, scale = 2)
    private BigDecimal shippingFee;

    // "CUSTOMER" / "SHOP"
    @Column(name = "payer", nullable = false, length = 20)
    private String payer;

    // Số tiền thực tế sẽ trừ vào shop khi đối soát
    @Column(name = "charged_to_shop", precision = 18, scale = 2)
    private BigDecimal chargedToShop;

    // flag cho case shop fault
    @Column(name = "shop_fault")
    private Boolean shopFault;

    @Builder.Default
    @Column(name = "picked", nullable = false)
    private boolean picked = false;
}
