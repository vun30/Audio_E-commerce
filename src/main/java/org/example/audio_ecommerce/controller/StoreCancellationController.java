package org.example.audio_ecommerce.controller;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.service.OrderCancellationService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stores/{storeId}/orders")
@RequiredArgsConstructor
public class StoreCancellationController {

    private final OrderCancellationService cancellationService;

    // Shop duyệt hủy
    @PostMapping("/{storeOrderId}/cancel/approve")
    public BaseResponse<Void> approveCancel(
            @PathVariable UUID storeId,
            @PathVariable UUID storeOrderId
    ) {
        return cancellationService.shopApproveCancel(storeId, storeOrderId);
    }

    // Shop từ chối hủy
    @PostMapping("/{storeOrderId}/cancel/reject")
    public BaseResponse<Void> rejectCancel(
            @PathVariable UUID storeId,
            @PathVariable UUID storeOrderId,
            @RequestParam(required = false) String note
    ) {
        return cancellationService.shopRejectCancel(storeId, storeOrderId, note);
    }
}
