package org.example.audio_ecommerce.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.example.audio_ecommerce.entity.Enum.OrderStatus;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
public class StoreOrderDebtItemResponse {
    private UUID orderId;
    private String orderCode;
    private OrderStatus status;

    private BigDecimal debtNeedToPay;           // totalDebtOrder
    private BigDecimal shippingFeeCustomerPaid; // shippingFee (estimate)
    private BigDecimal shippingFeeReal;         // shippingFeeReal (actual)
    private BigDecimal afterSubtract;           // max(real - estimate, 0) (success only)
    private BigDecimal boomFee;                 // real * 1.5 (boom/return)
    private Boolean returnChargeApplied;        // flag
}
