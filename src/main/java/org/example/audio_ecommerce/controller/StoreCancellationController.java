package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.entity.StoreOrderCancellationRequest;
import org.example.audio_ecommerce.service.OrderCancellationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Store Cancellation", description = "Các API xử lý yêu cầu huỷ đơn của shop (duyệt / từ chối)")
@RestController
@RequestMapping("/api/v1/stores/{storeId}/orders")
@RequiredArgsConstructor
public class StoreCancellationController {

    private final OrderCancellationService cancellationService;

    @Operation(
            summary = "Shop duyệt yêu cầu huỷ đơn",
            description = """
                    Shop phê duyệt một yêu cầu huỷ của khách khi đơn **đang ở AWAITING_SHIPMENT**.
                    Khi duyệt:
                    - Hệ thống **refund phần tiền** của `storeOrder` từ **PlatformWallet → Wallet (customer)**.
                    - Ví shop **giảm pending** (gỡ hold), **không** tăng available.
                    - `storeOrder` chuyển sang **CANCELLED**. Nếu đơn tổng chỉ chứa store này thì `customerOrder` cũng thành CANCELLED.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Duyệt huỷ thành công",
                    content = @Content(schema = @Schema(implementation = BaseResponse.class))),
            @ApiResponse(responseCode = "400", description = "Yêu cầu không hợp lệ (không thuộc shop, sai trạng thái, v.v.)",
                    content = @Content(schema = @Schema(implementation = BaseResponse.class))),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy đơn hoặc dữ liệu liên quan",
                    content = @Content(schema = @Schema(implementation = BaseResponse.class)))
    })
    @PostMapping("/{storeOrderId}/cancel/approve")
    public BaseResponse<Void> approveCancel(
            @Parameter(description = "ID của shop đang thao tác", required = true)
            @PathVariable UUID storeId,
            @Parameter(description = "ID đơn của shop (StoreOrder) cần duyệt huỷ", required = true)
            @PathVariable UUID storeOrderId
    ) {
        return cancellationService.shopApproveCancel(storeId, storeOrderId);
    }

    @Operation(
            summary = "Shop từ chối yêu cầu huỷ đơn",
            description = """
                    Shop từ chối một yêu cầu huỷ của khách khi đơn **đang ở AWAITING_SHIPMENT**.
                    Khi từ chối:
                    - Không thay đổi dòng tiền/settlement.
                    - Ghi chú lý do (nếu có) vào yêu cầu huỷ.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Từ chối huỷ thành công",
                    content = @Content(schema = @Schema(implementation = BaseResponse.class))),
            @ApiResponse(responseCode = "400", description = "Yêu cầu không hợp lệ (không thuộc shop, không có request chờ duyệt, v.v.)",
                    content = @Content(schema = @Schema(implementation = BaseResponse.class))),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy đơn hoặc dữ liệu liên quan",
                    content = @Content(schema = @Schema(implementation = BaseResponse.class)))
    })
    @PostMapping("/{storeOrderId}/cancel/reject")
    public BaseResponse<Void> rejectCancel(
            @Parameter(description = "ID của shop đang thao tác", required = true)
            @PathVariable UUID storeId,
            @Parameter(description = "ID đơn của shop (StoreOrder) cần từ chối huỷ", required = true)
            @PathVariable UUID storeOrderId,
            @Parameter(description = "Ghi chú từ chối (tuỳ chọn)", example = "Đơn đã đóng gói, vui lòng liên hệ CSKH")
            @RequestParam(required = false) String note
    ) {
        return cancellationService.shopRejectCancel(storeId, storeOrderId, note);
    }

    @Operation(
            summary = "Shop xem các yêu cầu huỷ của một StoreOrder",
            description = """
                    Lấy danh sách tất cả `StoreOrderCancellationRequest` gắn với một `storeOrder`.
                    Dùng cho màn hình quản lý huỷ đơn của shop.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy danh sách yêu cầu huỷ thành công",
                    content = @Content(schema = @Schema(implementation = BaseResponse.class))),
            @ApiResponse(responseCode = "400", description = "Đơn không thuộc shop",
                    content = @Content(schema = @Schema(implementation = BaseResponse.class))),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy đơn hoặc dữ liệu liên quan",
                    content = @Content(schema = @Schema(implementation = BaseResponse.class)))
    })
    @GetMapping("/{storeOrderId}/cancel-requests")
    public BaseResponse<List<StoreOrderCancellationRequest>> getCancelRequestsForStoreOrder(
            @Parameter(description = "ID của shop đang thao tác", required = true)
            @PathVariable UUID storeId,
            @Parameter(description = "ID đơn của shop (StoreOrder) cần xem request huỷ", required = true)
            @PathVariable UUID storeOrderId
    ) {
        var list = cancellationService.getStoreCancellationRequests(storeId, storeOrderId);
        return BaseResponse.success("Fetched cancellation requests", list);
    }

    @Operation(
            summary = "Shop xem tất cả request huỷ của các đơn thuộc shop",
            description = """
                    Lấy tất cả `StoreOrderCancellationRequest` của các `storeOrder` thuộc store.
                    Dùng cho màn hình quản lý yêu cầu huỷ theo shop.
                    """
    )
    @GetMapping("/cancel-requests")
    public BaseResponse<List<StoreOrderCancellationRequest>> getAllStoreCancelRequests(
            @Parameter(description = "ID của shop đang thao tác", required = true)
            @PathVariable UUID storeId
    ) {
        var list = cancellationService.getAllStoreCancellationRequests(storeId);
        return BaseResponse.success("Fetched store cancellation requests", list);
    }


}
