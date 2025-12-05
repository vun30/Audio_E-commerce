package org.example.audio_ecommerce.dto.response;

import lombok.*;
import org.example.audio_ecommerce.entity.PayoutReturnShippingFee;

import java.math.BigDecimal;
import java.util.UUID;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class PayoutReturnShippingFeeResponse {

    private UUID returnRequestId;
    private String ghnOrderCode;
    private BigDecimal shippingFee;
    private BigDecimal chargedToShop;
    private String shippingType; // RETURN

    public static PayoutReturnShippingFeeResponse fromEntity(PayoutReturnShippingFee e) {
        return PayoutReturnShippingFeeResponse.builder()
                .returnRequestId(e.getReturnRequestId())
                .ghnOrderCode(e.getGhnOrderCode())
                .shippingFee(e.getShippingFee())
                .chargedToShop(e.getChargedToShop())
                .shippingType(e.getShippingType())
                .build();
    }
}
