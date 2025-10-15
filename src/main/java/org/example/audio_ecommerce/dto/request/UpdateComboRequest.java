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

    // ⚙️ Danh mục — BE sẽ tự động giữ/đặt lại theo "Combo"
    private UUID categoryId;

    // 📦 Thông tin cơ bản của combo
    private String name;
    private String shortDescription;
    private String description;

    // 📸 Media (ảnh & video)
    private List<String> images;
    private String videoUrl;

    // ⚖️ Giao hàng
    private BigDecimal weight;
    private Integer stockQuantity;
    private String shippingAddress;
    private String warehouseLocation;

    // 💰 Giá combo & trạng thái
    private BigDecimal comboPrice;
    private Boolean isActive;

    // 🧩 Danh sách sản phẩm con (cập nhật lại combo)
    private List<UUID> includedProductIds;
}
