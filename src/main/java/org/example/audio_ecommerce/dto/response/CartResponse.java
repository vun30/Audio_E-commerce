// CartResponse.java
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
public class CartResponse {
    private UUID cartId;
    private UUID customerId;
    private String status;
    private BigDecimal subtotal;
    private BigDecimal discountTotal;
    private BigDecimal grandTotal;
    private List<Item> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Item {
        private UUID cartItemId;
        private String type; // PRODUCT | COMBO
        private UUID refId;  // productId or comboId
        private String name;
        private String image;
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
        private String originProvinceCode; // từ Product.provinceCode
        private String originDistrictCode; // từ Product.districtCode
        private String originWardCode;     // từ Product.wardCode
        private UUID variantId;
        private String variantOptionName;
        private String variantOptionValue;
        private String variantUrl;
        private BigDecimal baseUnitPrice;          // giá gốc (chưa campaign)
        private BigDecimal platformCampaignPrice;  // giá sau campaign (nếu có)
        private Boolean inPlatformCampaign;        // có đang nằm trong campaign không
        private Boolean campaignUsageExceeded;
        private Integer campaignRemaining;
    }
}
