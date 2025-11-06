package org.example.audio_ecommerce.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderItemResponse {
    private UUID id;
    private String type;       // PRODUCT / COMBO
    private UUID refId;        // productId / comboId
    private String name;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;       // nếu có
}
