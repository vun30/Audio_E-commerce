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
public class StoreOrderItemResponse {
    private UUID id;
    private String type;
    private UUID refId;
    private String name;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
}