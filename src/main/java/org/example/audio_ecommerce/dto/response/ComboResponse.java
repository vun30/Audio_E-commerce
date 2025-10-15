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

    // 🆔 Định danh combo
    private UUID comboId;

    // 🏪 Thông tin cửa hàng
    private UUID storeId;
    private String storeName;

    // 📂 Danh mục (mặc định là "Combo")
    private UUID categoryId;
    private String categoryName;

    // 📦 Thông tin combo
    private String name;
    private String shortDescription;
    private String description;

    // 📸 Media
    private List<String> images;
    private String videoUrl;

    // ⚖️ Giao hàng
    private BigDecimal weight;
    private Integer stockQuantity;
    private String shippingAddress;
    private String warehouseLocation;

    // 💰 Giá combo
    private BigDecimal comboPrice;
    private BigDecimal originalTotalPrice;

    // 📊 Trạng thái & thời gian
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 🧩 Danh sách sản phẩm con trong combo
    private List<UUID> includedProductIds;
    private List<String> includedProductNames;
}
