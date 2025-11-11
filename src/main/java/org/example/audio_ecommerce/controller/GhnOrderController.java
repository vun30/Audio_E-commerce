package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.CreateGhnOrderRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.dto.response.GhnOrderResponse;
import org.example.audio_ecommerce.service.GhnOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "GHN Order", description = "Các API thao tác GHN")
@RestController
@RequestMapping("/api/v1/ghn-orders")
@RequiredArgsConstructor
public class GhnOrderController {

    private final GhnOrderService ghnOrderService;

    @Operation(summary = "Tạo mới GHN Order (nhập toàn bộ thông tin)")
    @PostMapping
    public ResponseEntity<BaseResponse<GhnOrderResponse>> create(@RequestBody CreateGhnOrderRequest req) {
        GhnOrderResponse resp = ghnOrderService.create(req);
        return ResponseEntity.ok(BaseResponse.success("Tạo thành công !",resp));
    }

    @Operation(summary = "Lấy GHN Order theo storeOrderId")
    @GetMapping("/by-store-order/{storeOrderId}")
    public ResponseEntity<BaseResponse<GhnOrderResponse>> getByStoreOrderId(
            @PathVariable UUID storeOrderId
    ) {
        GhnOrderResponse resp = ghnOrderService.getByStoreOrderId(storeOrderId);
        return ResponseEntity.ok(BaseResponse.success("Lấy danh sách thành công",resp));
    }
}
