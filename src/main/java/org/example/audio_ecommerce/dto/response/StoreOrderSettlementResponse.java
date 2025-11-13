package org.example.audio_ecommerce.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreOrderSettlementResponse {

    private UUID storeOrderId;
    private UUID customerOrderId;

    private UUID storeId;
    private String storeName;

    private String status;
    private LocalDateTime createdAt;

    // Tổng tiền hàng (chưa trừ voucher, chưa cộng ship)
    private BigDecimal productsTotal;

    // Giảm giá
    private BigDecimal discountTotal;
    private BigDecimal storeVoucherDiscount;
    private BigDecimal platformVoucherDiscount;

    // Shipping
    private BigDecimal customerShippingFee;      // shippingFee mà khách đã trả (StoreOrder.shippingFee)
    private BigDecimal actualShippingFee;        // GHN báo thật (GhnOrder.totalFee)
    private BigDecimal shippingExtraForStore;    // phần chênh lệch shop chịu (max(actual - customer, 0))

    // Phí nền tảng
    private BigDecimal platformFeeRate;          // % dùng lúc tính (ví dụ 5.00)
    private BigDecimal platformFeeAmount;        // số tiền phí nền tảng

    // Tiền shop thực nhận
    private BigDecimal netPayoutToStore;

    // Optional: gửi luôn JSON gốc để FE muốn show chi tiết thì parse
    private String settlementDetailJson;
}
