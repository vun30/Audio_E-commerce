package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.dto.response.DebtComponentItemResponse;
import org.example.audio_ecommerce.dto.response.StoreRiskWarningResponse;
import org.example.audio_ecommerce.entity.Enum.DebtComponentType;
import org.example.audio_ecommerce.entity.Store;
import org.example.audio_ecommerce.repository.StoreRepository;
import org.example.audio_ecommerce.service.Impl.StoreDebtQueryService;
import org.example.audio_ecommerce.service.StoreRiskWarningQueryService;
import org.example.audio_ecommerce.service.StoreWalletService;
import org.example.audio_ecommerce.util.SecurityUtils;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/store")
@RequiredArgsConstructor
public class StoreRiskWarningController {

    private final StoreRiskWarningQueryService storeRiskWarningQueryService;
    private final StoreRepository storeRepository;
    private final StoreWalletService storeWalletService;
    private final SecurityUtils securityUtils;
    private final StoreDebtQueryService storeDebtQueryService;

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
                            
                                                                             "warningLine": 50000,
                                                                             "criticalLine": 200000,//////// BỎ mẹ nó đi anh ơi
                            
                                   payableNowDebt = tổng số tiền nợ CÓ THỂ TRẢ NGAY (NOW)
                              👉 chỉ bao gồm các khoản phí đã “DONE / FINAL / ĐÃ PHÁT SINH”
                            
                              ❌ KHÔNG bao gồm các khoản ước tính, pending, chưa kết thúc, chưa quyết toán.
                            
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

    // ✅ 3) Breakdown nợ theo thành phần (filter + paging)
    @Operation(
            summary = "Breakdown các khoản nợ cấu thành Debt Balance của cửa hàng",
            description = """
    API trả về danh sách chi tiết các khoản nợ đang cấu thành Debt Balance của cửa hàng đang đăng nhập.

    🔹 Mỗi dòng trong kết quả tương ứng với MỘT khoản nợ cụ thể (Debt Component).
    🔹 Các khoản nợ được sinh ra từ 2 nguồn chính:
      - StoreOrder (đơn hàng của shop)
      - ReturnShippingFee (phí hoàn/return)

    =========================
    1️⃣ CÁC LOẠI KHOẢN NỢ (componentType)
    =========================

    ● SHIP_DIFF – Chênh lệch phí ship khi đơn đã giao thành công
      - Phát sinh khi:
        • Đơn hàng đã giao (deliveredAt != null)
        • Phí ship thực tế GHN (shippingFeeReal) > phí dự kiến ban đầu (shippingFee)
      - Công thức:
        SHIP_DIFF = max(shippingFeeReal - shippingFee, 0)

      Ví dụ:
      - shippingFee (ước tính): 20.000
      - shippingFeeReal (GHN thực tế): 28.000
      → SHIP_DIFF = 8.000

    -------------------------

    ● RTO_FEE – Phí quay đầu / không nhận hàng
      - Áp dụng cho đơn CHƯA giao thành công (deliveredAt == null)
      - Trường hợp:
        • Khách nhận hàng → KHÔNG phát sinh RTO_FEE
        • Khách boom / không nhận → phát sinh phí quay đầu
      - Số tiền nợ gồm:
        • Phí ship gốc GHN (shippingFeeReal)
        • + phụ thu quay đầu (ví dụ: 50% phí GHN quay về, nếu đã chốt)

      Công thức trong hệ thống:
        RTO_FEE = shippingFeeReal
                + (returnChargeApplied ? shippingFeeReal * returnShippingChargeRate% : 0)

      Ví dụ:
      - shippingFeeReal = 30.000
      - returnShippingChargeRate = 50%
      - returnChargeApplied = true
      → RTO_FEE = 30.000 + 15.000 = 45.000

    -------------------------

    ● RETURN_SHIPPING_FEE – Phí ship hoàn/return mà SHOP phải chịu
      - Phát sinh từ luồng return/hoàn hàng
      - Điều kiện:
        • payer = SHOP
        • Chưa thanh toán (paidByShop = false)
      - Số tiền:
        • Ưu tiên chargedToShop (nếu > 0)
        • Nếu không có thì dùng shippingFee

      Ví dụ:
      - chargedToShop = 25.000
      → RETURN_SHIPPING_FEE = 25.000

    =========================
    2️⃣ payableNowOnly – LỌC NỢ CÓ THỂ TRẢ NGAY (NOW)
    =========================

    Tham số payableNowOnly dùng để lọc các khoản nợ đã "DONE / FINAL / ĐÃ PHÁT SINH"
    tức là các khoản shop CÓ THỂ THANH TOÁN NGAY.

    ● payableNowOnly = true
      → CHỈ lấy các khoản nợ đã phát sinh:
        - SHIP_DIFF: đơn đã delivered
        - RTO_FEE: phí quay đầu đã được chốt (returnChargeApplied = true)
        - RETURN_SHIPPING_FEE: payer = SHOP và chưa thanh toán

    ● payableNowOnly = false hoặc không truyền
      → KHÔNG áp dụng lọc
      → Trả về TOÀN BỘ các khoản nợ (bao gồm cả pending / chưa chốt)

    =========================
    3️⃣ CÁC BỘ LỌC HỖ TRỢ
    =========================

    API hỗ trợ các bộ lọc sau:
      - componentType: SHIP_DIFF | RTO_FEE | RETURN_SHIPPING_FEE
      - status: PAID | UNPAID
      - from / to: khoảng thời gian phát sinh khoản nợ
      - minAmount / maxAmount: khoảng tiền
      - orderCode: mã đơn hàng
      - ghnOrderCode: mã vận đơn GHN
      - page / size: phân trang

    =========================
    4️⃣ MỤC ĐÍCH SỬ DỤNG
    =========================

    API này được FE sử dụng để:
      - Hiển thị bảng chi tiết breakdown Debt Balance
      - Drill-down từ dashboard cảnh báo nợ
      - Hỗ trợ shop theo dõi các khoản nợ chi tiết và thanh toán
    """
    )

    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lấy breakdown các khoản nợ thành công",
                    content = @Content(
                            schema = @Schema(implementation = DebtComponentItemResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Tham số filter không hợp lệ (sai format ngày, số tiền, enum...)"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Chưa đăng nhập hoặc token không hợp lệ"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Không có quyền truy cập dữ liệu cửa hàng"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Lỗi hệ thống khi xử lý breakdown nợ"
            )
    })
    @GetMapping("/me/debt-components")
    public ResponseEntity<BaseResponse> getMyDebtComponents(
            @RequestParam(required = false) DebtComponentType componentType, // SHIP_DIFF | RTO_FEE | RETURN_SHIPPING_FEE
            @RequestParam(required = false) String status,                    // UNPAID | PAID

            // ✅ NEW: filter chỉ lấy nợ có thể trả ngay (NOW)
            @RequestParam(required = false, defaultValue = "false") Boolean payableNowOnly,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) String orderCode,
            @RequestParam(required = false) String ghnOrderCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return storeWalletService.getMyDebtComponents(
                componentType,
                status,
                payableNowOnly,  // ✅ giờ đã có biến để truyền
                from,
                to,
                minAmount,
                maxAmount,
                orderCode,
                ghnOrderCode,
                page,
                size
        );
    }

    @GetMapping("/unpaid-ended")
    public ResponseEntity<BaseResponse> getUnpaidEndedDebts() {
        var storeId = securityUtils.getCurrentStoreId();
        var data = storeDebtQueryService.getUnpaidEndDebtOrders(storeId);
        return ResponseEntity.ok(BaseResponse.success("✅ Lấy danh sách nợ cần quyết toán thành công", data));
    }

//    @PostMapping("/unblock-by-debt")
//    public ResponseEntity<BaseResponse> unblockMyStoreByDebt() {
//        return storeWalletService.unblockMyStoreByDebt();
//    }
}
