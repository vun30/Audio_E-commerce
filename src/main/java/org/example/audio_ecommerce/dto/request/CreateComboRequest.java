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

    @Schema(description = "Tự động gán từ token", example = "Không cần nhập")
    private UUID storeId;

    @Schema(description = "Tự động gán category COMBO", example = "Không cần nhập")
    private UUID categoryId;

    private String name;
    private String shortDescription;
    private String description;

    private List<String> images;
    private String videoUrl;

    private BigDecimal weight;
    private Integer stockQuantity;
    private String shippingAddress;
    private String warehouseLocation;

    private BigDecimal comboPrice;

    // 🧩 Danh sách item gửi FULL DATA
    private List<ComboItemRequest> items;
}
