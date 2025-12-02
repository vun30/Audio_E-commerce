package org.example.audio_ecommerce.dto.response;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductComboResponse {

    private UUID comboId;
    private String categoryName; // luôn "COMBO"

    private String name;
    private String shortDescription;
    private String description;
    private List<String> images;
    private String videoUrl;

    private String provinceCode;
    private String districtCode;
    private String wardCode;

    private String shippingAddress;
    private String warehouseLocation;

    private Integer stockQuantity;
    private UUID storeId;
    private String storeName;

    private String creatorType;   // SHOP_CREATE / CUSTOMER_CREATE
    private UUID creatorId;

    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<Item> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Item {

        private UUID productId;
        private String productName;

        // 🆕 Thông tin biến thể đầy đủ
        private UUID variantId;
        private String optionName;
        private String optionValue;
        private java.math.BigDecimal variantPrice;
        private Integer variantStock;
        private String variantUrl;
        private String variantSku;

        private Integer quantity;
    }
}
