package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.dto.response.StoreRiskWarningResponse;
import org.example.audio_ecommerce.entity.Store;
import org.example.audio_ecommerce.repository.StoreRepository;
import org.example.audio_ecommerce.service.StoreRiskWarningQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/store")
@RequiredArgsConstructor
public class StoreRiskWarningController {

    private final StoreRiskWarningQueryService storeRiskWarningQueryService;
    private final StoreRepository storeRepository;

    @Operation(
            summary = "Lấy cảnh báo rủi ro nợ của shop đang đăng nhập",
            description =
                    """
                    API dùng cho Merchant Center/FE shop để hiển thị mức độ rủi ro nợ hiện tại của cửa hàng đang đăng nhập.
            
                    ✅ Nguồn dữ liệu:
                    - storeId được lấy từ JWT (SecurityContext), không truyền từ client.
                    - Service tính dựa trên:
                      - debtBalance (tổng nợ hiện tại của store)
                      - depositBalance (tiền cọc/ký quỹ)
                      - legalPoint (điểm uy tín) => quy đổi thành hạn mức tín dụng cộng thêm.
            
                    ✅ Công thức chính:
                    - creditLimit = depositBalance + legalPoint * 100,000
                    - ratio = debtBalance / creditLimit (nếu creditLimit > 0)
                    - warningLevel dựa trên ratio:
                      - < 20%  => NONE (an toàn)
                      - >=20%  => NOTICE_20 (có nợ)
                      - >=50%  => WARNING_50 (cảnh báo nhẹ)
                      - >=80%  => DANGER_80 (nguy hiểm)
                      - >=100% => BLOCK_100 (đạt ngưỡng khoá / chặn)
            
                    🔐 Yêu cầu đăng nhập:
                    - Authorization: Bearer <access_token>
                    """

    )
    @ApiResponse(responseCode = "200", description = "Lấy cảnh báo thành công")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập hoặc token không hợp lệ")
    @ApiResponse(responseCode = "404", description = "Không tìm thấy store cho tài khoản đang đăng nhập")
    @GetMapping("/me/risk-warning")
    public ResponseEntity<BaseResponse<StoreRiskWarningResponse>> getMyRiskWarning() {

        String principal = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        String email = principal.contains(":")
                ? principal.split(":")[0]
                : principal;

        Store store = storeRepository.findByAccount_Email(email)
                .orElseThrow(() ->
                        new RuntimeException("❌ Không tìm thấy store cho tài khoản"));

        StoreRiskWarningResponse data =
                storeRiskWarningQueryService.getRiskWarningByStoreId(store.getStoreId());

        return ResponseEntity.ok(
                new BaseResponse<>(200, "✅ Lấy cảnh báo nợ thành công", data)
        );
    }
}
