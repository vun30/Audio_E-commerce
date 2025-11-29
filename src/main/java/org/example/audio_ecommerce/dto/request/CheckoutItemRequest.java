package org.example.audio_ecommerce.dto.request;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Data;

@Data
public class CheckoutItemRequest {
    // PRODUCT: nhận productId / variantId (1 trong 2 có thể null)
    private UUID productId;
    private UUID variantId;

    // COMBO: dùng comboId
    private UUID comboId;
    private String type; // "PRODUCT" or "COMBO"
    private int quantity;
}
