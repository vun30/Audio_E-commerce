package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.CustomerWithdrawCreateRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.dto.response.CustomerWithdrawResponse;
import org.example.audio_ecommerce.entity.CustomerWithdrawRequest;
import org.example.audio_ecommerce.entity.Enum.WithdrawRequestStatus;
import org.example.audio_ecommerce.service.CustomerWithdrawService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/customers/{customerId}/withdraw-requests")
public class CustomerWithdrawController {

    private final CustomerWithdrawService service;

    @Operation(
            summary = "Customer tạo yêu cầu rút tiền.",
            description = """
            API cho Customer tạo yêu cầu rút tiền từ ví khách hàng.

            Hỗ trợ:
            - Nhập số tiền muốn rút
            - Thông tin tài khoản nhận tiền (ngân hàng / ví)
            - Kiểm tra số dư ví Customer trước khi tạo yêu cầu

            Trạng thái ban đầu:
            - PENDING (chờ Admin xử lý)

            Nghiệp vụ ví:
            - Khi tạo yêu cầu: balance giảm, pendingBalance tăng (hold tiền chờ admin)
            
            FE dùng cho màn Customer -> Ví -> Rút tiền.
            """
    )
    @ApiResponse(responseCode = "201", description = "Tạo yêu cầu rút tiền thành công")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BaseResponse<CustomerWithdrawRequest> create(
            @PathVariable UUID customerId,
            @Valid @RequestBody CustomerWithdrawCreateRequest req
    ) {
        CustomerWithdrawRequest request = service.create(customerId, req);
        return BaseResponse.success("✅ Gửi yêu cầu thành công", request);
    }

    @Operation(
            summary = "Customer xem lịch sử yêu cầu rút tiền",
            description = """
            API cho Customer xem danh sách các yêu cầu rút tiền của chính mình.

            Hỗ trợ:
            - Lọc theo trạng thái (PENDING / APPROVED / REJECTED / PAID)
            - Phân trang (page, size)

            FE dùng cho màn Customer -> Lịch sử rút tiền.
            """
    )
    @ApiResponse(responseCode = "200", description = "Lấy danh sách yêu cầu rút tiền thành công")
    @GetMapping
    public BaseResponse<Page<CustomerWithdrawResponse>> list(
            @PathVariable UUID customerId,
            @RequestParam(required = false) WithdrawRequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<CustomerWithdrawResponse> result = service.customerList(customerId, status, page, size);
        return BaseResponse.success("✅ Xem thành công", result);
    }

    @Operation(
            summary = "Customer xem chi tiết yêu cầu rút tiền",
            description = """
            API cho Customer xem chi tiết một yêu cầu rút tiền cụ thể.

            Mục đích:
            - Theo dõi trạng thái xử lý của Admin
            - Xem lý do từ chối (nếu có)
            - Xem payoutRef khi đã PAID
            - Xem ảnh chứng minh (proofUrls) do Admin upload
            
            FE dùng cho màn Customer -> Chi tiết yêu cầu rút tiền.
            """
    )
    @ApiResponse(responseCode = "200", description = "Lấy chi tiết yêu cầu rút tiền thành công")
    @GetMapping("/{id}")
    public BaseResponse<CustomerWithdrawResponse> detail(
            @PathVariable UUID customerId,
            @PathVariable UUID id
    ) {
        CustomerWithdrawResponse response = service.customerGet(customerId, id);
        return BaseResponse.success("✅ Xem thành công", response);
    }
}
