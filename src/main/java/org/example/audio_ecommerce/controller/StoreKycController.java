package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.request.StoreKycRequest;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.entity.Enum.KycStatus;
import org.example.audio_ecommerce.entity.StoreKyc;
import org.example.audio_ecommerce.service.StoreKycService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Store KYC", description = "Quản lý quy trình xác minh cửa hàng (KYC) – Chủ shop gửi, Admin duyệt")
@RestController
@RequestMapping("/api/stores/{storeId}/kyc")
@RequiredArgsConstructor
public class StoreKycController {

    private final StoreKycService storeKycService;

    @Operation(
            summary = "Gửi yêu cầu KYC",
            description = """
                    Chủ cửa hàng gửi thông tin xác minh (KYC) để hệ thống duyệt.
                    - Mỗi cửa hàng chỉ có thể có **1 request PENDING** tại một thời điểm.
                    - Sau khi gửi thành công, trạng thái cửa hàng sẽ chuyển thành `PENDING`.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Gửi yêu cầu KYC thành công"),
            @ApiResponse(responseCode = "400", description = "Đã tồn tại request KYC đang chờ duyệt")
    })
    @PostMapping
    public ResponseEntity<StoreKyc> submitKyc(
            @Parameter(description = "ID cửa hàng (UUID)", required = true)
            @PathVariable UUID storeId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Thông tin KYC cần gửi",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = StoreKycRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "storeName": "AudioKing Hi-Fi",
                                      "phoneNumber": "0987654321",
                                      "businessLicenseNumber": "123456789",
                                      "taxCode": "0312345678",
                                      "bankName": "Vietcombank",
                                      "bankAccountName": "CÔNG TY TNHH AUDIO KING",
                                      "bankAccountNumber": "1234567890",
                                      "idCardFrontUrl": "https://cdn.example.com/front.png",
                                      "idCardBackUrl": "https://cdn.example.com/back.png",
                                      "businessLicenseUrl": "https://cdn.example.com/license.pdf",
                                      "isOfficial": true
                                    }
                            """)
                    )
            )
            @Valid @RequestBody StoreKycRequest request) {
        return ResponseEntity.ok(storeKycService.submitKyc(storeId, request));
    }

    @Operation(summary = "Lấy danh sách tất cả request KYC của cửa hàng", description = "Dành cho chủ shop xem lịch sử các lần gửi KYC.")
    @ApiResponse(responseCode = "200", description = "Danh sách KYC")
    @GetMapping
    public ResponseEntity<List<StoreKyc>> getAllRequests(
            @Parameter(description = "ID cửa hàng (UUID)", required = true)
            @PathVariable UUID storeId) {
        return ResponseEntity.ok(storeKycService.getAllRequestsOfStore(storeId));
    }

    @Operation(summary = "Xem chi tiết một request KYC", description = "Trả về toàn bộ chi tiết của một yêu cầu KYC cụ thể.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy chi tiết thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy request")
    })
    @GetMapping("/{kycId}")
    public ResponseEntity<StoreKyc> getRequestDetail(
            @Parameter(description = "ID KYC (UUID)", required = true)
            @PathVariable String kycId) {
        return ResponseEntity.ok(storeKycService.getRequestDetail(kycId));
    }

    @Operation(summary = "Admin: Lọc danh sách KYC theo trạng thái", description = "Lấy danh sách tất cả request theo trạng thái (`PENDING`, `APPROVED`, `REJECTED`).")
    @ApiResponse(responseCode = "200", description = "Danh sách KYC theo trạng thái")
    @GetMapping("/filter")
    public ResponseEntity<BaseResponse> getAllKycByStatus(
            @Parameter(description = "Trạng thái KYC", example = "PENDING")
            @RequestParam KycStatus status) {
        List<StoreKyc> list = storeKycService.getRequestsByStatus(status);
        return ResponseEntity.ok(new BaseResponse<>(200, "Danh sách KYC theo trạng thái: " + status, list));
    }

    @Operation(summary = "Admin: Phê duyệt KYC", description = "Phê duyệt KYC và kích hoạt cửa hàng.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Phê duyệt thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy request KYC")
    })
    @PatchMapping("/{kycId}/approve")
    public ResponseEntity<String> approve(
            @Parameter(description = "ID KYC (UUID)", required = true)
            @PathVariable String kycId) {
        storeKycService.approveKyc(kycId);
        return ResponseEntity.ok("✅ KYC approved & store activated.");
    }

    @Operation(summary = "Admin: Từ chối KYC", description = "Từ chối yêu cầu KYC với lý do chi tiết.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Từ chối thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy request KYC")
    })
    @PatchMapping("/{kycId}/reject")
    public ResponseEntity<String> reject(
            @Parameter(description = "ID KYC (UUID)", required = true)
            @PathVariable String kycId,
            @Parameter(description = "Lý do từ chối", example = "Thiếu giấy phép kinh doanh")
            @RequestParam String reason) {
        storeKycService.rejectKyc(kycId, reason);
        return ResponseEntity.ok("❌ KYC rejected: " + reason);
    }
}
