package org.example.audio_ecommerce.dto.request;

import lombok.Data;
import org.example.audio_ecommerce.entity.Enum.OrderStatus;

@Data
public class StoreOrderStatusUpdateRequest {
    private OrderStatus status;
}