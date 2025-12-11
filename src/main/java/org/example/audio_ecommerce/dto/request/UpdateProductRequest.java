package org.example.audio_ecommerce.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.example.audio_ecommerce.entity.Enum.ProductStatus;



import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * DTO for updating a product
 * Hỗ trợ MULTI CATEGORY + ATTRIBUTE VALUES
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProductRequest {

    // =========================================================
    // 🔗 LIÊN KẾT & DANH MỤC (MULTI CATEGORY - FIXED)
    // =========================================================
    @Schema(description = "Danh sách ID danh mục sản phẩm")
    private List<UUID> categoryIds;

    @Schema(description = "Tên thương hiệu", example = "Sony")
    private String brandName;

    @Schema(description = "Mã SKU (phải duy nhất)", example = "SONY-SPK-001")
    private String sku;

    // =========================================================
    // 🏷️ THÔNG TIN CƠ BẢN
    // =========================================================
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
    // 💰 GIÁ & KHO
    // =========================================================
    private BigDecimal price;
    private BigDecimal discountPrice;
    private String currency;
    private Integer stockQuantity;
    private String warehouseLocation;

    // =========================================================
    // 🌍 ĐỊA CHỈ HÀNH CHÍNH
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
    // 🧩 ATTRIBUTE VALUES (Dynamic)
    // =========================================================
    private List<ProductAttributeValueRequest> attributeValues;

    // =========================================================
    // 🧩 BIẾN THỂ SẢN PHẨM
    // =========================================================

    // ----- ADD -----
    @Schema(description = "Danh sách biến thể cần thêm")
    private List<VariantToAdd> variantsToAdd;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VariantToAdd {
        private String optionName;
        private String optionValue;
        private BigDecimal variantPrice;
        private Integer variantStock;
        private String variantUrl;
        private String variantSku;
    }

    // ----- UPDATE -----
    @Schema(description = "Danh sách biến thể cần cập nhật")
    private List<VariantToUpdate> variantsToUpdate;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VariantToUpdate {
        private UUID variantId;
        private String optionName;
        private String optionValue;
        private BigDecimal variantPrice;
        private Integer variantStock;
        private String variantUrl;
        private String variantSku;
    }

    // ----- DELETE -----
    @Schema(description = "Danh sách ID biến thể cần xoá")
    private List<UUID> variantsToDelete;

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
    // 📊 TRẠNG THÁI
    // =========================================================
    private ProductStatus status;
    private Boolean isFeatured;

    // =========================================================
    // 👤 AUDIT
    // =========================================================
    private UUID updatedBy;
}
