package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.entity.Enum.CancellationReason;
import org.example.audio_ecommerce.service.OrderCancellationService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Customer Cancellation", description = "Các API khách hàng huỷ đơn hoặc gửi yêu cầu huỷ đơn")
@RestController
@RequestMapping("/api/v1/customers/{customerId}")
@RequiredArgsConstructor
public class CustomerCancellationController {

    private final OrderCancellationService cancellationService;

    @Operation(
            summary = "Khách huỷ toàn bộ đơn (PENDING) → hoàn tiền ngay",
            description = """
                    Khách huỷ toàn bộ `customerOrder` khi đơn **vẫn ở PENDING**:
                    - Hệ thống **refund toàn bộ** từ **PlatformWallet → Wallet (customer)** (không cần shop duyệt).
                    - Tất cả `storeOrder` thuộc đơn chuyển **CANCELLED**, `customerOrder` chuyển **CANCELLED**.
                    - Dùng khi khách vừa đặt xong nhưng đổi ý ngay.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Huỷ & hoàn tiền thành công",
                    content = @Content(schema = @Schema(implementation = BaseResponse.class))),
            @ApiResponse(responseCode = "400", description = "Không thể huỷ ngay (đơn không ở PENDING hoặc không thuộc khách)",
                    content = @Content(schema = @Schema(implementation = BaseResponse.class))),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy đơn hoặc dữ liệu liên quan",
                    content = @Content(schema = @Schema(implementation = BaseResponse.class)))
    })
    @PostMapping("/orders/{customerOrderId}/cancel")
    public BaseResponse<Void> cancelWholeOrderIfPending(
            @Parameter(description = "ID khách hàng đang thao tác", required = true)
            @PathVariable UUID customerId,
            @Parameter(description = "ID đơn hàng của khách (CustomerOrder) cần huỷ", required = true)
            @PathVariable UUID customerOrderId,
            @Parameter(description = "Lý do huỷ (enum)", required = true, example = "CHANGE_MIND")
            @RequestParam @NotNull CancellationReason reason,
            @Parameter(description = "Ghi chú bổ sung", example = "Đặt nhầm phiên bản")
            @RequestParam(required = false) String note
    ) {
        return cancellationService.customerCancelWholeOrderIfPending(customerId, customerOrderId, reason, note);
    }

    @Operation(
            summary = "Khách gửi yêu cầu huỷ đơn (đơn đã AWAITING_SHIPMENT)",
            description = """
                    Khách gửi **yêu cầu huỷ** dựa trên `customerOrderId` (trường hợp hệ thống tách mỗi shop thành một customerOrder riêng).
                    - Chỉ áp dụng khi `storeOrder` tương ứng đang **AWAITING_SHIPMENT**.
                    - Tạo `StoreOrderCancellationRequest` ở trạng thái **REQUESTED** để shop xem xét.
                    - Khi shop **approve**: refund **phần tiền** của `storeOrder` từ **PlatformWallet → Wallet (customer)** và đánh dấu `storeOrder` **CANCELLED**.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tạo yêu cầu huỷ thành công (đang chờ shop duyệt)",
                    content = @Content(schema = @Schema(implementation = BaseResponse.class))),
            @ApiResponse(responseCode = "400", description = "Không thể gửi yêu cầu (đơn không ở AWAITING_SHIPMENT, không thuộc khách, v.v.)",
                    content = @Content(schema = @Schema(implementation = BaseResponse.class))),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy đơn hoặc dữ liệu liên quan",
                    content = @Content(schema = @Schema(implementation = BaseResponse.class)))
    })
    @PostMapping("/orders/{customerOrderId}/cancel-request")
    public BaseResponse<Void> requestCancelStoreOrderByCustomerOrderId(
            @Parameter(description = "ID khách hàng đang thao tác", required = true)
            @PathVariable UUID customerId,
            @Parameter(description = "ID đơn hàng của khách (CustomerOrder) muốn gửi yêu cầu huỷ", required = true)
            @PathVariable UUID customerOrderId,
            @Parameter(description = "Lý do huỷ (enum)", required = true, example = "FOUND_BETTER_PRICE")
            @RequestParam @NotNull CancellationReason reason,
            @Parameter(description = "Ghi chú bổ sung cho yêu cầu huỷ", example = "Muốn thay đổi địa chỉ giao")
            @RequestParam(required = false) String note
    ) {
        return cancellationService.customerRequestCancelStoreOrderByCustomerOrderId(
                customerId, customerOrderId, reason, note
        );
    }
}
