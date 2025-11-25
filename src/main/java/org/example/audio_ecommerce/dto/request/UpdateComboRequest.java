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
public class UpdateComboRequest {

    // ⚙️ Danh mục — BE giữ theo category COMBO, FE không cần gửi
    private UUID categoryId;

    // 📦 Thông tin cơ bản
    private String name;
    private String shortDescription;
    private String description;

    // 📸 Media
    private List<String> images;
    private String videoUrl;

    // ⚖️ Giao hàng & tồn kho
    private BigDecimal weight;
    private Integer stockQuantity;
    private String shippingAddress;
    private String warehouseLocation;

    // 💰 Giá combo và trạng thái
    private BigDecimal comboPrice;
    private Boolean isActive;

    // 🧩 DANH SÁCH ITEM MỚI — FULL VARIANT DATA
    private List<ComboItemRequest> items;
}
