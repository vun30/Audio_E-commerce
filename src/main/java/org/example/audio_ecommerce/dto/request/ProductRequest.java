package org.example.audio_ecommerce.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * DTO tạo/cập nhật sản phẩm
 * Hỗ trợ MULTI CATEGORY + ATTRIBUTE VALUES
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequest {

    // =========================================================
    // 🔗 MULTI CATEGORY (NEW)
    // =========================================================
    @Schema(
            description = "Danh sách ID danh mục sản phẩm",
            example = "[\"550e8400-e29b-41d4-a716-446655440001\", \"550e8400-e29b-41d4-a716-446655440002\"]"
    )
    private List<UUID> categoryIds;

    // =========================================================
    // 🔖 Thương hiệu & SKU
    // =========================================================
    @Schema(description = "Tên thương hiệu", example = "Sony")
    private String brandName;

    @Schema(description = "Mã SKU phải duy nhất trong từng store", example = "SONY-SPK-001")
    private String sku;

    // =========================================================
    // 📝 Thông tin cơ bản
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

    private String warrantyPeriod;
    // 📝 NOTE: Thời gian bảo hành | Ví dụ: `24 tháng`

    private String warrantyType;
    // 📝 NOTE: Loại BH | Ví dụ: `1 đổi 1`, `Sửa chữa`

    // =========================================================
    // 📸 Media
    // =========================================================
    private List<String> images;
    private String videoUrl;

    // =========================================================
    // 💰 Giá
    // =========================================================
    @Schema(description = "Giá sản phẩm (nếu không có biến thể)", example = "1500000")
    private BigDecimal price;

    private String currency;
    private Integer stockQuantity;
    private String warehouseLocation;

    // =========================================================
    // 🌍 Địa chỉ hành chính
    // =========================================================
    private String provinceCode;
    private String districtCode;
    private String wardCode;
    private String shippingAddress;

    // =========================================================
    // 🚚 Vận chuyển
    // =========================================================
    @Schema(description = "Phí vận chuyển mặc định", example = "30000")
    private BigDecimal shippingFee;

    private List<UUID> supportedShippingMethodIds;

    // =========================================================
    // 🧩 BIẾN THỂ SẢN PHẨM
    // =========================================================
    private List<VariantRequest> variants;

    // =========================================================
    // 🧮 Mua nhiều giảm giá
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
    // 🏷️ THUỘC TÍNH ĐỘNG (Dynamic Attributes)
    // =========================================================
    @Schema(
            description = "Danh sách thuộc tính kỹ thuật",
            example = "[{\"attributeId\": \"550e8400-e29b-41d4-a716-446655440002\", \"value\": \"20Hz - 20000Hz\"}]"
    )
    private List<ProductAttributeValueRequest> attributeValues;
}
