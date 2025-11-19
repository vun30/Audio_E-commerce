package org.example.audio_ecommerce.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.audio_ecommerce.entity.Enum.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerOrderDetailResponse {
    private UUID id;
    private String orderCode;
    private OrderStatus status;
    private String message;
    private LocalDateTime createdAt;
    private BigDecimal totalAmount;
    private BigDecimal discountTotal;
    private BigDecimal shippingFeeTotal;
    private BigDecimal grandTotal;
    private String externalOrderCode;
    private String receiverName;
    private String phoneNumber;
    private String country;
    private String province;
    private String district;
    private String ward;
    private String street;
    private String addressLine;
    private String postalCode;
    private String note;
    private List<StoreOrderSummary> storeOrders;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StoreOrderSummary {
        private UUID id;
        private String orderCode;
        private UUID storeId;
        private String storeName;
        private OrderStatus status;
        private LocalDateTime createdAt;
        private BigDecimal totalAmount;
        private BigDecimal discountTotal;
        private BigDecimal shippingFee;
        private BigDecimal grandTotal;
        private List<StoreOrderItemResponse> items;
    }
}