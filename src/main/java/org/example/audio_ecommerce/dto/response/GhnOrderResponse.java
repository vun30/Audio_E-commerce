package org.example.audio_ecommerce.dto.response;

import lombok.*;
import org.example.audio_ecommerce.entity.Enum.GhnStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class GhnOrderResponse {
    private UUID id;
    private UUID storeOrderId;
    private UUID storeId;
    private String orderGhn;
    private BigDecimal totalFee;
    private LocalDateTime expectedDeliveryTime;
    private GhnStatus status;
    private LocalDateTime createdAt;
}
