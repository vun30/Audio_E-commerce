package org.example.audio_ecommerce.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Table(name = "payout_return_shipping_fee")
public class PayoutReturnShippingFee {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id", nullable = false)
    @JsonIgnore
    private PayoutBill bill;

    @Column(name = "return_request_id", nullable = false, columnDefinition = "CHAR(36)")
    private UUID returnRequestId;

    @Column(name = "ghn_order_code", length = 64)
    private String ghnOrderCode;

    @Column(name = "shipping_fee", precision = 18, scale = 2)
    private BigDecimal shippingFee;

    @Column(name = "charged_to_shop", precision = 18, scale = 2)
    private BigDecimal chargedToShop;

    @Column(name = "shipping_type", length = 20)
    private String shippingType; // RETURN
}
