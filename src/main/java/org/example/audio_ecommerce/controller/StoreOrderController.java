package org.example.audio_ecommerce.controller;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.StoreOrderStatusUpdateRequest;
import org.example.audio_ecommerce.dto.response.*;
import org.example.audio_ecommerce.entity.Enum.OrderStatus;
import org.example.audio_ecommerce.entity.StoreOrder;
import org.example.audio_ecommerce.repository.StoreOrderRepository;
import org.example.audio_ecommerce.service.StoreOrderService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stores/{storeId}/orders")
@RequiredArgsConstructor
public class StoreOrderController {

    private final StoreOrderService storeOrderService;
    private final StoreOrderRepository storeOrderRepo;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @GetMapping
    public PagedResult<StoreOrderDetailResponse> listStoreOrders(
            @PathVariable UUID storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String orderCodeKeyword,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return storeOrderService.getOrdersForStore(
                storeId,
                page,
                size,
                orderCodeKeyword,
                status,
                fromDate,
                toDate
        );
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

    @GetMapping("/{id}/settlement")
    @Operation(summary = "Xem chi tiết tiền về (settlement) của 1 StoreOrder")
    public ResponseEntity<BaseResponse<StoreOrderSettlementResponse>> getSettlement(
            @PathVariable("id") UUID storeOrderId
            // @AuthenticationPrincipal ... currentStoreUser -> có thể check quyền ở đây
    ) {
        StoreOrder so = storeOrderRepo.findById(storeOrderId)
                .orElseThrow(() -> new NoSuchElementException("StoreOrder not found: " + storeOrderId));

        // TODO: kiểm tra quyền: store hiện tại có trùng so.getStore().getStoreId() không

        StoreOrderSettlementResponse dto = toSettlementResponse(so);

        return ResponseEntity.ok(
                BaseResponse.success("Thông tin settlement của đơn cửa hàng", dto)
        );
    }

    // ✅ GET detail: GET /api/v1/store/orders/{orderId}?storeId=...
    @GetMapping("/{orderId}")
    public ResponseEntity<StoreOrderDetailResponse> getOrderDetail(
            @RequestParam UUID storeId,
            @PathVariable UUID orderId
    ) {
        StoreOrderDetailResponse res = storeOrderService.getOrderDetailForStore(storeId, orderId);
        return ResponseEntity.ok(res);
    }

    private StoreOrderSettlementResponse toSettlementResponse(StoreOrder so) {
        BigDecimal productsTotal = java.util.Optional.ofNullable(so.getTotalAmount()).orElse(BigDecimal.ZERO);
        BigDecimal discountTotal = java.util.Optional.ofNullable(so.getDiscountTotal()).orElse(BigDecimal.ZERO);
        BigDecimal storeVoucherDiscount = java.util.Optional.ofNullable(so.getStoreVoucherDiscount()).orElse(BigDecimal.ZERO);
        BigDecimal platformVoucherDiscount = java.util.Optional.ofNullable(so.getPlatformVoucherDiscount()).orElse(BigDecimal.ZERO);

        BigDecimal customerShippingFee = java.util.Optional.ofNullable(so.getShippingFee()).orElse(BigDecimal.ZERO);
        BigDecimal actualShippingFee = java.util.Optional.ofNullable(so.getActualShippingFee()).orElse(BigDecimal.ZERO);
        BigDecimal shippingExtraForStore = java.util.Optional.ofNullable(so.getShippingExtraForStore()).orElse(BigDecimal.ZERO);
        BigDecimal platformFeeAmount = java.util.Optional.ofNullable(so.getPlatformFeeAmount()).orElse(BigDecimal.ZERO);
        BigDecimal netPayoutToStore = java.util.Optional.ofNullable(so.getNetPayoutToStore()).orElse(BigDecimal.ZERO);

        BigDecimal platformFeeRate = null;
        if (so.getSettlementDetailJson() != null) {
            try {
                JsonNode root = objectMapper.readTree(so.getSettlementDetailJson());
                if (root.has("platformFeeRate")) {
                    platformFeeRate = new BigDecimal(root.get("platformFeeRate").asText("0"));
                }
            } catch (Exception ignored) {}
        }

        return StoreOrderSettlementResponse.builder()
                .storeOrderId(so.getId())
                .customerOrderId(
                        so.getCustomerOrder() != null ? so.getCustomerOrder().getId() : null
                )
                .storeId(so.getStore() != null ? so.getStore().getStoreId() : null)
                .storeName(so.getStore() != null ? so.getStore().getStoreName() : null)
                .status(so.getStatus() != null ? so.getStatus().name() : null)
                .createdAt(so.getCreatedAt())

                .productsTotal(productsTotal)
                .discountTotal(discountTotal)
                .storeVoucherDiscount(storeVoucherDiscount)
                .platformVoucherDiscount(platformVoucherDiscount)

                .customerShippingFee(customerShippingFee)
                .actualShippingFee(actualShippingFee)
                .shippingExtraForStore(shippingExtraForStore)

                .platformFeeRate(platformFeeRate)
                .platformFeeAmount(platformFeeAmount)

                .netPayoutToStore(netPayoutToStore)

                .settlementDetailJson(so.getSettlementDetailJson())
                .build();
    }
}
