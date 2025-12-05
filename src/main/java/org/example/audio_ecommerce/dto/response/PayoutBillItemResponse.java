package org.example.audio_ecommerce.dto.response;

import lombok.*;
import org.example.audio_ecommerce.entity.PayoutBillItem;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayoutBillItemResponse {

    private UUID orderItemId;
    private UUID storeOrderId;

    private String productName;
    private Integer quantity;

    private BigDecimal finalLineTotal;
    private BigDecimal platformFeePercentage;
    private BigDecimal platformFeeAmount;
    private BigDecimal netPayout;

    private Boolean isReturned;

    public static PayoutBillItemResponse fromEntity(PayoutBillItem e) {
        return PayoutBillItemResponse.builder()
                .orderItemId(e.getOrderItemId())
                .storeOrderId(e.getStoreOrderId())
                .productName(e.getProductName())
                .quantity(e.getQuantity())
                .finalLineTotal(e.getFinalLineTotal())
                .platformFeePercentage(e.getPlatformFeePercentage())
                .platformFeeAmount(e.getPlatformFeeAmount())
                .netPayout(e.getNetPayout())
                .isReturned(e.getIsReturned())
                .build();
    }
}
