package org.example.audio_ecommerce.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComboResponse {

    // 🆔 Thông tin định danh
    private UUID comboId;

    // 🏪 Thông tin cửa hàng
    private UUID storeId;
    private String storeName;

    // 📂 Danh mục
    private UUID categoryId;
    private String categoryName;

    // 📦 Thông tin cơ bản của combo
    private String name;
    private String shortDescription;
    private String description;

    // 📸 Media
    private List<String> images;
    private String videoUrl;

    // ⚖️ Thông tin giao hàng
    private BigDecimal weight;
    private Integer stockQuantity;
    private String shippingAddress;
    private String warehouseLocation;

    // 💰 Giá combo
    private BigDecimal comboPrice;
    private BigDecimal originalTotalPrice;

    // 📊 Trạng thái
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 📦 Danh sách sản phẩm trong combo
    private List<UUID> includedProductIds;
    private List<String> includedProductNames;
}
