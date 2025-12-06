package org.example.audio_ecommerce.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class TopProductLifetime {

    private UUID productId;
    private String name;
    private long totalSoldQuantity;
    private BigDecimal totalRevenue;
}
