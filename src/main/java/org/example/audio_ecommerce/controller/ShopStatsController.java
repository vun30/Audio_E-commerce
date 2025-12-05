package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.LifetimeStatsResponse;
import org.example.audio_ecommerce.dto.response.MonthlyGrowthPoint;
import org.example.audio_ecommerce.service.ShopStatsService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shop-stats")
@RequiredArgsConstructor
public class ShopStatsController {

    private final ShopStatsService shopStatsService;

    // ===============================================================
    // 1) LIFETIME STATS — thống kê trọn đời
    // ===============================================================
    @Operation(
            summary = "📊 Lifetime Statistics — Thống kê trọn đời",
            description = """
                    **API trả về full thống kê trọn đời của một store**, bao gồm:
                    
                    🔹 Tổng số đơn đã giao  
                    🔹 Tổng doanh thu  
                    🔹 Tổng phí nền tảng  
                    🔹 Doanh thu thực (sau platform fee)  
                    🔹 Tổng số đơn return thành công  
                    🔹 Tỉ lệ return (%)  
                    🔹 Top 10 sản phẩm bán chạy nhất (trọn đời)  
                    🔹 Sản phẩm bị return nhiều nhất  
                    🔹 Tổng phí ship chênh lệch GHN shop phải trả
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Thành công",
                            content = @Content(schema = @Schema(implementation = LifetimeStatsResponse.class))
                    ),
                    @ApiResponse(responseCode = "404", description = "Store không tồn tại"),
                    @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
            }
    )
    @GetMapping("/{storeId}/lifetime")
    public LifetimeStatsResponse getLifetimeStats(
            @Parameter(description = "ID cửa hàng (UUID của Store)", required = true)
            @PathVariable UUID storeId
    ) {
        return shopStatsService.getLifetimeStats(storeId);
    }

    // ===============================================================
    // 2) RANGE STATS — thống kê theo khoảng ngày
    // ===============================================================
    @Operation(
            summary = "📅 Range Statistics — Thống kê theo khoảng thời gian",
            description = """
                    API trả về thống kê **giống Lifetime**, nhưng chỉ trong khoảng ngày FE truyền lên.
                    
                    Các dữ liệu gồm:
                    - Tổng số đơn đã giao
                    - Tổng doanh thu
                    - Phí nền tảng
                    - Doanh thu thực
                    - Tỉ lệ return
                    - Top sản phẩm bán chạy (trong khoảng ngày)
                    - Sản phẩm return nhiều nhất
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Thành công",
                            content = @Content(schema = @Schema(implementation = LifetimeStatsResponse.class))
                    )
            }
    )
    @GetMapping("/{storeId}/range")
    public LifetimeStatsResponse getLifetimeStatsByRange(
            @PathVariable UUID storeId,
            @Parameter(description = "Ngày bắt đầu (yyyy-MM-dd)", required = true)
            @RequestParam LocalDate from,
            @Parameter(description = "Ngày kết thúc (yyyy-MM-dd)", required = true)
            @RequestParam LocalDate to
    ) {
        return shopStatsService.getLifetimeStatsByRange(storeId, from, to);
    }

    // ===============================================================
    // 3) YEAR GROWTH — biểu đồ tăng trưởng theo 12 tháng
    // ===============================================================
    @Operation(
            summary = "📈 Yearly Growth — Tăng trưởng theo 12 tháng",
            description = """
                    API trả về dữ liệu để FE vẽ biểu đồ doanh thu theo từng tháng trong năm:
                    
                    - Tháng (1–12)
                    - Số đơn giao thành công
                    - Tổng revenue của tháng
                    
                    **Dùng cho biểu đồ line chart / bar chart.**
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Thành công",
                            content = @Content(schema = @Schema(implementation = MonthlyGrowthPoint.class))
                    )
            }
    )
    @GetMapping("/{storeId}/growth")
    public List<MonthlyGrowthPoint> getYearGrowth(
            @PathVariable UUID storeId,
            @Parameter(description = "Năm muốn thống kê (VD: 2025)", required = true)
            @RequestParam int year
    ) {
        return shopStatsService.getYearGrowth(storeId, year);
    }
}
