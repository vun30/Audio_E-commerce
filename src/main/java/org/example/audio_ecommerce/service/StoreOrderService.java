package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.entity.Enum.OrderStatus;
import org.example.audio_ecommerce.entity.StoreOrder;
import java.util.UUID;

public interface StoreOrderService {
    StoreOrder updateOrderStatus(UUID storeId, UUID orderId, OrderStatus status);
}

