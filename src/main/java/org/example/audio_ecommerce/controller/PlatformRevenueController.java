package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.*;
import org.example.audio_ecommerce.service.PlatformWalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(
        name = "Platform Revenue & Growth",
        description = """
                Các API thống kê doanh thu và tăng trưởng của nền tảng.
                
                Bao gồm:
                - Tổng quan doanh thu nền tảng (từ order item đã giao)
                - Biểu đồ tăng trưởng doanh thu theo tháng
                - Biểu đồ tăng trưởng doanh thu theo năm
                """
)
@RestController
@RequestMapping("/api/platform/revenue")
@RequiredArgsConstructor
public class PlatformRevenueController {

    private final PlatformWalletService platformWalletService;

    // =========================================================
    // 1️⃣ OVERVIEW – TỔNG DOANH THU NỀN TẢNG
    // =========================================================
    @Operation(
            summary = "Tổng quan doanh thu nền tảng",
            description = """
                    Trả về tổng doanh thu nền tảng được tính từ StoreOrderItem.

                    Điều kiện:
                    - StoreOrderItem.deliveredAt != null
                    - StoreOrderItem.eligibleForPayout = true

                    Công thức:
                    - totalItemRevenue = SUM(finalLineTotal)
                    - platformFeeRevenue = SUM(finalLineTotal × platformFeePercentage / 100)

                    ❌ Không bao gồm phí ship
                    """
    )
    @GetMapping("/overview")
    public ResponseEntity<BaseResponse<PlatformRevenueOverviewResponse>> getPlatformRevenueOverview() {

        PlatformRevenueOverviewResponse data =
                platformWalletService.getPlatformRevenueOverview();

        return ResponseEntity.ok(
                BaseResponse.success(
                        "Lấy tổng quan doanh thu nền tảng thành công",
                        data
                )
        );
    }

    // =========================================================
    // 2️⃣ CHART – TĂNG TRƯỞNG THEO THÁNG
    // =========================================================
    @Operation(
            summary = "Biểu đồ tăng trưởng nền tảng theo tháng",
            description = """
                    Trả về dữ liệu vẽ biểu đồ đường (line chart):

                    - Line 1: Doanh thu nền tảng (platformRevenue)
                    - Line 2: Tỷ lệ hoàn hàng (%)

                    Return chỉ tính các trạng thái KHÁC:
                    PENDING, CANCELLED, CANCELED, REJECTED
                    """
    )
    @GetMapping("/growth/chart/month")
    public ResponseEntity<BaseResponse<List<PlatformGrowthChartPoint>>> getPlatformGrowthChartByMonth() {

        List<PlatformGrowthChartPoint> data =
                platformWalletService.getPlatformGrowthChartByMonth();

        return ResponseEntity.ok(
                BaseResponse.success(
                        "Lấy biểu đồ tăng trưởng nền tảng theo tháng",
                        data
                )
        );
    }

    // =========================================================
    // 3️⃣ CHART – TĂNG TRƯỞNG THEO NĂM
    // =========================================================
    @Operation(
            summary = "Biểu đồ tăng trưởng nền tảng theo năm",
            description = """
                    Trả về dữ liệu vẽ biểu đồ theo năm:

                    - platformRevenue: tổng phí nền tảng theo năm
                    - returnRate: tỷ lệ hoàn hàng (%)

                    Dùng cho dashboard tổng quan dài hạn.
                    """
    )
    @GetMapping("/growth/chart/year")
    public ResponseEntity<BaseResponse<List<PlatformGrowthChartPoint>>> getPlatformGrowthChartByYear() {

        List<PlatformGrowthChartPoint> data =
                platformWalletService.getPlatformGrowthChartByYear();

        return ResponseEntity.ok(
                BaseResponse.success(
                        "Lấy biểu đồ tăng trưởng nền tảng theo năm",
                        data
                )
        );
    }
}
