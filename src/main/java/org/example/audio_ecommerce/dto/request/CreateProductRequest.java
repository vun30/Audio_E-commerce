package org.example.audio_ecommerce.dto.request;

import lombok.*;
import org.example.audio_ecommerce.entity.Enum.ProductStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateProductRequest {

    // =========================================================
    // 🔗 LIÊN KẾT
    // =========================================================
    private UUID storeId;                      // FE gửi lên
    private List<UUID> categoryIds;            // Product có nhiều Category

    // =========================================================
    // 🏷️ THÔNG TIN CƠ BẢN
    // =========================================================
    private String brandName;                  // nếu có brand table thì đổi sang brandId
    private String name;
    private String slug;
    private String shortDescription;
    private String description;
    private String model;
    private String color;
    private String material;
    private String dimensions;
    private BigDecimal weight;

    // =========================================================
    // 📸 MEDIA
    // =========================================================
    private List<String> images;
    private String videoUrl;

    // =========================================================
    // 💰 GIÁ & TỒN KHO
    // =========================================================
    private String sku;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private String currency;
    private Integer stockQuantity;
    private String warehouseLocation;

    // =========================================================
    // 🌍 ĐỊA CHỈ GIAO HÀNG
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
    // 🧩 BIẾN THỂ
    // =========================================================
    private List<VariantRequest> variants;

    // =========================================================
    // 🧮 MUA NHIỀU GIẢM GIÁ
    // =========================================================
    private List<BulkDiscountRequest> bulkDiscounts;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkDiscountRequest {
        private Integer fromQuantity;
        private Integer toQuantity;
        private BigDecimal unitPrice;
    }

    // =========================================================
    // 📦 THUỘC TÍNH ĐỘNG (CategoryAttribute)
    // =========================================================
    private List<ProductAttributeValueRequest> attributeValues;

    // =========================================================
    // 📊 TRẠNG THÁI & AUDIT
    // =========================================================
    private ProductStatus status;
    private Boolean isFeatured;
    private UUID createdBy;
    private UUID updatedBy;
}
