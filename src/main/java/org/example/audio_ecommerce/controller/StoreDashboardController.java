package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.dto.response.StoreDashboardResponses;
import org.example.audio_ecommerce.service.StoreDashboardService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Tag(
        name = "Store Dashboard",
        description = """
                Dashboard thống kê cho Shop/Store:
                - Chỉ tính theo đơn DELIVERED (StoreOrder.deliveredAt != null)
                - Doanh thu tính theo item (StoreOrderItem) và KHÔNG bao gồm phí ship
                - Các API đều hỗ trợ lọc theo khoảng thời gian (from/to) hoặc theo năm
                """
)
@RestController
@RequestMapping("/api/store/dashboard")
@RequiredArgsConstructor
public class StoreDashboardController {

    private final StoreDashboardService storeDashboardService;

    // =========================
    // 1) SUMMARY
    // =========================
    @Operation(
            summary = "Tổng quan doanh thu & đơn hàng theo khoảng thời gian",
            description = """
                    Trả về thống kê tổng quan cho shop trong khoảng thời gian [from, to]:
                    
                    ✅ Điều kiện dữ liệu:
                    - Chỉ tính các StoreOrder có deliveredAt != null (đơn đã giao)
                    - Chỉ tính các StoreOrderItem có isPayout = true
                    
                    ✅ Công thức:
                    - grossRevenue = SUM(StoreOrderItem.finalLineTotal)
                      (tiền hàng sau discount, tuyệt đối KHÔNG gồm phí ship)
                    - platformFeePaid = SUM(StoreOrderItem.platformFeeAmount)
                      (tiền phí nền tảng đã trừ/đã ghi nhận trên item)
                    - netRevenue = grossRevenue - platformFeePaid
                    
                    ✅ Thống kê kèm theo:
                    - deliveredOrderCount: số đơn delivered trong khoảng thời gian
                    - itemsSold: tổng quantity item bán ra (chỉ tính item isPayout=true)
                    - topSellingRefId: refId (productId/comboId) bán chạy nhất
                    - topSellingQuantity: tổng quantity của refId top1
                    
                    📌 Format thời gian:
                    - from/to dùng ISO-8601: yyyy-MM-dd'T'HH:mm:ss
                    - Ví dụ: 2025-12-01T00:00:00
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy dashboard summary thành công"),
            @ApiResponse(responseCode = "400", description = "Thiếu hoặc sai format from/to"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập / không có storeId")
    })
    @GetMapping("/summary")
    public ResponseEntity<BaseResponse> summary(
            @Parameter(
                    description = "Thời gian bắt đầu (ISO-8601). Ví dụ: 2025-12-01T00:00:00",
                    required = true,
                    example = "2025-12-01T00:00:00"
            )
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @Parameter(
                    description = "Thời gian kết thúc (ISO-8601). Ví dụ: 2025-12-31T23:59:59",
                    required = true,
                    example = "2025-12-31T23:59:59"
            )
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to
    ) {
        StoreDashboardResponses.StoreDashboardSummaryResponse data =
                storeDashboardService.getSummary(from, to);

        return ResponseEntity.ok(BaseResponse.success("✅ Store dashboard summary", data));
    }

    // =========================
    // 2) RETURNS
    // =========================
    @Operation(
            summary = "Thống kê đơn RETURN theo khoảng thời gian + Top 5 product bị return",
            description = """
                    Trả về thống kê return trong khoảng thời gian [from, to] (theo ReturnRequest.createdAt).
                    
                    ✅ Điều kiện lọc return:
                    - Chỉ đếm các ReturnRequest có status KHÔNG thuộc:
                      PENDING, CANCELLED, DISPUTE_RESOLVED_SHOP, REJECTED
                    
                    ✅ Dữ liệu trả về:
                    - returnCount: tổng số return hợp lệ
                    - top5ReturnedProducts: top 5 productId bị return nhiều nhất
                      (kèm count)
                    
                    📌 Format thời gian:
                    - from/to dùng ISO-8601: yyyy-MM-dd'T'HH:mm:ss
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy return stats thành công"),
            @ApiResponse(responseCode = "400", description = "Thiếu hoặc sai format from/to"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập / không có storeId")
    })
    @GetMapping("/returns")
    public ResponseEntity<BaseResponse> returns(
            @Parameter(
                    description = "Thời gian bắt đầu (ISO-8601). Ví dụ: 2025-12-01T00:00:00",
                    required = true,
                    example = "2025-12-01T00:00:00"
            )
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @Parameter(
                    description = "Thời gian kết thúc (ISO-8601). Ví dụ: 2025-12-31T23:59:59",
                    required = true,
                    example = "2025-12-31T23:59:59"
            )
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to
    ) {
        StoreDashboardResponses.StoreReturnStatsResponse data =
                storeDashboardService.getReturnStats(from, to);

        return ResponseEntity.ok(BaseResponse.success("✅ Store return stats", data));
    }

    // =========================
    // 3) GROWTH BY MONTH
    // =========================
    @Operation(
            summary = "Chart tăng trưởng theo THÁNG trong 1 năm",
            description = """
                    Trả về dữ liệu chart tăng trưởng theo từng tháng trong 1 năm.
                    
                    ✅ Điều kiện:
                    - Chỉ tính đơn DELIVERED (StoreOrder.deliveredAt != null)
                    - Chỉ tính item isPayout=true
                    
                    ✅ Output:
                    - points[] gồm year, month, grossRevenue, platformFeePaid, netRevenue,
                      deliveredOrderCount, itemsSold
                    - granularity = 'MONTH'
                    
                    📌 Gợi ý FE:
                    - Dùng month (1..12) để map chart X-axis
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy growth by month thành công"),
            @ApiResponse(responseCode = "400", description = "Năm không hợp lệ"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập / không có storeId")
    })
    @GetMapping("/growth/month")
    public ResponseEntity<BaseResponse> growthByMonth(
            @Parameter(
                    description = "Năm cần thống kê (ví dụ: 2025)",
                    required = true,
                    example = "2025"
            )
            @RequestParam int year
    ) {
        StoreDashboardResponses.GrowthResponse data =
                storeDashboardService.growthByMonth(year);

        return ResponseEntity.ok(BaseResponse.success("✅ Store growth by month", data));
    }

    // =========================
    // 4) GROWTH BY YEAR (RANGE)
    // =========================
    @Operation(
            summary = "Chart tăng trưởng theo NĂM trong 1 khoảng năm",
            description = """
                    Trả về dữ liệu chart tăng trưởng theo từng năm trong một khoảng [fromYear, toYear].
                    
                    ✅ Điều kiện:
                    - Chỉ tính đơn DELIVERED (StoreOrder.deliveredAt != null)
                    - Chỉ tính item isPayout=true
                    
                    ✅ Output:
                    - points[] gồm year, grossRevenue, platformFeePaid, netRevenue,
                      deliveredOrderCount, itemsSold
                    - month = null
                    - granularity = 'YEAR'
                    
                    📌 Gợi ý FE:
                    - Dùng year làm X-axis
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy growth by year thành công"),
            @ApiResponse(responseCode = "400", description = "fromYear/toYear không hợp lệ"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập / không có storeId")
    })
    @GetMapping("/growth/year")
    public ResponseEntity<BaseResponse> growthByYear(
            @Parameter(
                    description = "Năm bắt đầu (ví dụ: 2023)",
                    required = true,
                    example = "2023"
            )
            @RequestParam int fromYear,

            @Parameter(
                    description = "Năm kết thúc (ví dụ: 2025)",
                    required = true,
                    example = "2025"
            )
            @RequestParam int toYear
    ) {
        StoreDashboardResponses.GrowthResponse data =
                storeDashboardService.growthByYear(fromYear, toYear);

        return ResponseEntity.ok(BaseResponse.success("✅ Store growth by year", data));
    }

    // =========================
    // 5) FULL DASHBOARD (1 CALL)
    // =========================
    @Operation(
            summary = "Dashboard FULL (1 call): summary + returns + growth(month)",
            description = """
                    API gộp cho FE gọi 1 lần để render trang dashboard.
                    
                    ✅ Input:
                    - from/to: lọc theo khoảng thời gian cho summary + returns
                    - year (optional): nếu có year thì trả thêm growth theo tháng của năm đó
                    
                    ✅ Output:
                    - summary: StoreDashboardSummaryResponse
                    - returns: StoreReturnStatsResponse
                    - growth: GrowthResponse (null nếu không truyền year)
                    
                    ✅ Lưu ý:
                    - Summary chỉ tính các đơn deliveredAt != null + item isPayout = true
                    - Returns lọc status (loại PENDING, CANCELLED, DISPUTE_RESOLVED_SHOP, REJECTED)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy full dashboard thành công"),
            @ApiResponse(responseCode = "400", description = "Sai format from/to hoặc year không hợp lệ"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập / không có storeId")
    })
    @GetMapping("/full")
    public ResponseEntity<BaseResponse> full(
            @Parameter(
                    description = "Thời gian bắt đầu (ISO-8601). Ví dụ: 2025-12-01T00:00:00",
                    required = true,
                    example = "2025-12-01T00:00:00"
            )
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @Parameter(
                    description = "Thời gian kết thúc (ISO-8601). Ví dụ: 2025-12-31T23:59:59",
                    required = true,
                    example = "2025-12-31T23:59:59"
            )
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,

            @Parameter(
                    description = "Năm để trả chart theo tháng (optional). Nếu không truyền thì growth = null",
                    required = false,
                    example = "2025"
            )
            @RequestParam(required = false)
            Integer year
    ) {
        StoreDashboardResponses.StoreDashboardFullResponse data =
                storeDashboardService.getFull(from, to, year);

        return ResponseEntity.ok(BaseResponse.success("✅ Store dashboard full", data));
    }
}