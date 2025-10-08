package org.example.audio_ecommerce.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class CartItemResponse {
    private UUID cartItemId;
    private String itemType;       // PRODUCT / COMBO
    private UUID productId;        // nullable nếu COMBO
    private UUID comboId;          // nullable nếu PRODUCT
    private UUID storeId;          // để group theo shop
    private String displayName;    // tên hiển thị
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
    private Boolean selected;
    private List<String> images;   // ảnh sản phẩm/combo
}
