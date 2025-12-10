    // package org.example.audio_ecommerce.dto.response;
    package org.example.audio_ecommerce.dto.response;

    import lombok.Builder;
    import lombok.Data;

    import java.math.BigDecimal;
    import java.time.LocalDateTime;
    import java.util.UUID;

    @Data
    @Builder
    public class StoreWalletItemResponse {

        private UUID storeOrderItemId;
        private UUID storeOrderId;
        private String orderCode;
        private LocalDateTime orderCreatedAt;

        private String productName;
        private String variantOptionName;
        private String variantOptionValue;
        private int quantity;

        // Tiền
        private BigDecimal grossAmount;     // tiền khách trả cho item (sau giảm giá, chưa trừ phí)
        private BigDecimal platformFee;     // phí nền tảng trên item
        private BigDecimal shippingExtra;   // ship chênh lệch mà shop phải gánh
        private BigDecimal costOfGoods;     // giá vốn
        private BigDecimal netProfit;       // lãi ròng của item

        // Trạng thái
        private Boolean eligibleForPayout;
        private Boolean isPayout;
        private Boolean isReturned;
        private String orderStatus;         // PENDING/DELIVERING/...
    }
