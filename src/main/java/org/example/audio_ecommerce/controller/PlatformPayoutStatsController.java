//package org.example.audio_ecommerce.controller;
//
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import lombok.RequiredArgsConstructor;
//import org.example.audio_ecommerce.dto.response.BaseResponse;
//import org.example.audio_ecommerce.dto.response.PlatformPayoutRevenueStatsResponse;
//import org.example.audio_ecommerce.service.PlatformPayoutStatsService;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//import java.util.UUID;
//
//@Tag(
//        name = "Platform Payout & Revenue Stats",
//        description = """
//                API thống kê doanh thu/payout theo tháng:
//                - Doanh thu item hợp lệ payout (eligible)
//                - Phí nền tảng đã thu
//                - Trung bình phí nền tảng / item
//                - Số lượng item eligible + số lượng đơn có item eligible
//                - Filter theo tháng/năm và 1..n storeId
//                """
//)
//@RestController
//@RequestMapping("/api/platform/payout-stats")
//@RequiredArgsConstructor
//public class PlatformPayoutStatsController {
//
//    private final PlatformPayoutStatsService payoutStatsService;
//
//    @Operation(
//            summary = "Thống kê doanh thu eligible + phí nền tảng theo tháng (lọc nhiều store)",
//            description = """
//                    Điều kiện item được tính:
//                    - store_order.delivered_at != null
//                    - store_order_item.eligible_for_payout = true
//                    - store_order_item.is_payout = true
//
//                    Trả về:
//                    - eligibleGross: SUM(final_line_total)
//                    - platformFeeCollected: SUM(platform_fee_amount)
//                    - avgPlatformFeePerItem: platformFeeCollected / eligibleItemCount
//                    - eligibleItemCount: COUNT(store_order_item)
//                    - eligibleOrderCount: COUNT(DISTINCT store_order)
//
//                    Query params:
//                    - year, month: optional. Không truyền => lấy tháng hiện tại
//                    - storeIds: optional (có thể truyền nhiều). Không truyền => thống kê toàn nền tảng
//                    """
//    )
//    @GetMapping("/revenue")
//    public ResponseEntity<BaseResponse<PlatformPayoutRevenueStatsResponse>> getPayoutRevenueStats(
//            @RequestParam(required = false) Integer year,
//            @RequestParam(required = false) Integer month,
//            @RequestParam(required = false) List<UUID> storeIds
//    ) {
//        var data = payoutStatsService.getPayoutRevenueStats(year, month, storeIds);
//        return ResponseEntity.ok(BaseResponse.success("Lấy thống kê payout/revenue theo tháng thành công", data));
//    }
//}
