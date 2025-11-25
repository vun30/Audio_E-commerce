package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.response.CustomerOrderDetailResponse;
import org.example.audio_ecommerce.dto.response.PagedResult;
import org.example.audio_ecommerce.entity.Enum.OrderStatus;

import java.util.UUID;

public interface CustomerOrderService {
    PagedResult<CustomerOrderDetailResponse> getCustomerOrders(UUID customerId, OrderStatus status, int page, int size);
    CustomerOrderDetailResponse getCustomerOrderDetail(UUID customerId, UUID orderId);
}