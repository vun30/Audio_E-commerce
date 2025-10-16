package org.example.audio_ecommerce.controller;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.StoreOrderStatusUpdateRequest;
import org.example.audio_ecommerce.dto.response.StoreOrderResponse;
import org.example.audio_ecommerce.entity.StoreOrder;
import org.example.audio_ecommerce.service.StoreOrderService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stores/{storeId}/orders")
@RequiredArgsConstructor
public class StoreOrderController {
    private final StoreOrderService storeOrderService;

    @PatchMapping("/{orderId}/status")
    @ResponseStatus(HttpStatus.OK)
    public StoreOrderResponse updateOrderStatus(
            @PathVariable UUID storeId,
            @PathVariable UUID orderId,
            @RequestBody StoreOrderStatusUpdateRequest request
    ) {
        StoreOrder order = storeOrderService.updateOrderStatus(storeId, orderId, request.getStatus());
        StoreOrderResponse resp = new StoreOrderResponse();
        resp.setId(order.getId());
        resp.setStoreId(order.getStore().getStoreId());
        resp.setStatus(order.getStatus());
        return resp;
    }
}
