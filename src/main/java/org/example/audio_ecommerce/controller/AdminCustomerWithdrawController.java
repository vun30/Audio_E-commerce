package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.AdminDecisionRequest;
import org.example.audio_ecommerce.dto.request.AdminWithdrawMarkPaidRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.dto.response.CustomerWithdrawResponse;
import org.example.audio_ecommerce.entity.Enum.WithdrawRequestStatus;
import org.example.audio_ecommerce.service.AdminWithdrawService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/customer-withdraw-requests")
public class AdminCustomerWithdrawController {

    private final AdminWithdrawService service;

    @Operation(
            summary = "Admin xem danh sách yêu cầu rút tiền của Customer.",
            description = """
            API cho Admin xem danh sách các yêu cầu rút tiền từ ví Customer.

            Hỗ trợ:
            - Lọc theo trạng thái (PENDING / APPROVED / REJECTED / PAID)
            - Phân trang (page, size)
            """
    )
    @ApiResponse(responseCode = "200", description = "Lấy danh sách yêu cầu rút tiền thành công")
    @GetMapping
    public BaseResponse<Page<CustomerWithdrawResponse>> list(
            @RequestParam(required = false) WithdrawRequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return BaseResponse.success("✅ Xem thành công", service.adminList(status, page, size));
    }

    @Operation(
            summary = "Admin xem chi tiết yêu cầu rút tiền",
            description = """
            API cho Admin xem chi tiết một yêu cầu rút tiền cụ thể.
            Bao gồm proofUrls (link ảnh chứng minh), payoutRef, adminNote.
            """
    )
    @ApiResponse(responseCode = "200", description = "Lấy chi tiết yêu cầu rút tiền thành công")
    @GetMapping("/{id}")
    public BaseResponse<CustomerWithdrawResponse> detail(@PathVariable UUID id) {
        return BaseResponse.success("✅ Xem thành công", service.adminGet(id));
    }

    @Operation(
            summary = "Admin duyệt yêu cầu rút tiền",
            description = """
            Chỉ duyệt khi trạng thái hiện tại là PENDING -> APPROVED
            """
    )
    @ApiResponse(responseCode = "200", description = "Duyệt yêu cầu rút tiền thành công")
    @PostMapping("/{id}/approve")
    public BaseResponse<CustomerWithdrawResponse> approve(
            @PathVariable UUID id,
            @RequestBody AdminDecisionRequest req
    ) {
        return BaseResponse.success("✅ Duyệt thành công", service.approve(id, req));
    }

    @Operation(
            summary = "Admin từ chối yêu cầu rút tiền",
            description = """
            Chỉ từ chối khi trạng thái hiện tại là PENDING -> REJECTED.
            Hệ thống hoàn tiền: pendingBalance giảm, balance tăng.
            """
    )
    @ApiResponse(responseCode = "200", description = "Từ chối yêu cầu rút tiền thành công")
    @PostMapping("/{id}/reject")
    public BaseResponse<CustomerWithdrawResponse> reject(
            @PathVariable UUID id,
            @RequestBody AdminDecisionRequest req
    ) {
        return BaseResponse.success("✅ Từ chối thành công", service.reject(id, req));
    }

    @Operation(
            summary = "Admin xác nhận đã chuyển tiền (mark PAID) + gửi kèm ảnh chứng minh (URL)",
            description = """
            API cho Admin xác nhận đã chuyển tiền cho Customer.

            Điều kiện:
            - Yêu cầu phải ở trạng thái APPROVED
            - Bắt buộc gửi proofUrls (URL ảnh chứng minh) trong request body
            - Khi PAID: pendingBalance giảm (balance không đổi vì đã trừ lúc tạo request)
            """
    )
    @ApiResponse(responseCode = "200", description = "Xác nhận PAID thành công")
    @PostMapping("/{id}/paid")
    public BaseResponse<CustomerWithdrawResponse> paid(
            @PathVariable UUID id,
            @Valid @RequestBody AdminWithdrawMarkPaidRequest req
    ) {
        return BaseResponse.success("✅ Xác nhận chuyển tiền thành công", service.markPaid(id, req));
    }
}
