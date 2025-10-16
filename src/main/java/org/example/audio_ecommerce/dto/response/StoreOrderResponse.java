package org.example.audio_ecommerce.dto.response;

import lombok.Builder;
import lombok.Data;
import org.example.audio_ecommerce.entity.Enum.OrderStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class StoreOrderResponse {
    private UUID id;
    private UUID storeId;
    private OrderStatus status;
    private LocalDateTime createdAt;
    // Thêm các trường khác nếu cần
}

