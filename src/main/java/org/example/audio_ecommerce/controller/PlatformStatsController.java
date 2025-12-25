package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.dto.response.PlatformUserStoreGrowthPoint;
import org.example.audio_ecommerce.dto.response.PlatformUserStoreOverviewResponse;
import org.example.audio_ecommerce.service.PlatformStatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(
        name = "Platform User & Store Stats",
        description = """
                Các API thống kê USER/STORE của nền tảng:
                - Tổng account role CUSTOMER
                - Tổng số store
                - Tăng trưởng customer/store theo tháng (so với tháng trước)
                - Dữ liệu chart 2 đường theo năm (12 tháng)
                """
)
@RestController
@RequestMapping("/api/platform/stats")
@RequiredArgsConstructor
public class PlatformStatsController {

    private final PlatformStatsService platformStatsService;

    @Operation(
            summary = "Overview customer/store + growth theo tháng",
            description = """
                    Trả về:
                    - totalCustomerAccounts: tổng account role CUSTOMER (ALL)
                    - totalStores: tổng shop (ALL)

                    - newCustomersInMonth/newStoresInMonth: số mới trong tháng filter
                    - newCustomersPrevMonth/newStoresPrevMonth: số mới tháng trước
                    - customerGrowthPercent/storeGrowthPercent: tăng trưởng % so với tháng trước

                    Query params:
                    - year, month: optional. Không truyền => lấy theo tháng hiện tại.
                    """
    )
    @GetMapping("/user-store/overview")
    public ResponseEntity<BaseResponse<PlatformUserStoreOverviewResponse>> getUserStoreOverview(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        var data = platformStatsService.getUserStoreOverview(year, month);
        return ResponseEntity.ok(BaseResponse.success("Lấy thống kê overview customer/store thành công", data));
    }

    @Operation(
            summary = "Chart tăng trưởng customer/store theo năm (12 tháng)",
            description = """
                    Trả về 12 điểm (month=1..12):
                    - newCustomers: số customer mới
                    - newStores: số store mới

                    Query params:
                    - year: optional. Không truyền => năm hiện tại.
                    """
    )
    @GetMapping("/user-store/growth/chart/year")
    public ResponseEntity<BaseResponse<List<PlatformUserStoreGrowthPoint>>> getUserStoreGrowthChartByYear(
            @RequestParam(required = false) Integer year
    ) {
        var data = platformStatsService.getUserStoreGrowthChartByYear(year);
        return ResponseEntity.ok(BaseResponse.success("Lấy chart tăng trưởng customer/store theo năm thành công", data));
    }
}
