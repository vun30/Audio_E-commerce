package org.example.audio_ecommerce.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
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

    // 🏪 ID cửa hàng — BE tự động gán từ JWT
    @Schema(description = "Không cần nhâp từ FE", example = "Không cần nhâp từ FE")
    private UUID storeId;

    // ⚙️ Danh mục — BE tự động gán theo category có name = "Combo"
    @Schema(description = "Không cần nhâp từ FE", example = "Không cần nhâp từ FE")
    private UUID categoryId;

    // 📦 Thông tin cơ bản của combo
    private String name;
    private String shortDescription;
    private String description;

    // 📸 Hình ảnh & video giới thiệu
    private List<String> images;
    private String videoUrl;

    // ⚖️ Thông tin giao hàng
    private BigDecimal weight;
    private Integer stockQuantity;
    private String shippingAddress;
    private String warehouseLocation;

    // 💰 Giá combo (giá bán gộp)
    private BigDecimal comboPrice;

    // 🧩 Danh sách sản phẩm con thuộc combo
    private List<UUID> includedProductIds;
}
