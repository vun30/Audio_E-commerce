package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.dto.response.StoreTotalRevenueResponse;
import org.example.audio_ecommerce.service.StoreRevenueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/store/revenue")
@RequiredArgsConstructor
@Tag(name = "Store Revenue", description = "API quản lý doanh thu cho cửa hàng")
public class StoreRevenueController {

    private final StoreRevenueService storeRevenueService;

    @GetMapping("/total/{storeId}")
    @Operation(
            summary = "Lấy tổng doanh thu nền tảng từ cửa hàng",
            description = """
                    Trả về tổng doanh thu nền tảng được tính từ phí nền tảng trong các đơn hàng đã giao thành công.
                    
                    Điều kiện:
                    - Chỉ tính các StoreOrder có status = DELIVERY_SUCCESS
                    - Chỉ tính các StoreOrder có deliveredAt != null
                    - Tổng doanh thu = SUM(platformFeeAmount) của các đơn hàng
                    
                    Kết quả bao gồm:
                    - Tổng phí nền tảng thu được
                    - Tổng số đơn đã giao
                    - Danh sách các đơn hàng đã giao với chi tiết phí nền tảng
                    """
    )
    public ResponseEntity<BaseResponse<StoreTotalRevenueResponse>> getStoreTotalRevenue(
            @PathVariable UUID storeId) {
        
        StoreTotalRevenueResponse data = storeRevenueService.getStoreTotalRevenue(storeId);

        return ResponseEntity.ok(
                BaseResponse.<StoreTotalRevenueResponse>builder()
                        .status(200)
                        .message("Lấy tổng doanh thu nền tảng từ cửa hàng thành công")
                        .data(data)
                        .build()
        );
    }
}