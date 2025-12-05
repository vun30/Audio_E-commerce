package org.example.audio_ecommerce.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerOrderItemResponse {

    private UUID id;
    private String type;          // PRODUCT / COMBO
    private UUID refId;           // productId hoặc comboId
    private String name;
    private int quantity;

    private BigDecimal unitPrice;
    private BigDecimal lineTotal;

    // Thông tin store
    private UUID storeId;
    private UUID storeOrderId;
    // ===== Variant info =====
    private UUID variantId;           // có thể null
    private String variantOptionName; // Color, Size,...
    private String variantOptionValue;// Black, M,...

    private String image;      // ảnh sản phẩm (lấy ảnh đầu tiên)
    private String variantUrl;
}
