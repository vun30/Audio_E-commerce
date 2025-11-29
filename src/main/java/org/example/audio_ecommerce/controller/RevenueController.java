package org.example.audio_ecommerce.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.*;
import org.example.audio_ecommerce.dto.response.BaseResponse;
import org.example.audio_ecommerce.service.RevenueService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/revenue")
@RequiredArgsConstructor
@Tag(name = "Revenue Management", description = "API quản lý doanh thu cho shop và nền tảng")
public class RevenueController {

    private final RevenueService revenueService;

    // ============== STORE REVENUE ==============

    @GetMapping("/store/{storeId}")
    @Operation(summary = "Danh sách doanh thu của 1 store theo ngày/thời gian")
    public ResponseEntity<BaseResponse<Page<StoreRevenueResponse>>> getStoreRevenue(
            @PathVariable UUID storeId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<StoreRevenueResponse> data =
                revenueService.getStoreRevenue(storeId, fromDate, toDate, pageable);

        return ResponseEntity.ok(
                BaseResponse.<Page<StoreRevenueResponse>>builder()
                        .status(200)
                        .message("Danh sách doanh thu của store")
                        .data(data)
                        .build()
        );
    }

    @GetMapping("/store/{storeId}/summary")
    @Operation(summary = "Tổng doanh thu, phí nền tảng, phí ship của 1 store")
    public ResponseEntity<BaseResponse<StoreRevenueSummaryResponse>> getStoreRevenueSummary(
            @PathVariable UUID storeId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        StoreRevenueSummaryResponse data =
                revenueService.getStoreRevenueSummary(storeId, fromDate, toDate);

        return ResponseEntity.ok(
                BaseResponse.<StoreRevenueSummaryResponse>builder()
                        .status(200)
                        .message("Tổng hợp doanh thu của store")
                        .data(data)
                        .build()
        );
    }

    // ============== PLATFORM REVENUE ==============

    @GetMapping("/platform")
    @Operation(summary = "Danh sách doanh thu nền tảng theo thời gian")
    public ResponseEntity<BaseResponse<Page<PlatformRevenueResponse>>> getPlatformRevenue(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PlatformRevenueResponse> data =
                revenueService.getPlatformRevenue(fromDate, toDate, pageable);

        return ResponseEntity.ok(
                BaseResponse.<Page<PlatformRevenueResponse>>builder()
                        .status(200)
                        .message("Danh sách doanh thu nền tảng")
                        .data(data)
                        .build()
        );
    }

    @GetMapping("/platform/summary")
    @Operation(summary = "Tổng hợp doanh thu nền tảng theo loại (COMMISSION, SHIPPING_DIFF, OTHER)")
    public ResponseEntity<BaseResponse<PlatformRevenueSummaryResponse>> getPlatformRevenueSummary(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        PlatformRevenueSummaryResponse data =
                revenueService.getPlatformRevenueSummary(fromDate, toDate);

        return ResponseEntity.ok(
                BaseResponse.<PlatformRevenueSummaryResponse>builder()
                        .status(200)
                        .message("Tổng hợp doanh thu nền tảng")
                        .data(data)
                        .build()
        );
    }
}
