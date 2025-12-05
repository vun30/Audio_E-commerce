package org.example.audio_ecommerce.dto.response;

import lombok.*;
import org.example.audio_ecommerce.entity.PayoutShippingOrderFee;

import java.math.BigDecimal;
import java.util.UUID;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class PayoutShippingOrderFeeResponse {

    private UUID storeOrderId;
    private String ghnOrderCode;
    private BigDecimal shippingFee;
    private String shippingType; // SHIPPING

    public static PayoutShippingOrderFeeResponse fromEntity(PayoutShippingOrderFee e) {
        return PayoutShippingOrderFeeResponse.builder()
                .storeOrderId(e.getStoreOrderId())
                .ghnOrderCode(e.getGhnOrderCode())
                .shippingFee(e.getShippingFee())
                .shippingType(e.getShippingType())
                .build();
    }
}
