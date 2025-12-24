package org.example.audio_ecommerce.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.example.audio_ecommerce.entity.Enum.ProductStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {

    // =========================================================
    // 🔑 THÔNG TIN ĐỊNH DANH
    // =========================================================
    private UUID productId;
    private UUID storeId;
    private String storeName;

    // =========================================================
    // 🔗 CATEGORY (MULTI CATEGORY)
    // =========================================================
    @Schema(description = "Danh sách category của sản phẩm")
    private List<CategoryResponse> categories;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryResponse {
        private UUID categoryId;
        private String categoryName;
    }

    // =========================================================
    // 🏷️ THÔNG TIN CƠ BẢN
    // =========================================================
    private String brandName;
    private String name;
    private String slug;
    private String shortDescription;
    private String description;
    private String model;
    private String color;
    private String material;
    private String dimensions;
    private BigDecimal weight;

    private String warrantyPeriod;
    // 📝 NOTE: Thời gian bảo hành | Ví dụ: `24 tháng`

    private String warrantyType;
    // 📝 NOTE: Loại BH | Ví dụ: `1 đổi 1`, `Sửa chữa`

    // =========================================================
    // 🔗 BIẾN THỂ
    // =========================================================
    private List<VariantResponse> variants;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VariantResponse {
        private UUID variantId;
        private String optionName;
        private String optionValue;
        private BigDecimal variantPrice;
        private Integer variantStock;
        private String variantUrl;
        private String variantSku;
    }

    // =========================================================
    // 📸 MEDIA
    // =========================================================
    private List<String> images;
    private String videoUrl;

    // =========================================================
    // 💰 GIÁ
    // =========================================================
    private String sku;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private BigDecimal promotionPercent;
    private BigDecimal priceAfterPromotion;
    private BigDecimal priceBeforeVoucher;
    private BigDecimal voucherAmount;
    private BigDecimal finalPrice;
    private BigDecimal platformFeePercent;
    private String currency;
    private Integer stockQuantity;
    private String warehouseLocation;
    private String approvalReason;
    // 📝 NOTE: Lý do admin chỉnh sửa | Ví dụ: `Cập nhật giá theo thị trường`


    // =========================================================
    // 🌍 ĐỊA CHỈ ADMIN
    // =========================================================
    private String provinceCode;
    private String districtCode;
    private String wardCode;
    private String shippingAddress;

    // =========================================================
    // 🚚 VẬN CHUYỂN
    // =========================================================
    private BigDecimal shippingFee;
    private List<UUID> supportedShippingMethodIds;

    // =========================================================
    // 🧮 GIẢM GIÁ SỐ LƯỢNG
    // =========================================================
    private List<BulkDiscountResponse> bulkDiscounts;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkDiscountResponse {
        private Integer fromQuantity;
        private Integer toQuantity;
        private BigDecimal unitPrice;
    }

    // =========================================================
    // 📊 TRẠNG THÁI & ĐÁNH GIÁ
    // =========================================================
    private ProductStatus status;
    private Boolean isFeatured;
    private BigDecimal ratingAverage;
    private Integer reviewCount;
    private Integer viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastUpdatedAt;
    private Long lastUpdateIntervalDays;
    private UUID createdBy;
    private UUID updatedBy;

    // =========================================================
    // 🏷️ ATTRIBUTE VALUES (KỸ THUẬT)
    // =========================================================
    @Schema(description = "Danh sách thuộc tính kỹ thuật của sản phẩm")
    private List<ProductAttributeValueResponse> attributeValues;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductAttributeValueResponse {
        private UUID attributeId;
        private String attributeName;
        private String attributeLabel;
        private String dataType;   // STRING / NUMBER / BOOLEAN / ENUM / JSON
        private String value;      // Giá trị nhập
    }
}
