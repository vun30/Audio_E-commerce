package org.example.audio_ecommerce.dto.request;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateComboRequest {

    private UUID storeId;
    private UUID categoryId;

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

    // 📦 Sản phẩm con
    private List<UUID> includedProductIds;
}
