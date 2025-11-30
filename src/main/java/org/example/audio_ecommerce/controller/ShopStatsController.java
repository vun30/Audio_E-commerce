package org.example.audio_ecommerce.controller;

import lombok.RequiredArgsConstructor;
import org.example.audio_ecommerce.dto.response.OrderStatusCountsResponse;
import org.example.audio_ecommerce.dto.response.OrderStatusRatioResponse;
import org.example.audio_ecommerce.dto.response.OrdersByDayResponse;
import org.example.audio_ecommerce.dto.response.ProfitStatsResponse;
import org.example.audio_ecommerce.dto.response.ShopOverviewStatsResponse;
import org.example.audio_ecommerce.dto.response.TopSellingProductResponse;
import org.example.audio_ecommerce.dto.response.ProductViewAnalyticsResponse;
import org.example.audio_ecommerce.dto.response.OutOfStockProductResponse;
import org.example.audio_ecommerce.service.ShopStatsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/shop/stats")
@RequiredArgsConstructor
public class ShopStatsController {

    private final ShopStatsService shopStatsService;

    // ✅ 1. API tổng quan (Dashboard Overview)
    // GET /api/shop/stats/overview?storeId=...&date=YYYY-MM-DD
    @GetMapping("/overview")
    public ResponseEntity<ShopOverviewStatsResponse> overview(
            @RequestParam("storeId") UUID storeId,
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        ShopOverviewStatsResponse resp = shopStatsService.getOverview(storeId, date, startDate, endDate);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/orders")
    public ResponseEntity<OrderStatusCountsResponse> orderStatusCounts(
            @RequestParam("storeId") UUID storeId,
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return ResponseEntity.ok(shopStatsService.getOrderStatusCounts(storeId, fromDate, toDate));
    }

    @GetMapping("/orders-by-day")
    public ResponseEntity<OrdersByDayResponse> ordersByDay(
            @RequestParam("storeId") UUID storeId,
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return ResponseEntity.ok(shopStatsService.getOrdersByDay(storeId, fromDate, toDate));
    }

    @GetMapping("/order-status-ratio")
    public ResponseEntity<OrderStatusRatioResponse> orderStatusRatio(
            @RequestParam("storeId") UUID storeId,
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return ResponseEntity.ok(shopStatsService.getOrderStatusRatio(storeId, fromDate, toDate));
    }

    @GetMapping("/profit")
    public ResponseEntity<ProfitStatsResponse> profitStats(
            @RequestParam("storeId") UUID storeId,
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return ResponseEntity.ok(shopStatsService.getProfitStats(storeId, fromDate, toDate));
    }

    // ============================================================
    // 📊 PRODUCT ANALYTICS APIs
    // ============================================================

    @GetMapping("/products/top-selling")
    public ResponseEntity<List<TopSellingProductResponse>> topSellingProducts(
            @RequestParam("storeId") UUID storeId,
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(value = "limit", defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(shopStatsService.getTopSellingProducts(storeId, fromDate, toDate, limit));
    }

    @GetMapping("/products/view-analytics")
    public ResponseEntity<List<ProductViewAnalyticsResponse>> productViewAnalytics(
            @RequestParam("storeId") UUID storeId
    ) {
        return ResponseEntity.ok(shopStatsService.getProductViewAnalytics(storeId));
    }

    @GetMapping("/products/out-of-stock")
    public ResponseEntity<List<OutOfStockProductResponse>> outOfStockProducts(
            @RequestParam("storeId") UUID storeId,
            @RequestParam(value = "threshold", required = false) Integer threshold
    ) {
        return ResponseEntity.ok(shopStatsService.getOutOfStockProducts(storeId, threshold));
    }
}
