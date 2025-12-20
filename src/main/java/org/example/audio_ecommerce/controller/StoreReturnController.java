package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.ReturnCreateGhnOrderRequest;
import org.example.audio_ecommerce.dto.request.ReturnDisputeRequest;
import org.example.audio_ecommerce.dto.request.ReturnRejectRequest;
import org.example.audio_ecommerce.dto.request.ReturnShopReceiveRequest;
import org.example.audio_ecommerce.dto.response.ReturnRequestResponse;
import org.example.audio_ecommerce.service.ReturnRequestService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/store/returns")
@RequiredArgsConstructor
public class StoreReturnController {

    private final ReturnRequestService returnService;

    @Operation(
            summary = "Shop xem danh sách yêu cầu hoàn",
            description = """
                ✅ FE Shop dùng cho màn 'Return requests'.
                
                🔒 ShopId tự lấy từ JWT (không truyền shopId).
                
                Status thường gặp:
                - PENDING: mới tạo
                - APPROVED/REJECTED: shop phản hồi
                - SHIPPING/IN_TRANSIT: đã tạo GHN return order
                - RECEIVED: shop đã nhận hàng
                - DISPUTE_ESCALATED: shop dispute lên admin
                - RESOLVED_*: admin xử xong
                
                Pagination: page/size.
                """
    )
    @GetMapping
    public Page<ReturnRequestResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return returnService.listForCurrentShop(pageable);
    }

    @Operation(
            summary = "Shop chấp nhận yêu cầu hoàn",
            description = """
                ✅ FE Shop dùng khi bấm 'Approve'.
                
                Action:
                - ReturnRequest.status chuyển sang APPROVED (hoặc state tương ứng)
                - Cho phép bước tiếp theo tạo GHN order.
                
                Next step:
                - gọi /create-ghn-order để tạo đơn vận chuyển hoàn.
                """
    )
    @PostMapping("/{id}/approve")
    public void approve(@PathVariable UUID id) {
        returnService.approveReturnByShop(id);
    }

    @Operation(
            summary = "Shop tạo GHN return order",
            description = """
                ✅ FE Shop dùng khi đã approve và muốn tạo đơn GHN để khách gửi hàng về shop.
                
                Input:
                - req có thể null (nếu dùng default)
                - nếu có: thông tin pickup/notes/serviceType...
                
                Output:
                - ReturnRequestResponse có ghnOrderCode + trạng thái tracking ban đầu.
                
                Lưu ý:
                - Nếu customer đã set package-info, phí ship return sẽ được dùng ở bước này.
                """
    )
    @PostMapping("/{id}/create-ghn-order")
    public ReturnRequestResponse createGhnOrder(@PathVariable UUID id,@RequestBody(required = false) ReturnCreateGhnOrderRequest req) {
        return returnService.createGhnReturnOrderByShop(id, req);
    }

    @Operation(
            summary = "Shop xác nhận đã nhận hàng hoàn hoặc dispute",
            description = """
                ✅ FE Shop dùng khi hàng hoàn về đến shop.
                
                Input:
                - received=true => shop xác nhận nhận hàng (đi tới bước refund)
                - received=false + disputeReason/media => dispute lên admin
                
                Output:
                - ReturnRequestResponse cập nhật status
                """
    )
    @PostMapping("/{id}/receive-or-dispute")
    public ReturnRequestResponse shopReceiveOrDispute(
            @PathVariable UUID id,
            @Valid @RequestBody ReturnShopReceiveRequest req
    ) {
        return returnService.shopReceiveOrDispute(id, req);
    }

    @Operation(
            summary = "Shop từ chối yêu cầu hoàn",
            description = """
                ✅ FE Shop dùng khi bấm Reject.
                
                Input:
                - req optional: lý do reject, ghi chú, media...
                
                Nếu customer không đồng ý:
                - customer có thể tạo complaint hoặc dispute (tuỳ flow FE).
                """
    )
    @PostMapping("/{id}/reject")
    public void reject(
            @PathVariable UUID id,
            @RequestBody(required = false) ReturnRejectRequest req
    ) {
        returnService.rejectReturnByShop(id, req);
    }

    @Operation(
            summary = "Shop hoàn tiền ngay, không yêu cầu khách gửi hàng",
            description = """
                ✅ FE Shop dùng trong trường hợp:
                - hàng lỗi nhỏ / shop muốn goodwill
                - không cần logistics return
                
                Action:
                - Refund tiền cho customer theo snapshot amountCharged
                - ReturnRequest chuyển sang REFUNDED / DONE (tuỳ enum)
                """
    )
    @PostMapping("/{id}/refund-without-return")
    public ReturnRequestResponse refundWithoutReturn(@PathVariable UUID id) {
        return returnService.refundWithoutReturnByShop(id);
    }

    @Operation(
            summary = "Shop dispute lên Admin xử lý ngay khi customer gửi yêu cầu lên và nó kêu lỗi do shop mà thực chất lỗi do nó",
            description = """
                ✅ FE Shop dùng khi shop và customer không thoả thuận được.
                
                Action:
                - status = DISPUTE_ESCALATED
                - admin sẽ xem ở /api/admin/returns/disputes
                """
    )
    @PostMapping("/{id}/dispute")
    public ReturnRequestResponse disputeToAdmin(
            @PathVariable UUID id,
            @Valid @RequestBody ReturnDisputeRequest req
    ) {
        return returnService.disputeToAdmin(id, req);
    }


}
