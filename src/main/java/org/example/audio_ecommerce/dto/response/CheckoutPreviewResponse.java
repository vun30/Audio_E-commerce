package org.example.audio_ecommerce.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutPreviewResponse {

    private BigDecimal overallSubtotal;      // tổng linePriceBeforeDiscount (tất cả store)
    private BigDecimal overallShipping;      // tổng ship
    private BigDecimal overallDiscount;      // tổng store + platform discount
    private BigDecimal overallGrandTotal;    // subtotal + ship - discount

    private List<PerStore> stores;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PerStore {
        private UUID storeId;
        private String storeName;

        private BigDecimal subtotal;         // trước voucher (linePriceBeforeDiscount)
        private BigDecimal shippingFee;

        private BigDecimal platformDiscount;
        private BigDecimal storeDiscount;
        private BigDecimal discountTotal;

        private BigDecimal grandTotal;

        // optional: để FE hiển thị chi tiết voucher
        private String storeVoucherDetailJson;
        private String platformVoucherDetailJson;

        // optional: list item breakdown
        private List<Item> items;
        private Integer shippingServiceTypeId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Item {
        private String type;     // PRODUCT/COMBO
        private UUID refId;      // productId/comboId
        private UUID variantId;

        private String name;
        private String image;

        private Integer quantity;

        private BigDecimal unitPriceBeforeDiscount;
        private BigDecimal linePriceBeforeDiscount;

        private BigDecimal finalUnitPrice;
        private BigDecimal finalLineTotal;
    }
}
