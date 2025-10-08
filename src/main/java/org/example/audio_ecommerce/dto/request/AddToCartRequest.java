package org.example.audio_ecommerce.dto.request;

import lombok.Data;
import java.util.UUID;

@Data
public class AddToCartRequest {
    /** Chỉ truyền 1 trong 2 field: productId hoặc comboId */
    private UUID productId;
    private UUID comboId;
    private Integer quantity = 1;
}
