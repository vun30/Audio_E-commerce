package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.response.PagedResult;
import org.example.audio_ecommerce.dto.response.StoreOrderDetailResponse;
import org.example.audio_ecommerce.dto.response.StoreOrderResponse;
import org.example.audio_ecommerce.entity.Enum.OrderStatus;
import org.example.audio_ecommerce.entity.StoreOrder;
import java.util.UUID;

public interface StoreOrderService {
    StoreOrder updateOrderStatus(UUID storeId, UUID orderId, OrderStatus status);
    PagedResult<StoreOrderDetailResponse> getOrdersForStore(UUID storeId, int page, int size, String orderCodeKeyword );
    StoreOrderDetailResponse getOrderDetailForStore(UUID storeId, UUID orderId);
}

