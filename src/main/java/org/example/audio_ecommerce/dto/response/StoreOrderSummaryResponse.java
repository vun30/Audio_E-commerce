package org.example.audio_ecommerce.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.audio_ecommerce.entity.Enum.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreOrderSummaryResponse {

    private UUID id;                     // store_order_id
    private String orderCode;

    private UUID storeId;
    private String storeName;

    private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime deliveredAt;

    private BigDecimal totalAmount;
    private BigDecimal discountTotal;
    private BigDecimal storeVoucherDiscount;
    private BigDecimal platformVoucherDiscount;

    private BigDecimal shippingFee;
    private BigDecimal shippingFeeReal;
    private BigDecimal shippingFeeForStore;
    private BigDecimal grandTotal;

    // ⭐ snapshot voucher áp cho store này
    private String storeVoucherDetailJson;     // {"CODE1":10000,"CODE2":15000}
    private String platformVoucherDetailJson;  // {"PLAT_CODE_1":20000}

    // (Nếu muốn detail product theo store luôn, có thể thêm List<StoreOrderItemResponse> items ở đây)
}
