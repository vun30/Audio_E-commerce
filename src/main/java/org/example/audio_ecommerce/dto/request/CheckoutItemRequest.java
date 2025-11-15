package org.example.audio_ecommerce.dto.request;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Data;

@Data
public class CheckoutItemRequest {
    private UUID id; // product or combo id
    private String type; // "PRODUCT" or "COMBO"
    private int quantity;
    private BigDecimal variantUnitPrice; // NEW: giá biến thể mà FE chọn (nếu không có biến thể -> null)
}
