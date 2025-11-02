package org.example.audio_ecommerce.controller;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.StoreOrderStatusUpdateRequest;
import org.example.audio_ecommerce.dto.response.StoreOrderResponse;
import org.example.audio_ecommerce.entity.Enum.OrderStatus;
import org.example.audio_ecommerce.entity.StoreOrder;
import org.example.audio_ecommerce.service.StoreOrderService;
import org.example.audio_ecommerce.dto.response.PagedResult;
import org.example.audio_ecommerce.dto.response.StoreOrderDetailResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stores/{storeId}/orders")
@RequiredArgsConstructor
public class StoreOrderController {

    private final StoreOrderService storeOrderService;

    @GetMapping
    public PagedResult<StoreOrderDetailResponse> listStoreOrders(
            @PathVariable UUID storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return storeOrderService.getOrdersForStore(storeId, page, size);
    }

    @PatchMapping("/{orderId}/status")
    @ResponseStatus(HttpStatus.OK)
    public StoreOrderResponse updateOrderStatus(
            @PathVariable UUID storeId,
            @PathVariable UUID orderId,
            @RequestBody StoreOrderStatusUpdateRequest request
    ) {
        // Gọi service cập nhật trạng thái
        StoreOrder updatedOrder = storeOrderService.updateOrderStatus(storeId, orderId, request.getStatus());

        // Trả response chuẩn
        return StoreOrderResponse.builder()
                .id(updatedOrder.getId())
                .storeId(updatedOrder.getStore().getStoreId())
                .status(updatedOrder.getStatus())
                .createdAt(updatedOrder.getCreatedAt())
                .build();
    }
}
