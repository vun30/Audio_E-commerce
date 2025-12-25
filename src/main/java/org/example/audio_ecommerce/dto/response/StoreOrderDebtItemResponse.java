package org.example.audio_ecommerce.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.example.audio_ecommerce.entity.Enum.OrderStatus;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
public class StoreOrderDebtItemResponse {

    // ====== ORDER ======
    private UUID orderId;
    private String orderCode;
    private OrderStatus status;

    // ====== RETURN FEE ======
    private UUID returnRequestId;
    private String ghnOrderCode;

    // ====== COMMON ======
    private BigDecimal debtNeedToPay;
    private BigDecimal shippingFeeCustomerPaid;
    private BigDecimal shippingFeeReal;
    private BigDecimal afterSubtract;
    private BigDecimal boomFee;
    private Boolean returnChargeApplied;

    private String debtType; // ORDER | RETURN_SHIP
}
