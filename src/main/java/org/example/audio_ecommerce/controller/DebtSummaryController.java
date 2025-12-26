package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.dto.response.GhnFlatDebtSummaryResponse;
import org.example.audio_ecommerce.service.DebtSummaryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/debt")
@RequiredArgsConstructor
public class DebtSummaryController {

    private final DebtSummaryService debtSummaryService;

    /**
     * ✅ Không truyền storeId => tính toàn hệ thống
     * ✅ Có storeId => tính riêng store
     */

    @Operation(
            summary = "Tổng hợp công nợ GHN/Flat (operator view)",
            description = """
            **Mục tiêu:** Cho operator biết **Flat đang nợ GHN bao nhiêu** và số tiền đó đến từ đâu.

            **Phạm vi tính toán**
            - Nếu không truyền `storeId` → tính **TOÀN HỆ THỐNG**
            - Nếu truyền `storeId` → chỉ tính **THEO STORE đó**

            ---
            ## Nguồn dữ liệu
            - Bảng `store_order`
            - Bảng `return_shipping_fees`

            ---
            ## Điều kiện áp dụng cho ORDER
            - Chỉ tính các đơn có `deliveredAt != null` (đơn đã có ngày delivered)
            - Chỉ tính khi `shippingFeeReal > 0`

            ---
            ## Định nghĩa các khoản (operator)
            ### 1) customerPaidTotal (Tổng khách đã trả)
            `customerPaidTotal = Σ shippingFee` của các đơn **đã delivered**

            ### 2) storeOrderDebtToFlat (Shop nợ Flat từ ORDER)
            Với mỗi order delivered:
            - Nếu `returnChargeApplied = true` → `shopDebt = shippingFeeReal × 1.5`
            - Nếu `returnChargeApplied = false` → `shopDebt = max(shippingFeeReal − shippingFee, 0)`

            Tách:
            - `paid`: Σ shopDebt với `paidByShop = true`
            - `outstanding`: Σ shopDebt với `paidByShop = false`
            - `total = paid + outstanding`

            ### 3) returnFeeDebtToFlat (Shop nợ Flat từ RETURN SHIPPING FEE)
            Với mỗi record `return_shipping_fees` có `payer = SHOP`:
            - `amount = (chargedToShop > 0) ? chargedToShop : shippingFee`

            Tách:
            - `paid`: Σ amount với `paidByShop = true`
            - `outstanding`: Σ amount với `paidByShop = false`
            - `total = paid + outstanding`

            ---
            ## Công thức chốt (LUÔN ĐÚNG)
            **flatDebtToGHN = customerPaidTotal + storeOrderDebtToFlat.total + returnFeeDebtToFlat.total**

            > Operator dùng công thức này để đối soát GHN: Flat ứng trả GHN,
            một phần lấy từ khách (ship dự kiến), phần còn lại shop bù (chênh/1.5R),
            cộng thêm phí return shop chịu.
            """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Tổng hợp công nợ thành công",
                            content = @Content(schema = @Schema(implementation = GhnFlatDebtSummaryResponse.class))
                    )
            }
    )
    @GetMapping("/ghn-flat-summary")
    public ResponseEntity<BaseResponse<GhnFlatDebtSummaryResponse>> getGhnFlatSummary(
            @RequestParam(required = false) UUID storeId
    ) {
        GhnFlatDebtSummaryResponse resp = debtSummaryService.calcGhnFlatDebtSummary(storeId);
        return ResponseEntity.ok(BaseResponse.success("✅ Tổng hợp công nợ GHN / Flat", resp));
    }
}
