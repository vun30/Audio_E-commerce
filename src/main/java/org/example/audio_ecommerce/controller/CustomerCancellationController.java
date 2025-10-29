package org.example.audio_ecommerce.controller;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.entity.Enum.CancellationReason;
import org.example.audio_ecommerce.service.OrderCancellationService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers/{customerId}")
@RequiredArgsConstructor
public class CustomerCancellationController {

    private final OrderCancellationService cancellationService;

    // KH hủy toàn bộ đơn nếu còn PENDING -> refund ngay
    @PostMapping("/orders/{customerOrderId}/cancel")
    public BaseResponse<Void> cancelWholeOrderIfPending(
            @PathVariable UUID customerId,
            @PathVariable UUID customerOrderId,
            @RequestParam @NotNull CancellationReason reason,
            @RequestParam(required = false) String note
    ) {
        return cancellationService.customerCancelWholeOrderIfPending(customerId, customerOrderId, reason, note);
    }

    // KH gửi yêu cầu hủy dựa trên customerOrderId (mặc định map sang đúng store-order duy nhất)
    @PostMapping("/orders/{customerOrderId}/cancel-request")
    public BaseResponse<Void> requestCancelStoreOrderByCustomerOrderId(
            @PathVariable UUID customerId,
            @PathVariable UUID customerOrderId,
            @RequestParam @NotNull CancellationReason reason,
            @RequestParam(required = false) String note
    ) {
        return cancellationService.customerRequestCancelStoreOrderByCustomerOrderId(
                customerId, customerOrderId, reason, note
        );
    }
}


