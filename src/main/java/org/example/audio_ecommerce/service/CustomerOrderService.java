package org.example.audio_ecommerce.service;

import org.example.audio_ecommerce.dto.response.CustomerOrderDetailResponse;
import org.example.audio_ecommerce.dto.response.PagedResult;

import java.util.UUID;

public interface CustomerOrderService {
    PagedResult<CustomerOrderDetailResponse> getCustomerOrders(UUID customerId, int page, int size);
    CustomerOrderDetailResponse getCustomerOrderDetail(UUID customerId, UUID orderId);
}